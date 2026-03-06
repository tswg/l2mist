package core.gameserver.phantom;

import core.gameserver.ThreadPoolManager;
import core.gameserver.model.Player;
import core.gameserver.phantom.model.PhantomBot;
import core.gameserver.phantom.model.PhantomSpot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ThreadLocalRandom;

public final class PhantomManager {
	private static final Logger _log = LoggerFactory.getLogger(PhantomManager.class);
	private static final long TRACKED_FORCED_ROAM_INTERVAL_MS = 4500L;
	private static final long TRACKED_FORCED_PROBE_INTERVAL_MS = 7000L;
	private static final int FORCED_PROBE_TICKS = 5;

    private static final PhantomManager INSTANCE = new PhantomManager();
    public static PhantomManager getInstance() { return INSTANCE; }

    private final Queue<PhantomBot> active = new ConcurrentLinkedQueue<PhantomBot>();
    private final Queue<PhantomBot> idle   = new ConcurrentLinkedQueue<PhantomBot>();
    private final Queue<PhantomBot> sleep  = new ConcurrentLinkedQueue<PhantomBot>();
    private final Set<Integer> debugTracked = new HashSet<Integer>();

    private PhantomManager() {}

    public void init() {
        System.out.println("[PHANTOM] PhantomManager init start");
        debugTracked.clear();
        PhantomWorld.getInstance().loadAll();
        if (!PhantomConfig.ENABLED) {
            System.out.println("[PHANTOM] PhantomManager init aborted: PhantomEnabled=false");
            _log.info("PhantomManager disabled by config");
            return;
        }

        System.out.println("[PHANTOM] PhantomManager world stats before spawn: spots=" + PhantomWorld.getInstance().spotsCount()
                + " profiles=" + PhantomWorld.getInstance().profilesCount()
                + " candidates=" + PhantomWorld.getInstance().candidatesCount());

        SpawnStats stats = spawnInitial();

        if (stats.created <= 0) {
            _log.error("PhantomManager is disabled: spawn failed for all planned phantoms. planned={} failed={}.", stats.planned, stats.failed);
            return;
        }

        schedule();
		_log.info("PhantomManager initialized: spots={}, profiles={}, planned={}, created={}, failed={}, active={}, idle={}, sleep={}",
				PhantomWorld.getInstance().spotsCount(),
				PhantomWorld.getInstance().profilesCount(),
				stats.planned,
				stats.created,
				stats.failed,
				active.size(), idle.size(), sleep.size());
    }

    private SpawnStats spawnInitial() {
        SpawnStats stats = new SpawnStats();
        int total = PhantomConfig.TOTAL_ONLINE;
        int candidates = PhantomWorld.getInstance().candidatesCount();
        if (candidates > 0 && total > candidates) {
            _log.warn("Phantom requested count {} exceeds candidate pool {}. Limiting spawn plan.", total, candidates);
            total = candidates;
        }
        stats.planned = total;

        for (int i = 0; i < total; i++) {
            PhantomSpot spot = PhantomWorld.getInstance().pickSpotForLevel(PhantomWorld.getInstance().pickRandomLevel());
            if (spot == null) {
				stats.failed++;
				_log.error("No spot found for phantom index={}", i);
				continue;
			}

            Player p;
            try {
                p = PhantomWorld.getInstance().spawnPhantomActor(spot);
            } catch (Throwable t) {
				stats.failed++;
                _log.error("Phantom spawn failed for index={} spot={}({})", i, spot.id, spot.name, t);
                continue;
            }

            stats.created++;

            // стартовое размещение
            PhantomAdapter.moveTo(p, PhantomAdapter.loc(spot.centerX, spot.centerY, spot.centerZ));

            PhantomBot bot = new PhantomBot(p, spot);
            if (PhantomConfig.DEBUG && debugTracked.isEmpty())
                debugTracked.add(p.getObjectId());

            if (i < PhantomConfig.ACTIVE_CAP) {
                switchState(bot, PhantomState.ACTIVE, "spawn-active-cap");
                active.add(bot);
            } else {
                switchState(bot, PhantomState.SLEEP, "spawn-over-cap");
                sleep.add(bot);
            }
        }
        return stats;
    }


    private static final class TickSnapshot {
        private final int x;
        private final int y;
        private final int z;

