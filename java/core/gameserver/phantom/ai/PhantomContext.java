package core.gameserver.phantom.ai;

import core.gameserver.model.Creature;
import core.gameserver.model.Player;
import core.gameserver.phantom.PhantomSpot;
import core.gameserver.phantom.model.PhantomBot;

public final class PhantomContext {
    public final PhantomBot bot;
    public final Player actor;

    public Creature target;

    public PhantomContext(PhantomBot bot, Player actor) {
        this.bot = bot;
        this.actor = actor;
        this.target = bot.target;
    }

    public void syncBack() {
        bot.target = target;
    }

    public static PhantomContext from(PhantomBot bot) {
        return new PhantomContext(bot, (Player) bot.actor);
    }
}