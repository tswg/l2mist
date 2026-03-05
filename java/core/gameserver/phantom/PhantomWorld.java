package core.gameserver.phantom;

import  core.gameserver.model.Player;
import  core.gameserver.phantom.ai.PhantomBrain;
import  core.gameserver.phantom.io.ProfileParser;
import  core.gameserver.phantom.io.SpotParser;
import  core.gameserver.phantom.model.PhantomProfile;
import  core.gameserver.phantom.model.PhantomSpot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public final class PhantomWorld {
    private static final Logger _log = LoggerFactory.getLogger(PhantomWorld.class);

    private static final PhantomWorld INSTANCE = new PhantomWorld();
    public static PhantomWorld getInstance() { return INSTANCE; }

    private final List<PhantomSpot> spots = new ArrayList<>();
    private final List<PhantomProfile> profiles = new ArrayList<>();
    private final PhantomBrain brain = new PhantomBrain();
    private PhantomSpawner spawner;

    private PhantomWorld() {}

    public PhantomBrain brain() { return brain; }

    public int spotsCount() { return spots.size(); }

    public int profilesCount() { return profiles.size(); }

    public int candidatesCount() { return spawner != null ? spawner.candidatesCount() : 0; }

    public int pickRandomLevel() {
        PhantomProfile p = pickAnyProfile();
        if (p == null)
            return 1;
        return ThreadLocalRandom.current().nextInt(p.minLvl, p.maxLvl + 1);
    }

    private PhantomProfile pickAnyProfile() {
        if (profiles.isEmpty())
            return null;
        return profiles.get(ThreadLocalRandom.current().nextInt(profiles.size()));
    }

    public void loadAll() {
        PhantomConfig.load("config/phantom/phantom.properties");
        spots.clear();
        profiles.clear();
        spots.addAll(SpotParser.load("config/phantom/phantom_spots.ini"));
        profiles.addAll(ProfileParser.load("config/phantom/phantom_profiles.ini"));

        _log.info("PhantomWorld config loaded: spots={}, profiles={}", spots.size(), profiles.size());

        if (spots.isEmpty()) {
			_log.warn("No phantom spots loaded from config/phantom/phantom_spots.ini");
		}
        if (profiles.isEmpty()) {
			_log.warn("No phantom profiles loaded from config/phantom/phantom_profiles.ini");
		}

        spawner = new PhantomSpawner();
        spawner.prepare();
    }

    public Player spawnPhantomActor(PhantomSpot spot) {
        if (spawner == null)
            throw new IllegalStateException("Phantom spawner is not prepared");
        if (spot == null)
            throw new IllegalArgumentException("Phantom spot is null");

        PhantomProfile profile = pickProfileForLevel((spot.minLvl + spot.maxLvl) / 2);
        return spawner.spawn(spot, profile);
    }

    public PhantomSpot pickSpotFor(Player p) {
        int lvl = PhantomAdapter.level(p);
        return pickSpotForLevel(lvl);
    }

    public PhantomSpot pickSpotForLevel(int lvl) {
        // выбираем подходящие по уровню
        List<PhantomSpot> ok = new ArrayList<>();
        for (PhantomSpot s : spots) {
            if (lvl >= s.minLvl && lvl <= s.maxLvl) ok.add(s);
        }
        if (ok.isEmpty()) {
			if(spots.isEmpty())
				return null;
            return spots.get(ThreadLocalRandom.current().nextInt(spots.size()));
        }
        return ok.get(ThreadLocalRandom.current().nextInt(ok.size()));
    }

    public PhantomProfile pickProfileForLevel(int level) {
        List<PhantomProfile> ok = new ArrayList<>();
        for (PhantomProfile pr : profiles) {
            if (level >= pr.minLvl && level <= pr.maxLvl) ok.add(pr);
        }
        if (ok.isEmpty()) {
			if(profiles.isEmpty())
				return null;
			return profiles.get(0);
		}
        return ok.get(ThreadLocalRandom.current().nextInt(ok.size()));
    }
}
