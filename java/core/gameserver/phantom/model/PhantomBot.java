package core.gameserver.phantom.model;

import core.gameserver.model.Creature;
import core.gameserver.model.Player;
import core.gameserver.phantom.PhantomState;

public class PhantomBot {
    public final Player actor;
    public final PhantomSpot spot;
    public long lastPotionTs = 0L;
    public long lastShotsTs = 0L;

    public PhantomState state = PhantomState.IDLE;
    public long stateSinceTs = System.currentTimeMillis();
    public int noTargetTicks = 0;

    public Creature target;
    public boolean firstTick = true;
    public long forceActiveUntilTs;

    // stuck
    public int lastX, lastY, lastZ;
    public long lastMoveTs;

    public PhantomBot(Player actor, PhantomSpot spot) {
        this.actor = actor;
        this.spot = spot;
        this.lastMoveTs = System.currentTimeMillis();
        this.forceActiveUntilTs = this.lastMoveTs + 8000L;
    }
}
