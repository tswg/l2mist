package core.gameserver.phantom;

import core.gameserver.utils.Location;

public class PhantomSpot {
    final int minLvl;
    final int maxLvl;
    final Location loc;
    final int radius;
    final int weight;

    PhantomSpot(int minLvl, int maxLvl, int x, int y, int z, int radius, int weight) {
        this.minLvl = minLvl;
        this.maxLvl = maxLvl;
        this.loc = new Location(x, y, z);
        this.radius = radius;
        this.weight = weight;
    }
    boolean fits(int lvl) {
        return lvl >= minLvl && lvl <= maxLvl;
    }
}
