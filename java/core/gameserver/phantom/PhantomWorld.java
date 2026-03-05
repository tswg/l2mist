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

    private PhantomWorld() {}

    public PhantomBrain brain() { return brain; }

    public void loadAll() {
        PhantomConfig.load("config/phantom/phantom.properties");
        spots.clear();
        profiles.clear();
        spots.addAll(SpotParser.load("config/phantom/phantom_spots.ini"));
        profiles.addAll(ProfileParser.load("config/phantom/phantom_profiles.ini"));

        if (spots.isEmpty()) {
			_log.warn("No phantom spots loaded from config/phantom/phantom_spots.ini");
		}
        if (profiles.isEmpty()) {
			_log.warn("No phantom profiles loaded from config/phantom/phantom_profiles.ini");
		}
    }

    // ---- ВАЖНО: тут ты подключаешь свою реальную фабрику фантомов ----
    public Player spawnPhantomActor() {
        _log.warn("spawnPhantomActor() is not bound to First Team phantom factory yet; returning null fallback");
        return null;
    }

    public PhantomSpot pickSpotFor(Player p) {
        int lvl = PhantomAdapter.level(p);
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
