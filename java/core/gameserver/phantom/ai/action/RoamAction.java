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
        var r = ThreadLocalRandom.current();
        int dx = r.nextInt(-ctx.bot.spot.radius, ctx.bot.spot.radius + 1);
        int dy = r.nextInt(-ctx.bot.spot.radius, ctx.bot.spot.radius + 1);
        PhantomAdapter.moveTo(ctx.actor,
                PhantomAdapter.loc(ctx.bot.spot.centerX + dx, ctx.bot.spot.centerY + dy, ctx.bot.spot.centerZ));
    }
}