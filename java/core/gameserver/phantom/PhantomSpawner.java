package core.gameserver.phantom;

import core.commons.dbutils.DbUtils;
import core.commons.util.Rnd;
import core.gameserver.database.DatabaseFactory;
import core.gameserver.ai.CtrlIntention;
import core.gameserver.model.Player;
import core.gameserver.model.SubClass;
import core.gameserver.model.base.InvisibleType;
import core.gameserver.model.items.Inventory;
import core.gameserver.model.items.ItemInstance;
import core.gameserver.phantom.model.PhantomProfile;
import core.gameserver.phantom.model.PhantomSpot;
import core.gameserver.templates.item.CreateItem;
import core.gameserver.templates.item.ItemTemplate;
import core.gameserver.utils.ItemFunctions;
import core.gameserver.utils.Location;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public final class PhantomSpawner {
	private static final Logger _log = LoggerFactory.getLogger(PhantomSpawner.class);

	private final List<Integer> candidateIds = new ArrayList<Integer>();
	private final AtomicInteger index = new AtomicInteger();

	public void prepare() {
		candidateIds.clear();

		Connection con = null;
		PreparedStatement st = null;
		ResultSet rs = null;
		try {
			con = DatabaseFactory.getInstance().getConnection();
			st = con.prepareStatement("SELECT obj_Id FROM characters WHERE account_name = ? ORDER BY obj_Id");
			st.setString(1, PhantomConfig.ACCOUNT_NAME);
			rs = st.executeQuery();
			while (rs.next())
				candidateIds.add(rs.getInt("obj_Id"));
		} catch (Exception e) {
			throw new IllegalStateException("Cannot load phantom candidates from DB", e);
		} finally {
			DbUtils.closeQuietly(con, st, rs);
		}

		if (candidateIds.isEmpty())
			throw new IllegalStateException("No characters found for phantom account " + PhantomConfig.ACCOUNT_NAME);

		Collections.shuffle(candidateIds);
		index.set(0);
		_log.info("PhantomSpawner prepared: candidates={} account={}", candidateIds.size(), PhantomConfig.ACCOUNT_NAME);
	}

	public Player spawn(PhantomSpot spot, PhantomProfile profile) {
		int objectId = nextObjectId();
		Player phantom = Player.restorePhantom(objectId, 0, 0, false);
		if (phantom == null)
			throw new IllegalStateException("Player.restorePhantom returned null for objectId=" + objectId);

		// restorePhantom may return player without active class context; this breaks
		// equipment validation paths via Player#getActiveClassId().
		if (phantom.getActiveClass() == null) {
			SubClass base = new SubClass();
			base.setClassId(phantom.getBaseClassId());
			base.setBase(true);
			base.setActive(true);
			phantom.setActiveClass(base);
			phantom.getSubClasses().put(base.getClassId(), base);
		}

		Location spawnLoc = randomPointAround(spot);

		phantom.setOfflineMode(false);
		phantom.setIsOnline(true);
		phantom.updateOnlineStatus();
		phantom.setOnlineStatus(true);
		phantom.setInvisibleType(InvisibleType.NONE);
		phantom.setNonAggroTime(Long.MAX_VALUE);
		phantom.setCurrentHpMp(phantom.getMaxHp(), phantom.getMaxMp());
		phantom.setCurrentCp(phantom.getMaxCp());
		phantom.setPhantomLoc(spawnLoc.getX(), spawnLoc.getY(), spawnLoc.getZ());
		phantom.setXYZ(spawnLoc.getX(), spawnLoc.getY(), spawnLoc.getZ());
		phantom.spawnMe(spawnLoc);
		phantom.setRunning();
		phantom.getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
		if (_log.isDebugEnabled() && PhantomConfig.DEBUG)
			_log.debug("[PHANTOM][SPAWN] after spawn name={} objectId={} activeWeapon={} inventorySize={}",
				phantom.getName(), phantom.getObjectId(),
				phantom.getActiveWeaponInstance() != null ? phantom.getActiveWeaponInstance().getItemId() : 0,
				phantom.getInventory() != null ? phantom.getInventory().getSize() : 0);

		ensureBasicEquipment(phantom);
		ensureConsumables(phantom);
		phantom.getInventory().validateItems();
		phantom.getInventory().refreshEquip();
		phantom.sendUserInfo(true);
		phantom.broadcastUserInfo(true);
		phantom.broadcastCharInfo();
		if (_log.isDebugEnabled() && PhantomConfig.DEBUG)
			logPaperdoll(phantom, "after-equip");
		if (_log.isDebugEnabled() && PhantomConfig.DEBUG)
			_log.debug("[PHANTOM][SPAWN] after equip name={} objectId={} activeWeapon={} inventorySize={}",
				phantom.getName(), phantom.getObjectId(),
				phantom.getActiveWeaponInstance() != null ? phantom.getActiveWeaponInstance().getItemId() : 0,
				phantom.getInventory() != null ? phantom.getInventory().getSize() : 0);
		return phantom;
	}

	private int nextObjectId() {
		int size = candidateIds.size();
		int i = index.getAndIncrement();
		if (i >= size)
			throw new IllegalStateException("Phantom candidate pool exhausted: requested=" + (i + 1) + " available=" + size);

		int pos = i;
		return candidateIds.get(pos);
	}

	public int candidatesCount() {
		return candidateIds.size();
	}

	private Location randomPointAround(PhantomSpot spot) {
		int radius = Math.max(50, spot.radius);
		int x = spot.centerX + Rnd.get(-radius, radius);
		int y = spot.centerY + Rnd.get(-radius, radius);
		return new Location(x, y, spot.centerZ);
	}

	private void ensureBasicEquipment(Player phantom) {
		if (phantom.getInventory() == null)
			return;

		for (CreateItem createItem : phantom.getTemplate().getItems()) {
			ItemInstance item = ItemFunctions.createItem(createItem.getItemId());
			if (item == null)
				continue;

			phantom.getInventory().addItem(item);
			if (createItem.isEquipable() && item.isEquipable()) {
				if (item.getTemplate().getType2() == ItemTemplate.TYPE2_WEAPON && phantom.getActiveWeaponInstance() != null)
					continue;

				try {
					phantom.getInventory().equipItem(item);
				} catch (Exception e) {
					_log.warn("[PHANTOM][SPAWN_ERROR] equip failed name={} objectId={} itemId={}", phantom.getName(), phantom.getObjectId(), item.getItemId(), e);
				}
			}
		}
	}


	private void logPaperdoll(Player phantom, String stage)
	{
		if (phantom == null || phantom.getInventory() == null)
			return;

		_log.debug("[PHANTOM][SPAWN] paperdoll stage={} name={} objectId={} weapon={} chest={} legs={} gloves={} feet={}",
				stage,
				phantom.getName(),
				phantom.getObjectId(),
				paperdollItemId(phantom, Inventory.PAPERDOLL_RHAND),
				paperdollItemId(phantom, Inventory.PAPERDOLL_CHEST),
				paperdollItemId(phantom, Inventory.PAPERDOLL_LEGS),
				paperdollItemId(phantom, Inventory.PAPERDOLL_GLOVES),
				paperdollItemId(phantom, Inventory.PAPERDOLL_FEET));
	}

	private int paperdollItemId(Player phantom, int slot)
	{
		return phantom.getInventory().getPaperdollItemId(slot);
	}

	private void ensureConsumables(Player phantom) {
		if (PhantomConfig.USE_SHOTS && PhantomConfig.SOULSHOT_ITEM_ID > 0)
			ItemFunctions.addItem(phantom, PhantomConfig.SOULSHOT_ITEM_ID, 5000, false);
		if (PhantomConfig.USE_POTIONS && PhantomConfig.HP_POTION_ITEM_ID > 0)
			ItemFunctions.addItem(phantom, PhantomConfig.HP_POTION_ITEM_ID, 200, false);
	}
}
