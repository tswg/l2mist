package core.gameserver.phantom.ai.action;

import core.gameserver.model.Player;
import core.gameserver.phantom.PhantomAdapter;
import core.gameserver.phantom.PhantomConfig;
import core.gameserver.phantom.ai.PhantomContext;

public class UseShotsAction implements PhantomAction {

    @Override
    public boolean canRun(PhantomContext ctx) {
        if (!PhantomConfig.USE_SHOTS) return false;
        if (ctx.target == null || PhantomAdapter.isDead(ctx.target)) return false;

        long now = System.currentTimeMillis();
        if (now - ctx.bot.lastShotsTs < PhantomConfig.SHOTS_COOLDOWN_MS) return false;

        // Если у тебя есть способ проверить, что SS уже заряжены — можно добавить.
        return true;
    }

    @Override
    public int priority(PhantomContext ctx) {
        return 800; // выше атаки, но ниже ретрита
    }

    @Override
    public void run(PhantomContext ctx) {
        Player p = ctx.actor;

        // Для милика — Soulshot, для мага — Spiritshot (позже по профилю)
        int itemId = PhantomConfig.SOULSHOT_ITEM_ID;

        boolean ok = PhantomAdapter.useItem(p, itemId);
        if (ok) ctx.bot.lastShotsTs = System.currentTimeMillis();
    }
}
