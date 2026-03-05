package core.gameserver.phantom;

import core.gameserver.phantom.ai.PhantomContext;

public class RetreatAction implements PhantomAction {
    @Override public boolean canRun(PhantomContext ctx) {
        double hp = ctx.actor.getCurrentHp() / ctx.actor.getMaxHp();
        if (hp > Config.PHANTOM_RETREAT_HP) return false;

        // далеко от спота или слишком много агра
        if (ctx.actor.distance2D(ctx.spot.centerX, ctx.spot.centerY) > Config.PHANTOM_LEASH_TO_SPOT)
            return true;

        return ctx.actor.getAggroList().getActiveCharList().size() >= 3;
    }

    @Override public void run(PhantomContext ctx) {
        // простое отступление: уйти к центру спота
        ctx.actor.getAI().setIntention(CtrlIntention.AI_INTENTION_MOVE_TO,
                new Location(ctx.spot.centerX, ctx.spot.centerY, ctx.spot.centerZ));
    }

    @Override public int priority() { return 1000; }
}