        private TickSnapshot(int x, int y, int z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        private static TickSnapshot capture(PhantomBot bot) {
            Player p = bot != null ? bot.actor : null;
            if (p == null)
                return new TickSnapshot(0, 0, 0);
            return new TickSnapshot(p.getX(), p.getY(), p.getZ());
        }

        private int chebyshevDistance(TickSnapshot to) {
            if (to == null)
                return 0;
            return Math.max(Math.abs(x - to.x), Math.abs(y - to.y));
        }
    }

    private static final class SpawnStats {
        int planned;
        int created;
        int failed;
    }

    private void schedule() {
        ThreadPoolManager.getInstance().scheduleAtFixedRate(
                new Runnable()
                {
                	@Override
                	public void run()
                	{
                		tickQueue(active, 30);
                	}
                },
                1000, PhantomConfig.TICK_ACTIVE
        );
        ThreadPoolManager.getInstance().scheduleAtFixedRate(
                new Runnable()
                {
                	@Override
                	public void run()
                	{
                		tickQueue(idle, 30);
                	}
                },
                1000, PhantomConfig.TICK_IDLE
        );
        ThreadPoolManager.getInstance().scheduleAtFixedRate(
                new Runnable()
                {
                	@Override
                	public void run()
                	{
                		tickQueue(sleep, 60);
                	}
                },
                1000, PhantomConfig.TICK_SLEEP
        );
        ThreadPoolManager.getInstance().scheduleAtFixedRate(
                new Runnable()
                {
                	@Override
                	public void run()
                	{
                		rebalanceActiveCap();
                	}
                },
                2000, 2000
        );
    }

    private void tickQueue(Queue<PhantomBot> q, int batch) {
        for (int i = 0; i < batch; i++) {
            PhantomBot bot = q.poll();
            if (bot == null) return;

            try {
                TickSnapshot before = TickSnapshot.capture(bot);
                PhantomState stateBeforeTick = bot.state;
                String intentionBefore = PhantomAdapter.currentIntention(bot.actor);
                String targetBefore = bot.target != null ? (bot.target.getName() + "(" + bot.target.getObjectId() + ")") : "null";
                forceTrackedRoamIfNeeded(bot);
                forceTrackedDirectMoveProbeIfNeeded(bot, before, "tick");
                PhantomWorld.getInstance().brain().tick(bot);
                TickSnapshot afterBrain = TickSnapshot.capture(bot);
                String intentionAfterBrain = PhantomAdapter.currentIntention(bot.actor);
                updateState(bot);
                TickSnapshot afterState = TickSnapshot.capture(bot);
                updateProbeProgress(bot, before, afterState);
                diagnostic(bot, stateBeforeTick, intentionBefore, intentionAfterBrain, targetBefore, before, afterBrain, afterState);
            } catch (Throwable t) {
				_log.warn("Phantom tick failed for actor={}", bot.actor != null ? bot.actor.getName() : "null", t);
            }

            requeue(bot);
        }
    }

    private void updateState(PhantomBot bot) {
        Player p = bot.actor;

        if (System.currentTimeMillis() < bot.forceActiveUntilTs) {
            switchState(bot, PhantomState.ACTIVE, "force-active-after-spawn");
            return;
        }

        if (bot.firstTick) {
            bot.firstTick = false;
            bot.state = PhantomState.ACTIVE;
            return;
        }

        boolean hasTarget = bot.target != null && !PhantomAdapter.isDead(bot.target);
        boolean inCombat = PhantomAdapter.isInCombat(p);

        if (hasTarget || inCombat) {
            bot.noTargetTicks = 0;
            switchState(bot, PhantomState.ACTIVE, hasTarget ? "has-target" : "in-combat");
            return;
        }

        bot.noTargetTicks++;
        long inStateMs = System.currentTimeMillis() - bot.stateSinceTs;

        if (bot.state == PhantomState.ACTIVE && bot.noTargetTicks < 6) {
            return;
        }

        double distToSpot = PhantomAdapter.dist2D(p, bot.spot.centerX, bot.spot.centerY);
        if (distToSpot > PhantomConfig.LEASH_TO_SPOT * 0.6) {
            switchState(bot, PhantomState.IDLE, "return-to-spot");
            return;
        }

        if (inStateMs < 5000L)
            return;

        switchState(bot, ThreadLocalRandom.current().nextDouble() < 0.35 ? PhantomState.SLEEP : PhantomState.IDLE, "no-target-random");
    }


