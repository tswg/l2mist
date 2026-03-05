package core.gameserver.phantom.model;

public final class PhantomSpot {
    public final int id;
    public final String name;
    public final String type; // FARM (пока строкой)
    public final int centerX, centerY, centerZ;
    public final int radius;
    public final int minLvl, maxLvl;
    public final int maxCount;

    public PhantomSpot(int id, String name, String type,
                       int centerX, int centerY, int centerZ,
                       int radius, int minLvl, int maxLvl, int maxCount) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.centerX = centerX;
        this.centerY = centerY;
        this.centerZ = centerZ;
        this.radius = radius;
        this.minLvl = minLvl;
        this.maxLvl = maxLvl;
        this.maxCount = maxCount;
    }
}