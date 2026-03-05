package core.gameserver.phantom;

import  core.gameserver.model.Player;
import  core.gameserver.phantom.ai.PhantomBrain;
import  core.gameserver.phantom.io.ProfileParser;
import  core.gameserver.phantom.io.SpotParser;
import  core.gameserver.phantom.model.PhantomProfile;
import  core.gameserver.phantom.model.PhantomSpot;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public final class PhantomWorld {
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

        if (spots.isEmpty()) throw new IllegalStateException("No phantom spots loaded");
        if (profiles.isEmpty()) throw new IllegalStateException("No phantom profiles loaded");
    }

    // ---- ВАЖНО: тут ты подключаешь свою реальную фабрику фантомов ----
    public Player spawnPhantomActor() {
        // Здесь должен быть реальный спавн Player.
        // Например: PhantomFactory.createAndSpawn(profile, spot)
        // Сейчас кидаю исключение, чтобы ты случайно не запустил “пустышку”.
        throw new UnsupportedOperationException("Implement spawnPhantomActor() to create/spawn Player");
    }

    public PhantomSpot pickSpotFor(Player p) {
        int lvl = p.getLevel();
        // выбираем подходящие по уровню
        List<PhantomSpot> ok = new ArrayList<>();
        for (PhantomSpot s : spots) {
            if (lvl >= s.minLvl && lvl <= s.maxLvl) ok.add(s);
        }
        if (ok.isEmpty()) {
            return spots.get(ThreadLocalRandom.current().nextInt(spots.size()));
        }
        return ok.get(ThreadLocalRandom.current().nextInt(ok.size()));
    }

    public PhantomProfile pickProfileForLevel(int level) {
        List<PhantomProfile> ok = new ArrayList<>();
        for (PhantomProfile pr : profiles) {
            if (level >= pr.minLvl && level <= pr.maxLvl) ok.add(pr);
        }
        if (ok.isEmpty()) return profiles.get(0);
        return ok.get(ThreadLocalRandom.current().nextInt(ok.size()));
    }
}