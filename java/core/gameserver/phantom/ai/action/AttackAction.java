package core.gameserver.phantom.ai.action;

import core.gameserver.model.Creature;
import core.gameserver.phantom.PhantomAdapter;
import core.gameserver.phantom.PhantomConfig;
import core.gameserver.phantom.ai.PhantomContext;

public class AttackAction implements PhantomAction {
    @Override
    public boolean canRun(PhantomContext ctx) {
        Creature t = ctx.target;
        if (t == null || t.isDead()) return false;
        double hp = PhantomAdapter.hpRatio(ctx.actor);
        if (hp <= PhantomConfig.RETREAT_AT_HP) return false;
        double d = PhantomAdapter.dist3D(ctx.actor, t);
        return d <= 160;
    }

    @Override
    public int priority(PhantomContext ctx) { return 500; }

    @Override
    public void run(PhantomContext ctx) {
        PhantomAdapter.attack(ctx.actor, ctx.target);
    }
}