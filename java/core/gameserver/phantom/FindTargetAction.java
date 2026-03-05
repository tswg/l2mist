package core.gameserver.phantom;

import core.gameserver.phantom.ai.PhantomContext;

public class FindTargetAction implements PhantomAction {
    @Override public boolean canRun(PhantomContext ctx) {
        return ctx.target == null || ctx.target.isDead() || !ctx.target.isVisible();
    }

    @Override public void run(PhantomContext ctx) {
        ctx.target = PhantomTargeting.findBestMob(ctx.actor, ctx.spot, Config.PHANTOM_SEARCH_RADIUS);
    }

    @Override public int priority() { return 600; }
}