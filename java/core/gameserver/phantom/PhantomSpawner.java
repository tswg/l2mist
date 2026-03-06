package core.gameserver.phantom;

import core.commons.dbutils.DbUtils;
import core.commons.util.Rnd;
import core.gameserver.database.DatabaseFactory;
import core.gameserver.ai.CtrlIntention;
import core.gameserver.data.xml.holder.ItemHolder;
import core.gameserver.model.Player;
import core.gameserver.model.SubClass;
import core.gameserver.model.base.ClassId;
import core.gameserver.model.base.ClassType2;
import core.gameserver.model.base.InvisibleType;
import core.gameserver.model.items.Inventory;
import core.gameserver.model.items.ItemInstance;
import core.gameserver.phantom.model.PhantomProfile;
import core.gameserver.phantom.model.PhantomSpot;
import core.gameserver.templates.item.ArmorTemplate;
import core.gameserver.templates.item.ItemTemplate;
import core.gameserver.templates.item.WeaponTemplate;
import core.gameserver.utils.ItemFunctions;
import core.gameserver.utils.Location;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
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

		if (phantom.getActiveClass() == null)
			restoreBaseClassContext(phantom);

		final int lvl = phantom.getLevel();
		if (_log.isDebugEnabled() && PhantomConfig.DEBUG)
			_log.debug("[PHANTOM][SPAWN] restored name={} objectId={} classId={} level={} exp={} hp={}/{}",
				phantom.getName(), phantom.getObjectId(), phantom.getActiveClassId(), lvl, phantom.getExp(), (int) phantom.getCurrentHp(), (int) phantom.getMaxHp());

		Location spawnLoc = randomPointAround(spot);

		phantom.setOfflineMode(false);
		phantom.setIsOnline(true);
		phantom.updateOnlineStatus();
		phantom.setOnlineStatus(true);
		phantom.setInvisibleType(InvisibleType.NONE);
		phantom.setNonAggroTime(Long.MAX_VALUE);
		resetSpawnState(phantom, "before-spawn");
		phantom.setPhantomLoc(spawnLoc.getX(), spawnLoc.getY(), spawnLoc.getZ());
		phantom.setXYZ(spawnLoc.getX(), spawnLoc.getY(), spawnLoc.getZ());
		phantom.spawnMe(spawnLoc);
		resetSpawnState(phantom, "after-spawn");

		if (_log.isDebugEnabled() && PhantomConfig.DEBUG)
			_log.debug("[PHANTOM][SPAWN] after spawn name={} objectId={} classId={} level={} exp={} hp={}/{} activeWeapon={} inventorySize={}",
				phantom.getName(), phantom.getObjectId(), phantom.getActiveClassId(), phantom.getLevel(), phantom.getExp(), (int) phantom.getCurrentHp(), (int) phantom.getMaxHp(),
				phantom.getActiveWeaponInstance() != null ? phantom.getActiveWeaponInstance().getItemId() : 0,
				phantom.getInventory() != null ? phantom.getInventory().getSize() : 0);

		equipByGrade(phantom);
		ensureConsumables(phantom);
		phantom.getInventory().validateItems();
		phantom.getInventory().refreshEquip();
		phantom.sendUserInfo(true);
		phantom.broadcastUserInfo(true);
		phantom.broadcastCharInfo();
		if (_log.isDebugEnabled() && PhantomConfig.DEBUG)
			logPaperdoll(phantom, "after-equip");
		if (_log.isDebugEnabled() && PhantomConfig.DEBUG)
			_log.debug("[PHANTOM][SPAWN] after equip name={} objectId={} classId={} level={} exp={} activeWeapon={} inventorySize={}",
				phantom.getName(), phantom.getObjectId(), phantom.getActiveClassId(), phantom.getLevel(), phantom.getExp(),
				phantom.getActiveWeaponInstance() != null ? phantom.getActiveWeaponInstance().getItemId() : 0,
				phantom.getInventory() != null ? phantom.getInventory().getSize() : 0);
		return phantom;
	}

	private void resetSpawnState(Player phantom, String stage) {
		if (phantom.getCurrentHp() <= 0 || phantom.isDead())
			phantom.doRevive();

		phantom.setCurrentHpMp(phantom.getMaxHp(), phantom.getMaxMp());
		phantom.setCurrentCp(phantom.getMaxCp());
		if (phantom.isFakeDeath())
			phantom.setFakeDeath(false);
		if (phantom.isSitting())
			phantom.setSitting(false);
		if (phantom.isSleeping())
			phantom.stopSleeping();
		if (phantom.isParalyzed())
			phantom.stopParalyzed();
		if (phantom.isImmobilized())
			phantom.stopImmobilized();
		if (phantom.isRooted())
			phantom.stopRooted();
		if (phantom.isStunned())
			phantom.stopStunning();
		phantom.abortAttack(true, false);
		phantom.abortCast(true, false);
		phantom.stopMove(false);
		if (phantom.isInStoreMode())
			phantom.setPrivateStoreType(Player.STORE_PRIVATE_NONE);
		if (phantom.isInObserverMode())
			phantom.leaveObserverMode();
		if (phantom.isOverloaded())
			phantom.setOverloaded(false);
		phantom.setTarget(null);
		phantom.setRunning();
		phantom.getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE);

		if (_log.isDebugEnabled() && PhantomConfig.DEBUG)
			_log.debug("[PHANTOM][SPAWN] state-reset stage={} name={} objectId={} dead={} hp={}/{} mp={}/{} cp={}/{} fakeDeath={} sitting={} paralyzed={} immobilized={} alikeDead={} store={} observer={} overloaded={} intention={} blockedFlags={}",
				stage,
				phantom.getName(),
				phantom.getObjectId(),
				phantom.isDead(),
				(int) phantom.getCurrentHp(), (int) phantom.getMaxHp(),
				(int) phantom.getCurrentMp(), (int) phantom.getMaxMp(),
				(int) phantom.getCurrentCp(), (int) phantom.getMaxCp(),
				phantom.isFakeDeath(), phantom.isSitting(), phantom.isParalyzed(), phantom.isImmobilized(), phantom.isAlikeDead(),
				phantom.isInStoreMode(), phantom.isInObserverMode(), phantom.isOverloaded(),
				phantom.getAI() != null ? phantom.getAI().getIntention() : null,
				PhantomAdapter.blockedStateSummary(phantom));
	}

	private void restoreBaseClassContext(Player phantom) {
		SubClass fromDb = loadBaseSubClass(phantom.getObjectId());
		if (fromDb != null) {
			phantom.getSubClasses().put(fromDb.getClassId(), fromDb);
			phantom.setActiveClass(fromDb);
			return;
		}

		SubClass base = new SubClass();
		base.setClassId(phantom.getBaseClassId());
		base.setBase(true);
		base.setActive(true);
		phantom.setActiveClass(base);
		phantom.getSubClasses().put(base.getClassId(), base);
	}

	private SubClass loadBaseSubClass(int objectId) {
		Connection con = null;
		PreparedStatement st = null;
		ResultSet rs = null;
		try {
			con = DatabaseFactory.getInstance().getConnection();
			st = con.prepareStatement("SELECT class_id, exp, sp, curHp, curMp, curCp FROM character_subclasses WHERE char_obj_id=? AND isBase=1 LIMIT 1");
			st.setInt(1, objectId);
			rs = st.executeQuery();
			if (!rs.next())
				return null;

			SubClass subClass = new SubClass();
			subClass.setBase(true);
			subClass.setClassId(rs.getInt("class_id"));
			subClass.setExp(rs.getLong("exp"));
			subClass.setSp(rs.getLong("sp"));
			subClass.setHp(rs.getDouble("curHp"));
			subClass.setMp(rs.getDouble("curMp"));
			subClass.setCp(rs.getDouble("curCp"));
			subClass.setActive(true);
			return subClass;
		} catch (Exception e) {
			_log.warn("[PHANTOM][SPAWN] failed to load base subclass data for objectId={}", objectId, e);
			return null;
		} finally {
			DbUtils.closeQuietly(con, st, rs);
		}
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

	private enum GearGrade {
		NG(ItemTemplate.Grade.NONE), D(ItemTemplate.Grade.D), C(ItemTemplate.Grade.C), B(ItemTemplate.Grade.B), A(ItemTemplate.Grade.A), S(ItemTemplate.Grade.S);
		private final ItemTemplate.Grade crystal;
		GearGrade(ItemTemplate.Grade crystal) { this.crystal = crystal; }
	}

	private enum Archetype { FIGHTER, MAGE, ARCHER, ROGUE, SUPPORT }

	private GearGrade gradeByLevel(int level) {
		if (level >= 76)
			return GearGrade.S;
		if (level >= 61)
			return GearGrade.A;
		if (level >= 52)
			return GearGrade.B;
		if (level >= 40)
			return GearGrade.C;
		if (level >= 20)
			return GearGrade.D;
		return GearGrade.NG;
	}

	private Archetype detectArchetype(Player phantom) {
		ClassId classId = resolveClassId(phantom);
		if (classId == null)
			return Archetype.FIGHTER;

		ClassType2 type2 = classId.getType2();
		if (type2 == ClassType2.Healer || type2 == ClassType2.Enchanter)
			return Archetype.SUPPORT;
		if (type2 == ClassType2.Wizard || type2 == ClassType2.Summoner || classId.isMage())
			return Archetype.MAGE;
		if (isArcherClass(classId))
			return Archetype.ARCHER;
		if (type2 == ClassType2.Rogue || isDaggerClass(classId))
			return Archetype.ROGUE;
		return Archetype.FIGHTER;
	}

	private boolean isArcherClass(ClassId classId) {
		switch (classId) {
			case hawkeye:
			case silverRanger:
			case phantomRanger:
			case sagittarius:
			case moonlightSentinel:
			case ghostSentinel:
			case arbalester:
			case trickster:
				return true;
			default:
				return false;
		}
	}

	private boolean isDaggerClass(ClassId classId) {
		switch (classId) {
			case treasureHunter:
			case plainsWalker:
			case abyssWalker:
			case adventurer:
			case windRider:
			case ghostHunter:
				return true;
			default:
				return false;
		}
	}

	private ClassId resolveClassId(Player phantom) {
		int classIdx = phantom != null ? phantom.getActiveClassId() : -1;
		if (classIdx < 0 || classIdx >= ClassId.VALUES.length)
			return null;
		return ClassId.VALUES[classIdx];
	}

	private void equipByGrade(Player phantom) {
		if (phantom.getInventory() == null)
			return;

		final GearGrade maxGrade = gradeByLevel(phantom.getLevel());
		final Archetype archetype = detectArchetype(phantom);

		unequipCombatSlots(phantom);

		ItemInstance invWeapon = pickWeaponFromInventory(phantom, maxGrade, archetype);
		if (invWeapon != null)
			equipExistingItem(phantom, invWeapon);
		else {
			ItemTemplate weapon = selectWeaponFor(maxGrade, archetype);
			if (weapon != null)
				equipTemplateItem(phantom, weapon);
		}

		equipArmorSet(phantom, maxGrade, archetype);

		if (_log.isDebugEnabled() && PhantomConfig.DEBUG)
			_log.debug("[PHANTOM][SPAWN] grade-equip name={} objectId={} classId={} level={} grade={} archetype={} weapon={} chest={} legs={} gloves={} feet={}",
				phantom.getName(), phantom.getObjectId(), phantom.getActiveClassId(), phantom.getLevel(), maxGrade, archetype,
				paperdollItemId(phantom, Inventory.PAPERDOLL_RHAND),
				paperdollItemId(phantom, Inventory.PAPERDOLL_CHEST),
				paperdollItemId(phantom, Inventory.PAPERDOLL_LEGS),
				paperdollItemId(phantom, Inventory.PAPERDOLL_GLOVES),
				paperdollItemId(phantom, Inventory.PAPERDOLL_FEET));
	}

	private void unequipCombatSlots(Player phantom) {
		for (int slot : new int[]{Inventory.PAPERDOLL_RHAND, Inventory.PAPERDOLL_LHAND, Inventory.PAPERDOLL_CHEST, Inventory.PAPERDOLL_LEGS, Inventory.PAPERDOLL_GLOVES, Inventory.PAPERDOLL_FEET, Inventory.PAPERDOLL_HEAD}) {
			ItemInstance it = phantom.getInventory().getPaperdollItem(slot);
			if (it != null)
				phantom.getInventory().unEquipItem(it);
		}
	}

	private ItemTemplate selectWeaponFor(GearGrade maxGrade, Archetype archetype) {
		List<ItemTemplate> candidates = collectWeaponTemplates(maxGrade, archetype);
		return pickRandomTopByPrice(candidates, 6);
	}

	private ItemInstance pickWeaponFromInventory(Player phantom, GearGrade maxGrade, Archetype archetype) {
		ItemInstance[] items = phantom.getInventory().getItems();
		List<ItemInstance> candidates = new ArrayList<ItemInstance>();
		for (ItemInstance item : items) {
			if (item == null || item.getTemplate() == null || !item.getTemplate().isWeapon())
				continue;
			ItemTemplate tpl = item.getTemplate();
			if (tpl.getBodyPart() != ItemTemplate.SLOT_R_HAND && tpl.getBodyPart() != ItemTemplate.SLOT_LR_HAND)
				continue;
			if (!isAllowedGrade(tpl.getCrystalType(), maxGrade))
				continue;
			WeaponTemplate.WeaponType type = ((WeaponTemplate) tpl).getItemType();
			if (!weaponFits(type, archetype))
				continue;
			candidates.add(item);
		}
		return pickRandomInventoryByPrice(candidates, 4);
	}

	private List<ItemTemplate> collectWeaponTemplates(GearGrade maxGrade, Archetype archetype) {
		List<ItemTemplate> candidates = new ArrayList<ItemTemplate>();
		for (ItemTemplate item : ItemHolder.getInstance().getAllTemplates()) {
			if (item == null || !item.isWeapon())
				continue;
			if (item.getBodyPart() != ItemTemplate.SLOT_R_HAND && item.getBodyPart() != ItemTemplate.SLOT_LR_HAND)
				continue;
			if (!isAllowedGrade(item.getCrystalType(), maxGrade))
				continue;
			WeaponTemplate.WeaponType type = ((WeaponTemplate) item).getItemType();
			if (!weaponFits(type, archetype))
				continue;
			candidates.add(item);
		}
		return candidates;
	}

	private void equipArmorSet(Player phantom, GearGrade maxGrade, Archetype archetype) {
		ArmorTemplate.ArmorType desired = desiredArmorType(archetype);
		Map<Integer, ItemInstance> invArmor = pickArmorFromInventory(phantom, maxGrade, desired);
		for (int bodyPart : new int[]{ItemTemplate.SLOT_CHEST, ItemTemplate.SLOT_LEGS, ItemTemplate.SLOT_GLOVES, ItemTemplate.SLOT_FEET}) {
			ItemInstance inv = invArmor.get(bodyPart);
			if (inv != null)
				equipExistingItem(phantom, inv);
			else {
				ItemTemplate best = pickBestArmor(bodyPart, desired, maxGrade);
				if (best != null)
					equipTemplateItem(phantom, best);
			}
		}
	}

	private Map<Integer, ItemInstance> pickArmorFromInventory(Player phantom, GearGrade maxGrade, ArmorTemplate.ArmorType desired) {
		Map<Integer, List<ItemInstance>> byPart = new HashMap<Integer, List<ItemInstance>>();
		for (ItemInstance item : phantom.getInventory().getItems()) {
			if (item == null || item.getTemplate() == null || !item.getTemplate().isArmor())
				continue;
			ItemTemplate tpl = item.getTemplate();
			if (!isAllowedGrade(tpl.getCrystalType(), maxGrade))
				continue;
			if (tpl.getBodyPart() != ItemTemplate.SLOT_CHEST && tpl.getBodyPart() != ItemTemplate.SLOT_LEGS && tpl.getBodyPart() != ItemTemplate.SLOT_GLOVES && tpl.getBodyPart() != ItemTemplate.SLOT_FEET)
				continue;
			ArmorTemplate.ArmorType type = ((ArmorTemplate) tpl).getItemType();
			if (type != desired)
				continue;
			List<ItemInstance> list = byPart.get(tpl.getBodyPart());
			if (list == null) {
				list = new ArrayList<ItemInstance>();
				byPart.put(tpl.getBodyPart(), list);
			}
			list.add(item);
		}

		Map<Integer, ItemInstance> chosen = new HashMap<Integer, ItemInstance>();
		for (Map.Entry<Integer, List<ItemInstance>> e : byPart.entrySet())
			chosen.put(e.getKey(), pickRandomInventoryByPrice(e.getValue(), 3));
		return chosen;
	}

	private ArmorTemplate.ArmorType desiredArmorType(Archetype archetype) {
		if (archetype == Archetype.MAGE || archetype == Archetype.SUPPORT)
			return ArmorTemplate.ArmorType.MAGIC;
		if (archetype == Archetype.ARCHER || archetype == Archetype.ROGUE)
			return ArmorTemplate.ArmorType.LIGHT;
		return ArmorTemplate.ArmorType.HEAVY;
	}

	private ItemTemplate pickBestArmor(int bodyPart, ArmorTemplate.ArmorType desired, GearGrade maxGrade) {
		List<ItemTemplate> candidates = new ArrayList<ItemTemplate>();
		for (ItemTemplate item : ItemHolder.getInstance().getAllTemplates()) {
			if (item == null || !item.isArmor())
				continue;
			if (!isAllowedGrade(item.getCrystalType(), maxGrade))
				continue;
			if (item.getBodyPart() != bodyPart)
				continue;
			ArmorTemplate.ArmorType type = ((ArmorTemplate) item).getItemType();
			if (type != desired)
				continue;
			candidates.add(item);
		}
		return pickMostExpensive(candidates);
	}

	private boolean isAllowedGrade(ItemTemplate.Grade itemGrade, GearGrade maxGrade) {
		ItemTemplate.Grade normalized = normalizeGrade(itemGrade);
		return normalized.externalOrdinal <= maxGrade.crystal.externalOrdinal;
	}

	private ItemTemplate.Grade normalizeGrade(ItemTemplate.Grade grade) {
		if (grade == ItemTemplate.Grade.S80 || grade == ItemTemplate.Grade.S84)
			return ItemTemplate.Grade.S;
		return grade;
	}

	private boolean weaponFits(WeaponTemplate.WeaponType type, Archetype archetype) {
		if (archetype == Archetype.MAGE || archetype == Archetype.SUPPORT)
			return type == WeaponTemplate.WeaponType.BLUNT || type == WeaponTemplate.WeaponType.BIGBLUNT || type == WeaponTemplate.WeaponType.SWORD || type == WeaponTemplate.WeaponType.DAGGER;
		if (archetype == Archetype.ARCHER)
			return type == WeaponTemplate.WeaponType.BOW || type == WeaponTemplate.WeaponType.CROSSBOW;
		if (archetype == Archetype.ROGUE)
			return type == WeaponTemplate.WeaponType.DAGGER || type == WeaponTemplate.WeaponType.DUALDAGGER || type == WeaponTemplate.WeaponType.SWORD;
		return type == WeaponTemplate.WeaponType.SWORD || type == WeaponTemplate.WeaponType.BLUNT || type == WeaponTemplate.WeaponType.BIGSWORD || type == WeaponTemplate.WeaponType.BIGBLUNT || type == WeaponTemplate.WeaponType.DUAL;
	}

	private ItemTemplate pickMostExpensive(List<ItemTemplate> items) {
		return pickRandomTopByPrice(items, 1);
	}

	private ItemTemplate pickRandomTopByPrice(List<ItemTemplate> items, int topN) {
		if (items == null || items.isEmpty())
			return null;
		Collections.sort(items, new Comparator<ItemTemplate>() {
			@Override
			public int compare(ItemTemplate o1, ItemTemplate o2) {
				int p2 = o2.getReferencePrice();
				int p1 = o1.getReferencePrice();
				if (p2 > p1)
					return 1;
				if (p2 < p1)
					return -1;
				return 0;
			}
		});
		int n = Math.max(1, Math.min(topN, items.size()));
		return items.get(Rnd.get(n));
	}


	private ItemInstance pickRandomInventoryByPrice(List<ItemInstance> items, int topN) {
		if (items == null || items.isEmpty())
			return null;
		Collections.sort(items, new Comparator<ItemInstance>() {
			@Override
			public int compare(ItemInstance o1, ItemInstance o2) {
				int p2 = o2.getTemplate() != null ? o2.getTemplate().getReferencePrice() : 0;
				int p1 = o1.getTemplate() != null ? o1.getTemplate().getReferencePrice() : 0;
				if (p2 > p1)
					return 1;
				if (p2 < p1)
					return -1;
				return 0;
			}
		});
		int n = Math.max(1, Math.min(topN, items.size()));
		return items.get(Rnd.get(n));
	}

	private void equipExistingItem(Player phantom, ItemInstance item) {
		if (phantom == null || item == null || phantom.getInventory() == null)
			return;
		try {
			phantom.getInventory().equipItem(item);
		} catch (Exception e) {
			_log.warn("[PHANTOM][SPAWN_ERROR] equip existing failed name={} objectId={} itemObjId={} itemId={}", phantom.getName(), phantom.getObjectId(), item.getObjectId(), item.getItemId(), e);
		}
	}

	private void equipTemplateItem(Player phantom, ItemTemplate template) {
		ItemInstance item = ItemFunctions.createItem(template.getItemId());
		if (item == null)
			return;
		phantom.getInventory().addItem(item);
		try {
			phantom.getInventory().equipItem(item);
		} catch (Exception e) {
			_log.warn("[PHANTOM][SPAWN_ERROR] equip failed name={} objectId={} itemId={}", phantom.getName(), phantom.getObjectId(), item.getItemId(), e);
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
