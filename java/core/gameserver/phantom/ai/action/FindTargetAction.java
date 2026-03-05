package core.gameserver.phantom.ai.action;

import core.gameserver.phantom.PhantomConfig;
import core.gameserver.phantom.PhantomAdapter;
import core.gameserver.phantom.ai.PhantomContext;
import core.gameserver.phantom.ai.target.Targeting;

public class FindTargetAction implements PhantomAction {
    @Override
    public boolean canRun(PhantomContext ctx) {
        return ctx.target == null || PhantomAdapter.isDead(ctx.target) || !PhantomAdapter.isVisible(ctx.target);
    }

    @Override
    public int priority(PhantomContext ctx) { return 600; }

    @Override
    public void run(PhantomContext ctx) {
        ctx.target = Targeting.findBestMob(ctx.actor, PhantomConfig.SEARCH_RADIUS);
        ctx.syncBack();
    }
}
