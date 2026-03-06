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
        int attackRange = ctx.actor != null ? ctx.actor.getPhysicalAttackRange() + 40 : 160;
        return d <= attackRange;
    }

    @Override
    public int priority(PhantomContext ctx) { return 500; }

    @Override
    public void run(PhantomContext ctx) {
        double dist = PhantomAdapter.dist3D(ctx.actor, ctx.target);
        int attackRange = ctx.actor != null ? ctx.actor.getPhysicalAttackRange() + 40 : 160;
        if (dist > attackRange) {
            PhantomAdapter.moveTo(ctx.actor, PhantomAdapter.location(ctx.target));
            if (_log.isDebugEnabled() && PhantomConfig.DEBUG)
                _log.debug("[PHANTOM][AttackAction] actor={} objectId={} move-before-attack target={} dist={} range={}",
                        ctx.actor != null ? ctx.actor.getName() : "null",
                        ctx.actor != null ? ctx.actor.getObjectId() : 0,
                        ctx.target != null ? (ctx.target.getName() + "(" + ctx.target.getObjectId() + ")") : "null",
                        String.format("%.2f", dist),
                        attackRange);
            return;
        }

        if (_log.isDebugEnabled() && PhantomConfig.DEBUG)
            _log.debug("[PHANTOM][AttackAction] actor={} objectId={} target={} dist={} range={}",
                    ctx.actor != null ? ctx.actor.getName() : "null",
                    ctx.actor != null ? ctx.actor.getObjectId() : 0,
                    ctx.target != null ? (ctx.target.getName() + "(" + ctx.target.getObjectId() + ")") : "null",
                    String.format("%.2f", dist),
                    attackRange);
        PhantomAdapter.attack(ctx.actor, ctx.target);
    }
}
