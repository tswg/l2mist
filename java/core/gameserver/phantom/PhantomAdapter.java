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
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Весь остальной phantom-код НЕ должен напрямую трогать Player/NpcInstance.
 * Только через этот адаптер.
 */
public final class PhantomAdapter {
	private static final Logger _log = LoggerFactory.getLogger(PhantomAdapter.class);
	private static final int LOS_TOP_N = 8;
	private static final long LOS_CACHE_TTL_MS = 1500L;
	private static final Map<Long, LosCacheEntry> LOS_CACHE = new ConcurrentHashMap<Long, LosCacheEntry>();

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
		List<NpcDistance> top = new ArrayList<NpcDistance>();
		for(NpcInstance npc : around)
		{
			if(npc == null || npc.isDead() || !npc.isVisible())
				continue;
			if(!(npc.isMonster() || npc.isAttackable(p)))
				continue;

			long d2 = distance2Dsq(p, npc);
			if(d2 > (long) radius * radius)
				continue;

			top.add(new NpcDistance(npc, d2));
		}

		Collections.sort(top, new Comparator<NpcDistance>() {
			@Override
			public int compare(NpcDistance o1, NpcDistance o2) {
				if (o1.d2 == o2.d2)
					return 0;
				return o1.d2 < o2.d2 ? -1 : 1;
			}
		});

		int checks = Math.min(LOS_TOP_N, top.size());
		for (int i = 0; i < checks; i++) {
			NpcInstance npc = top.get(i).npc;
			if (canSeeCached(p, npc))
				result.add(npc);
		}

		if (_log.isInfoEnabled())
			_log.info("[PHANTOM][getAroundMonsters] actor={} objectId={} radius={} worldAround={} candidate={} visible={}",
					p.getName(), p.getObjectId(), radius, around.size(), top.size(), result.size());

		return result;
    }

    private static long distance2Dsq(Player p, Creature t)
    {
		long dx = (long) p.getX() - t.getX();
		long dy = (long) p.getY() - t.getY();
		return dx * dx + dy * dy;
    }

    private static boolean canSeeCached(Player p, Creature t)
    {
		long now = System.currentTimeMillis();
		long key = (((long) p.getObjectId()) << 32) | (t.getObjectId() & 0xffffffffL);
		LosCacheEntry entry = LOS_CACHE.get(key);
		if(entry != null && now <= entry.expiresAt)
			return entry.canSee;

		boolean canSee = canSee(p, t);
		LOS_CACHE.put(key, new LosCacheEntry(canSee, now + LOS_CACHE_TTL_MS));

		if (LOS_CACHE.size() > 50000)
			LOS_CACHE.clear();

		return canSee;
    }

    private static final class NpcDistance
    {
		private final NpcInstance npc;
		private final long d2;

		private NpcDistance(NpcInstance npc, long d2)
		{
			this.npc = npc;
			this.d2 = d2;
		}
    }

    private static final class LosCacheEntry
    {
		private final boolean canSee;
		private final long expiresAt;

		private LosCacheEntry(boolean canSee, long expiresAt)
		{
			this.canSee = canSee;
			this.expiresAt = expiresAt;
		}
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
