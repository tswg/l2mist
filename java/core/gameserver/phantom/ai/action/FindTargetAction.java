package core.gameserver.phantom.ai.action;

import core.gameserver.phantom.PhantomConfig;
import core.gameserver.phantom.PhantomAdapter;
import core.gameserver.phantom.ai.PhantomContext;
import core.gameserver.phantom.ai.target.Targeting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FindTargetAction implements PhantomAction {
    private static final Logger _log = LoggerFactory.getLogger(FindTargetAction.class);
    @Override
    public boolean canRun(PhantomContext ctx) {
        return ctx.target == null || PhantomAdapter.isDead(ctx.target) || !PhantomAdapter.isVisible(ctx.target);
    }

    @Override
    public int priority(PhantomContext ctx) { return 600; }

    @Override
    public void run(PhantomContext ctx) {
        String before = ctx.target != null ? (ctx.target.getName() + "(" + ctx.target.getObjectId() + ")") : "null";
        ctx.target = Targeting.findBestMob(ctx.actor, PhantomConfig.SEARCH_RADIUS);
        if (ctx.actor != null)
            ctx.actor.setTarget(ctx.target);

        String after = ctx.target != null ? (ctx.target.getName() + "(" + ctx.target.getObjectId() + ")") : "null";
        if (_log.isDebugEnabled() && PhantomConfig.DEBUG)
            _log.debug("[PHANTOM][FindTargetAction] actor={} objectId={} searchRadius={} targetBefore={} selectedTarget={}",
                    ctx.actor != null ? ctx.actor.getName() : "null",
                    ctx.actor != null ? ctx.actor.getObjectId() : 0,
                    PhantomConfig.SEARCH_RADIUS,
                    before,
                    after);
        ctx.syncBack();

        if (_log.isDebugEnabled() && PhantomConfig.DEBUG)
            _log.debug("[PHANTOM][FindTargetAction] actor={} objectId={} syncedBotTarget={}",
                    ctx.actor != null ? ctx.actor.getName() : "null",
                    ctx.actor != null ? ctx.actor.getObjectId() : 0,
                    ctx.bot.target != null ? (ctx.bot.target.getName() + "(" + ctx.bot.target.getObjectId() + ")") : "null");

        if (ctx.target != null && ctx.bot.target == null)
            _log.warn("[PHANTOM][FindTargetActionSync] actor={} objectId={} target-picked-but-lost-after-sync",
                    ctx.actor != null ? ctx.actor.getName() : "null",
                    ctx.actor != null ? ctx.actor.getObjectId() : 0);
    }
}
