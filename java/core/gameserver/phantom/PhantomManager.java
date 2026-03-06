package core.gameserver.phantom;

import core.gameserver.ThreadPoolManager;
import core.gameserver.model.Player;
import core.gameserver.phantom.model.PhantomBot;
import core.gameserver.phantom.model.PhantomSpot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ThreadLocalRandom;

public final class PhantomManager {
	private static final Logger _log = LoggerFactory.getLogger(PhantomManager.class);

    private static final PhantomManager INSTANCE = new PhantomManager();
    public static PhantomManager getInstance() { return INSTANCE; }

    private final Queue<PhantomBot> active = new ConcurrentLinkedQueue<>();
    private final Queue<PhantomBot> idle   = new ConcurrentLinkedQueue<>();
    private final Queue<PhantomBot> sleep  = new ConcurrentLinkedQueue<>();

    private PhantomManager() {}

    public void init() {
        System.out.println("[PHANTOM] PhantomManager init start");
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

            if (i < activeCap) {
                switchState(bot, PhantomState.ACTIVE, "spawn-active-cap");
                active.add(bot);
            } else {
                switchState(bot, PhantomState.SLEEP, "spawn-over-cap");
                sleep.add(bot);
            }
        }
        return stats;
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
                PhantomWorld.getInstance().brain().tick(bot);
                updateState(bot);
                diagnostic(bot);
            } catch (Throwable t) {
				_log.warn("Phantom tick failed for actor={}", bot.actor != null ? bot.actor.getName() : "null", t);
            }

            requeue(bot);
        }
    }

    private void updateState(PhantomBot bot) {
        Player p = bot.actor;

        if (bot.firstTick) {
            bot.firstTick = false;
            bot.state = PhantomState.ACTIVE;
            return;
        }

        // если есть цель/в бою -> ACTIVE
        boolean hasTarget = bot.target != null && !PhantomAdapter.isDead(bot.target);
        boolean inCombat = PhantomAdapter.isInCombat(p);

        if (hasTarget || inCombat) {
            bot.noTargetTicks = 0;
            switchState(bot, PhantomState.ACTIVE, hasTarget ? "has-target" : "in-combat");
            return;
        }

        bot.noTargetTicks++;
        long inStateMs = System.currentTimeMillis() - bot.stateSinceTs;

        // не усыпляем слишком рано: минимум несколько тиков без цели.
        if (bot.state == PhantomState.ACTIVE && bot.noTargetTicks < 6) {
            return;
        }

        // если далеко от спота — IDLE (идёт домой)
        double distToSpot = PhantomAdapter.dist2D(p, bot.spot.centerX, bot.spot.centerY);
        if (distToSpot > PhantomConfig.LEASH_TO_SPOT * 0.6) {
            switchState(bot, PhantomState.IDLE, "return-to-spot");
            return;
        }

        // иначе часть спит, часть idle (для “жизни”), но не чаще чем раз в 5 секунд.
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

    private void diagnostic(PhantomBot bot) {
        Player p = bot.actor;
        if (p == null || !PhantomAdapter.shouldSampleDiagnostic())
            return;

        boolean hasTarget = bot.target != null && !PhantomAdapter.isDead(bot.target);
        double targetDistance = hasTarget ? PhantomAdapter.dist3D(p, bot.target) : -1d;

        _log.debug("[PHANTOM][diag] actor={} state={} hasTarget={} targetDistance={} intention={} isMoving={} isInCombat={} activeWeapon={}",
                p.getName(), bot.state, hasTarget, targetDistance,
                PhantomAdapter.currentIntention(p),
                PhantomAdapter.isMoving(p),
                PhantomAdapter.isInCombat(p),
                PhantomAdapter.activeWeapon(p));
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
        // держим active примерно около ACTIVE_CAP
        int cap = PhantomConfig.ACTIVE_CAP;

        // если активных мало — поднимаем из idle/sleep
        while (active.size() < cap) {
            PhantomBot b = idle.poll();
            if (b == null) b = sleep.poll();
            if (b == null) break;
            switchState(b, PhantomState.ACTIVE, "rebalance-up");
            active.add(b);
        }

        // если активных много — скидываем “лишних” в idle
        while (active.size() > cap) {
            PhantomBot b = active.poll();
            if (b == null) break;
            switchState(b, PhantomState.IDLE, "rebalance-down");
            idle.add(b);
        }
    }
}
