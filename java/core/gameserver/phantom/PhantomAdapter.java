package core.gameserver.phantom;

import core.gameserver.model.Player;              // <-- проверь пакет
import core.gameserver.model.Creature;            // <-- проверь пакет
import core.gameserver.model.World;
import core.gameserver.model.instances.NpcInstance; // <-- проверь пакет
import core.gameserver.model.base.TeamType;       // если есть, не обязательно
import core.gameserver.model.items.ItemInstance;
import core.gameserver.utils.Location;            // <-- проверь пакет
import core.gameserver.ai.CtrlIntention;          // <-- проверь пакет

import java.util.ArrayList;
import java.util.List;

/**
 * Весь остальной phantom-код НЕ должен напрямую трогать Player/NpcInstance.
 * Только через этот адаптер.
 */
public final class PhantomAdapter {

    private PhantomAdapter() {}

    // --------- базовые метрики ---------

    public static double hpRatio(Player p) {
        return p.getCurrentHp() / p.getMaxHp();
    }

    public static boolean isDead(Creature c) {
        return c == null || c.isDead();
    }

    public static double dist2D(Player p, int x, int y) {
        long dx = (long)p.getX() - x;
        long dy = (long)p.getY() - y;
        return Math.sqrt(dx*dx + dy*dy);
    }

    public static double dist3D(Player p, Creature t) {
        return p.distance3D(t);
    }

    public static Location loc(int x, int y, int z) {
        return new Location(x, y, z);
    }

    // --------- команды AI ---------

    public static void moveTo(Player p, Location loc) {
        p.getAI().setIntention(CtrlIntention.AI_INTENTION_MOVE_TO, loc);
    }

    public static void attack(Player p, Creature target) {
        p.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, target);
    }

    public static void idle(Player p) {
        p.getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
    }

    public static List<NpcInstance> getAroundMonsters(Player p, int radius) {
        return p.getKnownList()
                .getKnownTypeInRadius(NpcInstance.class, radius)
                .stream()
                .filter(NpcInstance::isMonster)
                .toList();
    }

    public static boolean useItem(Player p, int itemId) {
        if (itemId <= 0) return false;

        ItemInstance item = p.getInventory().getItemByItemId(itemId);
        if (item == null || item.getCount() <= 0) return false;

        try {
            // Вариант A: если у Player есть метод useItem / useItem(item, ctrl, shift)
            // p.useItem(item, false, false);
            // return true;

            // Вариант B: типовой L2J: p.useItem(item, false);
            // return p.useItem(item, false);

            // Вариант C: через ItemHandler (если есть)
            // IItemHandler handler = ItemHandler.getInstance().getHandler(item.getItemId());
            // if (handler == null) return false;
            // handler.useItem(p, item, false);
            // return true;

            // Заглушка, чтобы ты выбрал свой вариант:
            throw new UnsupportedOperationException("Implement useItem() for your сборку");

        } catch (Throwable t) {
            return false;
        }
    }

    public static boolean canSee(Player p, Creature t) {
        // если есть геодата/видимость:
        // return GeoEngine.canSeeTarget(p, t, false);
        return true;
    }

    public static boolean isValidFarmTarget(Player p, NpcInstance n) {
        if (n == null || n.isDead()) return false;
        if (!n.isMonster()) return false;
        if (!n.isVisible()) return false;
        if (!canSee(p, n)) return false;

        // если есть мирные/ивентовые/неагр:
        // if (n.isRaid()) return false;
        // if (n.isInvul()) return false;

        return true;
    }
}