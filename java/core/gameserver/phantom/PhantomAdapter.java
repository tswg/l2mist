package core.gameserver.phantom;

import core.gameserver.geodata.GeoEngine;
import core.gameserver.handler.items.IItemHandler;
import core.gameserver.model.Creature;
import core.gameserver.model.Player;
import core.gameserver.model.World;
import core.gameserver.model.instances.NpcInstance;
import core.gameserver.model.items.ItemInstance;
import core.gameserver.utils.Location;
import core.gameserver.ai.CtrlIntention;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Весь остальной phantom-код НЕ должен напрямую трогать Player/NpcInstance.
 * Только через этот адаптер.
 */
public final class PhantomAdapter {
	private static final Logger _log = LoggerFactory.getLogger(PhantomAdapter.class);

    private PhantomAdapter() {}

    // --------- базовые метрики ---------

    public static double hpRatio(Player p) {
        if(p == null || p.getMaxHp() <= 0)
            return 0D;
        return p.getCurrentHp() / p.getMaxHp();
    }

    public static int level(Player p)
    {
    	return p == null ? 1 : p.getLevel();
    }

    public static boolean isDead(Creature c) {
        return c == null || c.isDead();
    }

    public static boolean isVisible(Creature c)
    {
    	return c != null && c.isVisible();
    }

    public static boolean isInCombat(Player p)
    {
    	return p != null && p.isInCombat();
    }

    public static double dist2D(Player p, int x, int y) {
        long dx = (long)p.getX() - x;
        long dy = (long)p.getY() - y;
        return Math.sqrt(dx*dx + dy*dy);
    }

    public static double dist3D(Player p, Creature t) {
    	if(p == null || t == null)
    		return Double.MAX_VALUE;
        return p.getDistance3D(t);
    }

	public static int x(Player p)
	{
		return p == null ? 0 : p.getX();
	}

	public static int y(Player p)
	{
		return p == null ? 0 : p.getY();
	}

	public static int z(Player p)
	{
		return p == null ? 0 : p.getZ();
	}

	public static Location location(Creature c)
	{
		return c == null ? null : c.getLoc();
	}

    public static Location loc(int x, int y, int z) {
        return new Location(x, y, z);
    }

    // --------- команды AI ---------

    public static void moveTo(Player p, Location loc) {
    	if(p == null || loc == null)
    		return;
        p.moveToLocation(loc, 0, true);
    }

    public static void attack(Player p, Creature target) {
    	if(p == null || target == null)
    		return;
        p.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, target);
    }

    public static void idle(Player p) {
		if(p == null)
			return;
        p.getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
    }

    public static List<NpcInstance> getAroundMonsters(Player p, int radius) {
		List<NpcInstance> result = new ArrayList<NpcInstance>();
		if(p == null || !p.isVisible())
			return result;

		List<NpcInstance> around = World.getAroundNpc(p, radius, 300);
		for(NpcInstance npc : around)
		{
			if(npc == null || npc.isDead() || !npc.isVisible())
				continue;
			if(!(npc.isMonster() || npc.isAttackable(p)))
				continue;
			if(!canSee(p, npc))
				continue;
			result.add(npc);
		}
		return result;
    }

    public static boolean useItem(Player p, int itemId) {
        if (itemId <= 0) return false;
        if (p == null || p.getInventory() == null) return false;

        ItemInstance item = p.getInventory().getItemByItemId(itemId);
        if (item == null || item.getCount() <= 0) return false;

        try {
        	IItemHandler handler = item.getTemplate().getHandler();
        	if(handler == null)
        	{
        		_log.debug("PhantomAdapter: item handler not found for itemId={} player={}", itemId, p.getName());
        		return false;
        	}

        	return handler.useItem(p, item, false);

        } catch (Throwable t) {
			_log.warn("PhantomAdapter: failed useItem itemId={} player={}", itemId, p.getName(), t);
            return false;
        }
    }

    public static boolean canSee(Player p, Creature t) {
		if(p == null || t == null)
			return false;
        return GeoEngine.canSeeTarget(p, t, false);
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
