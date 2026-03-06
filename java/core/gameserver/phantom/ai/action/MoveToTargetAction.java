package core.gameserver.phantom.ai.action;

import core.gameserver.model.Creature;
import core.gameserver.phantom.PhantomAdapter;
import core.gameserver.phantom.PhantomConfig;
import core.gameserver.phantom.ai.PhantomContext;

public class MoveToTargetAction implements PhantomAction {
    @Override
    public boolean canRun(PhantomContext ctx) {
        Creature t = ctx.target;
        if (t == null || PhantomAdapter.isDead(t)) return false;

        double d = PhantomAdapter.dist3D(ctx.actor, t);
        return d > 160 && d < PhantomConfig.PURSUIT_RANGE;
    }

    @Override
    public int priority(PhantomContext ctx) { return 520; }

    @Override
    public void run(PhantomContext ctx) {
		PhantomAdapter.moveTo(ctx.actor, PhantomAdapter.location(ctx.target));
    }
}
