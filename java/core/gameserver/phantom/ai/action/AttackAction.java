package core.gameserver.phantom.ai.action;

import core.gameserver.model.Creature;
import core.gameserver.phantom.PhantomAdapter;
import core.gameserver.phantom.PhantomConfig;
import core.gameserver.phantom.ai.PhantomContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AttackAction implements PhantomAction {
    private static final Logger _log = LoggerFactory.getLogger(AttackAction.class);
    @Override
    public boolean canRun(PhantomContext ctx) {
        Creature t = ctx.target;
        if (t == null || t.isDead()) return false;
        double hp = PhantomAdapter.hpRatio(ctx.actor);
        if (hp <= PhantomConfig.RETREAT_AT_HP) return false;
        double d = PhantomAdapter.dist3D(ctx.actor, t);
        return d <= 220;
    }

    @Override
    public int priority(PhantomContext ctx) { return 500; }

    @Override
    public void run(PhantomContext ctx) {
        if (_log.isInfoEnabled())
            _log.info("[PHANTOM][AttackAction] actor={} objectId={} target={} dist={}",
                    ctx.actor != null ? ctx.actor.getName() : "null",
                    ctx.actor != null ? ctx.actor.getObjectId() : 0,
                    ctx.target != null ? (ctx.target.getName() + "(" + ctx.target.getObjectId() + ")") : "null",
                    ctx.target != null ? String.format("%.2f", PhantomAdapter.dist3D(ctx.actor, ctx.target)) : "n/a");
        PhantomAdapter.attack(ctx.actor, ctx.target);
    }
}