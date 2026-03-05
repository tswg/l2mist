package core.gameserver.phantom.model;

import core.gameserver.model.Creature;
import core.gameserver.phantom.PhantomSpot;
import core.gameserver.phantom.PhantomState;

public class PhantomBot {
    public final Object actor;   // Player
    public final PhantomSpot spot;
    public long lastPotionTs = 0L;
    public long lastShotsTs = 0L;

    public PhantomState state = PhantomState.IDLE;

    public Creature target;

    // stuck
    public int lastX, lastY, lastZ;
    public long lastMoveTs;

    public PhantomBot(Object actor, PhantomSpot spot) {
        this.actor = actor;
        this.spot = spot;
        this.lastMoveTs = System.currentTimeMillis();
    }
}