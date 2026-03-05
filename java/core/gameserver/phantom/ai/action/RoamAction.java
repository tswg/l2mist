package core.gameserver.phantom.ai.action;

import core.gameserver.phantom.PhantomAdapter;
import core.gameserver.phantom.ai.PhantomContext;

import java.util.concurrent.ThreadLocalRandom;

public class RoamAction implements PhantomAction {
    @Override
    public boolean canRun(PhantomContext ctx) {
        return ctx.target == null;
    }

    @Override
    public int priority(PhantomContext ctx) { return 100; }

    @Override
    public void run(PhantomContext ctx) {
		ThreadLocalRandom r = ThreadLocalRandom.current();
		int span = ctx.bot.spot.radius * 2 + 1;
		int dx = r.nextInt(span) - ctx.bot.spot.radius;
		int dy = r.nextInt(span) - ctx.bot.spot.radius;
        PhantomAdapter.moveTo(ctx.actor,
                PhantomAdapter.loc(ctx.bot.spot.centerX + dx, ctx.bot.spot.centerY + dy, ctx.bot.spot.centerZ));
    }
}
