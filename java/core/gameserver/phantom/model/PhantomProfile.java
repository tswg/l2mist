package core.gameserver.phantom.model;

public final class PhantomProfile {
    public final int id;
    public final String name;
    public final String archetype; // FARM_MELEE и т.д.
    public final int minLvl, maxLvl;
    public final double aggression;

    public PhantomProfile(int id, String name, String archetype, int minLvl, int maxLvl, double aggression) {
        this.id = id;
        this.name = name;
        this.archetype = archetype;
        this.minLvl = minLvl;
        this.maxLvl = maxLvl;
        this.aggression = aggression;
    }
}