    private void switchState(PhantomBot bot, PhantomState next, String reason) {
        if (bot.state == next)
            return;

        PhantomState prev = bot.state;
        bot.state = next;
        bot.stateSinceTs = System.currentTimeMillis();

        if (_log.isDebugEnabled())
            _log.debug("[PHANTOM][state] actor={} {} -> {} reason={} noTargetTicks={}",
                    bot.actor != null ? bot.actor.getName() : "null", prev, next, reason, bot.noTargetTicks);
    }

    private void diagnostic(PhantomBot bot, PhantomState stateBeforeTick, String intentionBefore, String intentionAfterBrain, String targetBefore, TickSnapshot before, TickSnapshot afterBrain, TickSnapshot afterState) {
        Player p = bot.actor;
        if (p == null || !PhantomConfig.DEBUG || !_log.isDebugEnabled())
            return;

        boolean tracked = debugTracked.contains(p.getObjectId());
        if (!tracked)
            return;

        boolean hasTarget = bot.target != null && !PhantomAdapter.isDead(bot.target);
        double targetDistance = hasTarget ? PhantomAdapter.dist3D(p, bot.target) : -1d;

        _log.debug("[PHANTOM][diag] actor={} objId={} tracked={} stateBeforeTick={} stateAfterTick={} targetBefore={} targetAfter={} intentionBefore={} intentionAfterBrain={} intentionAfterState={} isMoving={} isInCombat={} distToTarget={} coordsBefore=({}, {}, {}) coordsAfterBrain=({}, {}, {}) coordsAfterState=({}, {}, {}) blockedFlags={} updateStateResult={} ",
                p.getName(), p.getObjectId(), tracked, stateBeforeTick, bot.state,
                targetBefore,
                bot.target != null ? (bot.target.getName() + "(" + bot.target.getObjectId() + ")") : "null",
                intentionBefore,
                intentionAfterBrain,
                PhantomAdapter.currentIntention(p),
                PhantomAdapter.isMoving(p),
                PhantomAdapter.isInCombat(p),
                targetDistance,
                before.x, before.y, before.z,
                afterBrain.x, afterBrain.y, afterBrain.z,
                afterState.x, afterState.y, afterState.z,
                PhantomAdapter.blockedStateSummary(p),
                bot.state.name());
    }


    private void forceTrackedRoamIfNeeded(PhantomBot bot) {
        Player p = bot.actor;
        if (p == null)
            return;
        if (!PhantomConfig.DEBUG || !debugTracked.contains(p.getObjectId()))
            return;
        if (bot.target != null && !PhantomAdapter.isDead(bot.target))
            return;

        long now = System.currentTimeMillis();
        if (now - bot.lastForcedRoamTs < TRACKED_FORCED_ROAM_INTERVAL_MS)
            return;

        int dx = ThreadLocalRandom.current().nextInt(401) - 200;
        int dy = ThreadLocalRandom.current().nextInt(401) - 200;
        boolean started = PhantomAdapter.moveTo(p, PhantomAdapter.loc(p.getX() + dx, p.getY() + dy, p.getZ()));
        bot.lastForcedRoamTs = now;

        _log.debug("[PHANTOM][forced-roam] actor={} objId={} to=({}, {}, {}) started={} snapshot={}",
                p.getName(), p.getObjectId(), p.getX() + dx, p.getY() + dy, p.getZ(), started, PhantomAdapter.movementDebugSnapshot(p));
    }

