package core.gameserver.phantom;

import java.io.FileInputStream;
import java.io.File;
import java.util.Properties;

public final class PhantomConfig {
    private PhantomConfig() {}

    public static boolean USE_POTIONS;
    public static int HP_POTION_ITEM_ID;
    public static int MP_POTION_ITEM_ID;

    public static boolean USE_SHOTS;
    public static int SOULSHOT_ITEM_ID;
    public static int SPIRITSHOT_ITEM_ID;

    public static int POTION_COOLDOWN_MS;
    public static int SHOTS_COOLDOWN_MS;

    public static double MIN_HP_POTION_RATIO;
    public static double MIN_MP_POTION_RATIO;

    public static boolean ENABLED;
    public static int TOTAL_ONLINE;
    public static int ACTIVE_CAP;

    public static int TICK_ACTIVE;
    public static int TICK_IDLE;
    public static int TICK_SLEEP;

    public static int SEARCH_RADIUS;
    public static int PURSUIT_RANGE;
    public static int LEASH_TO_SPOT;

    public static double HEAL_AT_HP;
    public static double RETREAT_AT_HP;

    public static int STUCK_CHECK_SEC;
    public static boolean STUCK_TELEPORT;
    public static String ACCOUNT_NAME;

    public static void load(String path) {
        File configFile = new File(path);
        String absolutePath = configFile.getAbsolutePath();
        boolean exists = configFile.exists() && configFile.isFile();
        System.out.println("[PHANTOM] PhantomConfig.load path=" + absolutePath + " exists=" + exists);

        if (!exists) {
            ENABLED = false;
            System.err.println("[PHANTOM][ERROR] phantom config not found: " + absolutePath + ". PhantomEnabled=false");
            return;
        }

        Properties p = new Properties();
        try (FileInputStream fis = new FileInputStream(configFile)) {
            p.load(fis);
        } catch (Exception e) {
            ENABLED = false;
            System.err.println("[PHANTOM][ERROR] Cannot load phantom config: " + absolutePath);
            e.printStackTrace();
            return;
        }

        ENABLED = getBool(p, "PhantomEnabled", true);
        TOTAL_ONLINE = getInt(p, "PhantomTotalOnline", 200);
        ACTIVE_CAP = getInt(p, "PhantomActiveCap", 60);

        TICK_ACTIVE = getInt(p, "PhantomTickActive", 400);
        TICK_IDLE   = getInt(p, "PhantomTickIdle", 1200);
        TICK_SLEEP  = getInt(p, "PhantomTickSleep", 4500);

        SEARCH_RADIUS  = getInt(p, "PhantomSearchRadius", 1500);
        PURSUIT_RANGE  = getInt(p, "PhantomPursuitRange", 900);
        LEASH_TO_SPOT  = getInt(p, "PhantomLeashToSpot", 1200);

        HEAL_AT_HP     = getDouble(p, "PhantomHealAtHp", 0.55);
        RETREAT_AT_HP  = getDouble(p, "PhantomRetreatAtHp", 0.25);

        STUCK_CHECK_SEC = getInt(p, "PhantomStuckCheckSec", 15);
        STUCK_TELEPORT  = getBool(p, "PhantomStuckTeleport", true);
        USE_POTIONS = getBool(p, "PhantomUsePotions", true);
        HP_POTION_ITEM_ID = getInt(p, "PhantomHpPotionItemId", 1539);
        MP_POTION_ITEM_ID = getInt(p, "PhantomMpPotionItemId", 728);

        USE_SHOTS = getBool(p, "PhantomUseShots", true);
        SOULSHOT_ITEM_ID = getInt(p, "PhantomSoulshotItemId", 1835);
        SPIRITSHOT_ITEM_ID = getInt(p, "PhantomSpiritshotItemId", 3947);

        POTION_COOLDOWN_MS = getInt(p, "PhantomPotionCooldownMs", 7000);
        SHOTS_COOLDOWN_MS = getInt(p, "PhantomShotsCooldownMs", 1500);

        MIN_HP_POTION_RATIO = getDouble(p, "PhantomMinHpPotionRatio", 0.70);
        MIN_MP_POTION_RATIO = getDouble(p, "PhantomMinMpPotionRatio", 0.35);
        ACCOUNT_NAME = getStr(p, "PhantomAccountName", "phantom");

        System.out.println("[PHANTOM] PhantomConfig loaded. PhantomEnabled=" + ENABLED);
    }

    private static String getStr(Properties p, String k, String def) {
        String v = p.getProperty(k);
        return (v == null) ? def : v.trim();
    }

    private static int getInt(Properties p, String k, int def) {
        String v = p.getProperty(k);
        return (v == null) ? def : Integer.parseInt(v.trim());
    }

    private static double getDouble(Properties p, String k, double def) {
        String v = p.getProperty(k);
        return (v == null) ? def : Double.parseDouble(v.trim());
    }

    private static boolean getBool(Properties p, String k, boolean def) {
        String v = p.getProperty(k);
        return (v == null) ? def : Boolean.parseBoolean(v.trim());
    }
}
