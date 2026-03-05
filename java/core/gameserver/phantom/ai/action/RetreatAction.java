package core.gameserver.phantom.ai.action;

import core.gameserver.model.Player;
import core.gameserver.phantom.PhantomAdapter;
import core.gameserver.phantom.PhantomConfig;
import core.gameserver.phantom.ai.PhantomContext;

public class RetreatAction implements PhantomAction {
    @Override
    public boolean canRun(PhantomContext ctx) {
        Player p = ctx.actor;

        double hp = PhantomAdapter.hpRatio(p);
        if (hp <= PhantomConfig.RETREAT_AT_HP) return true;

        double distToSpot = PhantomAdapter.dist2D(p, ctx.bot.spot.centerX, ctx.bot.spot.centerY);
        return distToSpot > PhantomConfig.LEASH_TO_SPOT;
    }

    @Override
    public int priority(PhantomContext ctx) { return 1000; }

    @Override
    public void run(PhantomContext ctx) {
        ctx.target = null;
        PhantomAdapter.moveTo(ctx.actor,
                PhantomAdapter.loc(ctx.bot.spot.centerX, ctx.bot.spot.centerY, ctx.bot.spot.centerZ));
        ctx.syncBack();
    }
}