    private void forceTrackedDirectMoveProbeIfNeeded(PhantomBot bot, TickSnapshot before, String reason) {
        Player p = bot.actor;
        if (p == null)
            return;
        if (!PhantomConfig.DEBUG || !debugTracked.contains(p.getObjectId()))
            return;

        long now = System.currentTimeMillis();
        if (now - bot.lastForcedProbeTs < TRACKED_FORCED_PROBE_INTERVAL_MS)
            return;

        int dx = ThreadLocalRandom.current().nextInt(241) - 120;
        int dy = ThreadLocalRandom.current().nextInt(241) - 120;
        bot.forcedProbeTicksLeft = FORCED_PROBE_TICKS;
        bot.forcedProbeStartX = before.x;
        bot.forcedProbeStartY = before.y;
        bot.forcedProbeStartZ = before.z;
        bot.forcedProbeReason = reason;
        boolean started = PhantomAdapter.moveTo(p, PhantomAdapter.loc(p.getX() + dx, p.getY() + dy, p.getZ()));
        bot.lastForcedProbeTs = now;

        _log.debug("[PHANTOM][forced-probe-start] actor={} objId={} reason={} started={} from=({}, {}, {}) to=({}, {}, {}) intention={} isMoving={} blockedFlags={}",
                p.getName(), p.getObjectId(), reason, started,
                before.x, before.y, before.z,
                p.getX() + dx, p.getY() + dy, p.getZ(),
                PhantomAdapter.currentIntention(p), PhantomAdapter.isMoving(p), PhantomAdapter.blockedStateSummary(p));
    }

    private void updateProbeProgress(PhantomBot bot, TickSnapshot before, TickSnapshot afterState) {
        Player p = bot.actor;
        if (p == null)
            return;
        if (!PhantomConfig.DEBUG || !debugTracked.contains(p.getObjectId()))
            return;
        if (bot.forcedProbeTicksLeft <= 0)
            return;

        bot.forcedProbeTicksLeft--;
        _log.debug("[PHANTOM][forced-probe-tick] actor={} objId={} ticksLeft={} reason={} coordsBeforeTick=({}, {}, {}) coordsAfterTick=({}, {}, {}) intention={} isMoving={} moveTaskStartedCheck={} blockedFlags={}",
                p.getName(), p.getObjectId(), bot.forcedProbeTicksLeft, bot.forcedProbeReason,
                before.x, before.y, before.z,
                afterState.x, afterState.y, afterState.z,
                PhantomAdapter.currentIntention(p), PhantomAdapter.isMoving(p),
                (before.chebyshevDistance(afterState) > 0), PhantomAdapter.blockedStateSummary(p));

        if (bot.forcedProbeTicksLeft == 0) {
            int moved = Math.max(Math.abs(afterState.x - bot.forcedProbeStartX), Math.abs(afterState.y - bot.forcedProbeStartY));
            if (moved <= 3)
                _log.warn("phantom direct move failed: coords unchanged after forced move probe actor={} objId={} start=({}, {}, {}) end=({}, {}, {}) intention={} isMoving={} blockedFlags={}",
                        p.getName(), p.getObjectId(),
                        bot.forcedProbeStartX, bot.forcedProbeStartY, bot.forcedProbeStartZ,
                        afterState.x, afterState.y, afterState.z,
                        PhantomAdapter.currentIntention(p), PhantomAdapter.isMoving(p), PhantomAdapter.blockedStateSummary(p));
            else
                _log.debug("[PHANTOM][forced-probe-ok] actor={} objId={} moved={} start=({}, {}, {}) end=({}, {}, {}) intention={} isMoving={}",
                        p.getName(), p.getObjectId(), moved,
                        bot.forcedProbeStartX, bot.forcedProbeStartY, bot.forcedProbeStartZ,
                        afterState.x, afterState.y, afterState.z,
                        PhantomAdapter.currentIntention(p), PhantomAdapter.isMoving(p));
        }
    }

    private void requeue(PhantomBot bot) {
		switch (bot.state)
		{
			case ACTIVE:
				active.add(bot);
				break;
			case IDLE:
				idle.add(bot);
				break;
			case SLEEP:
				sleep.add(bot);
				break;
			default:
				idle.add(bot);
		}
    }

    private void rebalanceActiveCap() {
        int cap = PhantomConfig.ACTIVE_CAP;

        while (active.size() < cap) {
            PhantomBot b = idle.poll();
            if (b == null) b = sleep.poll();
            if (b == null) break;
            switchState(b, PhantomState.ACTIVE, "rebalance-up");
            active.add(b);
        }

        while (active.size() > cap) {
            PhantomBot b = active.poll();
            if (b == null) break;
            switchState(b, PhantomState.IDLE, "rebalance-down");
            idle.add(b);
        }
    }
}
