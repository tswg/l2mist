package core.gameserver.phantom;

import core.gameserver.ai.CtrlIntention;
import core.gameserver.model.Creature;
import core.gameserver.model.Player;
import core.gameserver.phantom.ai.PhantomContext;

public class AttackAction implements PhantomAction {
    @Override public boolean canRun(PhantomContext ctx) {
        return ctx.target != null && !ctx.target.isDead();
    }

    @Override public void run(PhantomContext ctx) {
        final Player a = ctx.actor;
        final Creature t = ctx.target;

        if (a.distance3D(t) > 120) {
            a.getAI().setIntention(CtrlIntention.AI_INTENTION_MOVE_TO, t.getLoc());
            return;
        }
        a.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, t);
    }

    @Override public int priority() { return 500; }
}