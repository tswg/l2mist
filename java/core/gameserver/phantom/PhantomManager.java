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
        PhantomWorld.getInstance().loadAll();
        spawnInitial();
        schedule();
		_log.info("PhantomManager initialized: active={}, idle={}, sleep={}", active.size(), idle.size(), sleep.size());
    }

    private void spawnInitial() {
        int total = PhantomConfig.TOTAL_ONLINE;
        int activeCap = PhantomConfig.ACTIVE_CAP;

        for (int i = 0; i < total; i++) {
            Player p = PhantomWorld.getInstance().spawnPhantomActor();
            if (p == null) {
				_log.warn("Phantom spawn returned null, skip index={}", i);
				continue;
			}
            PhantomSpot spot = PhantomWorld.getInstance().pickSpotFor(p);
            if (spot == null) {
				_log.warn("No spot found for phantom={}, skip", p.getName());
				continue;
			}

            // стартовое размещение
            PhantomAdapter.moveTo(p, PhantomAdapter.loc(spot.centerX, spot.centerY, spot.centerZ));

            PhantomBot bot = new PhantomBot(p, spot);

            if (i < activeCap) {
                bot.state = PhantomState.ACTIVE;
                active.add(bot);
            } else {
                bot.state = PhantomState.SLEEP;
                sleep.add(bot);
            }
        }
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
            } catch (Throwable t) {
				_log.warn("Phantom tick failed for actor={}", bot.actor != null ? bot.actor.getName() : "null", t);
            }

            requeue(bot);
        }
    }

    private void updateState(PhantomBot bot) {
        Player p = bot.actor;

        // если есть цель/в бою -> ACTIVE
        boolean hasTarget = bot.target != null && !PhantomAdapter.isDead(bot.target);
        boolean inCombat = PhantomAdapter.isInCombat(p);

        if (hasTarget || inCombat) {
            bot.state = PhantomState.ACTIVE;
            return;
        }

        // если далеко от спота — IDLE (идёт домой)
        double distToSpot = PhantomAdapter.dist2D(p, bot.spot.centerX, bot.spot.centerY);
        if (distToSpot > PhantomConfig.LEASH_TO_SPOT * 0.6) {
            bot.state = PhantomState.IDLE;
            return;
        }

        // иначе часть спит, часть idle (для “жизни”)
        bot.state = ThreadLocalRandom.current().nextDouble() < 0.35
                ? PhantomState.SLEEP
                : PhantomState.IDLE;
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
            b.state = PhantomState.ACTIVE;
            active.add(b);
        }

        // если активных много — скидываем “лишних” в idle
        while (active.size() > cap) {
            PhantomBot b = active.poll();
            if (b == null) break;
            b.state = PhantomState.IDLE;
            idle.add(b);
        }
    }
}
