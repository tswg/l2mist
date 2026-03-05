package core.gameserver.phantom.ai.action;

import core.gameserver.model.Player;
import core.gameserver.phantom.PhantomAdapter;
import core.gameserver.phantom.PhantomConfig;
import core.gameserver.phantom.ai.PhantomContext;

public class UsePotionsAction implements PhantomAction {

    @Override
    public boolean canRun(PhantomContext ctx) {
        if (!PhantomConfig.USE_POTIONS) return false;

        long now = System.currentTimeMillis();
        if (now - ctx.bot.lastPotionTs < PhantomConfig.POTION_COOLDOWN_MS) return false;

        double hp = PhantomAdapter.hpRatio(ctx.actor);
        if (hp <= PhantomConfig.MIN_HP_POTION_RATIO) return true;

        // MP часть можно включить позже, когда будет маг-профиль
        // double mp = ctx.actor.getCurrentMp() / ctx.actor.getMaxMp();
        // if (mp <= PhantomConfig.MIN_MP_POTION_RATIO) return true;

        return false;
    }

    @Override
    public int priority(PhantomContext ctx) {
        return 950; // почти как retreat, но ниже него
    }

    @Override
    public void run(PhantomContext ctx) {
        Player p = ctx.actor;

        double hp = PhantomAdapter.hpRatio(p);
        int itemId = (hp <= PhantomConfig.MIN_HP_POTION_RATIO)
                ? PhantomConfig.HP_POTION_ITEM_ID
                : PhantomConfig.MP_POTION_ITEM_ID;

        boolean ok = PhantomAdapter.useItem(p, itemId);
        if (ok) ctx.bot.lastPotionTs = System.currentTimeMillis();
    }
}