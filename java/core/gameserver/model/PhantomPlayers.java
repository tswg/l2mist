package core.gameserver.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.log4j.Logger;

import core.commons.util.Rnd;
import core.gameserver.Config;
import core.gameserver.ThreadPoolManager;
import core.gameserver.ai.CtrlIntention;
import core.gameserver.ai.PlayableAI.nextAction;
import core.gameserver.model.instances.NpcInstance;
import core.gameserver.model.items.ItemInstance;
import core.gameserver.model.phantom.PhantomBehaviorService;
import core.gameserver.model.phantom.PhantomCombatService;
import core.gameserver.model.phantom.PhantomConfig;
import core.gameserver.model.phantom.PhantomDataService;
import core.gameserver.model.phantom.PhantomDebugService;
import core.gameserver.model.phantom.PhantomEquipService;
import core.gameserver.model.phantom.PhantomMovementService;
import core.gameserver.model.phantom.PhantomRegistry;
import core.gameserver.model.phantom.PhantomRespawnService;
import core.gameserver.model.phantom.PhantomRoleResolver;
import core.gameserver.model.phantom.PhantomSet;
import core.gameserver.model.phantom.PhantomSpawnerService;
import core.gameserver.model.phantom.PhantomSpotService;
import core.gameserver.model.phantom.PhantomTargetService;
import core.gameserver.network.l2.s2c.MagicSkillUse;

public class PhantomPlayers
{
	private static final Logger _log = Logger.getLogger(PhantomPlayers.class.getName());
	private static PhantomPlayers _instance;

	private final PhantomConfig config = new PhantomConfig();
	private final PhantomRegistry registry = new PhantomRegistry();
	private final PhantomSpotService spotService = new PhantomSpotService();
	private final PhantomDataService dataService = new PhantomDataService();
	private final List<PhantomSet> sets = new ArrayList<PhantomSet>();
	private PhantomEquipService equipService = new PhantomEquipService(sets);
	private final PhantomSpawnerService spawnerService = new PhantomSpawnerService(registry, spotService, equipService);
	private final PhantomRoleResolver roleResolver = new PhantomRoleResolver();
	private final PhantomTargetService targetService = new PhantomTargetService();
	private final PhantomMovementService movementService = new PhantomMovementService();
	private final PhantomCombatService combatService = new PhantomCombatService(roleResolver, movementService);
	private final PhantomRespawnService respawnService = new PhantomRespawnService();
	private PhantomBehaviorService behaviorService;
	private final PhantomDebugService debugService = new PhantomDebugService(_log);
	private List<String> enchantPhrases = new ArrayList<String>();
	private List<String> lastPhrases = new ArrayList<String>();
	private int phantomsLimit;

	private final static int[][] _mageBuff = new int[][]{{6, 75, 4322, 1}, {6, 75, 4323, 1}, {6, 75, 5637, 1}, {6, 75, 4328, 1}, {6, 75, 4329, 1}, {6, 75, 4330, 1}, {6, 75, 4331, 1}, {16, 34, 4338, 1}};
	private final static int[][] _warrBuff = new int[][]{{6, 75, 4322, 1}, {6, 75, 4323, 1}, {6, 75, 5637, 1}, {6, 75, 4324, 1}, {6, 75, 4325, 1}, {6, 75, 4326, 1}, {6, 39, 4327, 1}, {40, 75, 5632, 1}, {16, 34, 4338, 1}};
	private final static int[] _buffers = {30598, 30599, 30600, 30601, 30602, 32135};

	public static PhantomPlayers getInstance()
	{
		return _instance;
	}

	public static void init()
	{
		_instance = new PhantomPlayers();
		_instance.load();
	}

	private void load()
	{
		config.load();
		dataService.loadSets("./config/phantom/town_sets.ini", sets);
		spotService.loadTownSpots("./config/phantom/town_locs.ini");
		dataService.loadTownClans("./config/phantom/town_clans.ini", registry.getClanLists());
		enchantPhrases = dataService.loadPhrases("./config/phantom/phrases_enchant.txt");
		lastPhrases = dataService.loadPhrases("./config/phantom/phrases_last.ini");
		behaviorService = new PhantomBehaviorService(enchantPhrases, _buffers, _mageBuff, _warrBuff);
		if(Config.ALLOW_PHANTOM_PLAYERS)
		{
			spawnerService.loadProfiles(config.getAccountName());
			phantomsLimit = Config.PHANTOM_PLAYERS_COUNT_FIRST + Config.PHANTOM_PLAYERS_COUNT_NEXT + 10;
			if(!registry.getProfiles().isEmpty())
				ThreadPoolManager.getInstance().schedule(new FantomTask(1), Config.PHANTOM_PLAYERS_DELAY_FIRST * 1000);
		}
		debugService.info("PhantomPlayers: loaded sets=" + sets.size() + " spots=" + spotService.size() + " profiles=" + registry.getProfiles().size());
	}

	public void startWalk(Player phantom)
	{
		ThreadPoolManager.getInstance().schedule(new PhantomWalk(phantom), 10000);
	}

	public String getRandomLastPhrase()
	{
		if(lastPhrases.isEmpty())
			return "";
		return lastPhrases.get(Rnd.get(lastPhrases.size() - 1));
	}

	private int getRandomPhantomNext()
	{
		List<Integer> all = new ArrayList<Integer>(registry.getProfiles().keySet());
		for(int i = 0; i < 30; i++)
		{
			int objId = all.get(Rnd.get(all.size() - 1));
			if(!containsObject(registry.getWave(1), objId) && !containsObject(registry.getWave(2), objId))
				return objId;
		}
		return all.get(Rnd.get(all.size() - 1));
	}

	private boolean containsObject(ConcurrentLinkedQueue<Player> queue, int objectId)
	{
		for(Player p : queue)
			if(p != null && p.getObjectId() == objectId)
				return true;
		return false;
	}

	private int getTotalPhantoms()
	{
		return registry.getWave(1).size() + registry.getWave(2).size();
	}

	private class PhantomWalk implements Runnable
	{
		private final Player phantom;
		public PhantomWalk(Player phantom)
		{
			this.phantom = phantom;
		}

		public void run()
		{
			if(!respawnService.ensureAliveState(phantom))
				return;
			behaviorService.processTownLife(phantom);

			if(Rnd.get(100) < 70)
				combatService.attackPlayer(phantom, targetService.findPvpTarget(phantom, 600));
			if(Rnd.get(100) < 10)
				combatService.attackMonster(phantom, targetService.findPveTarget(phantom, 800));

			if(Rnd.get(100) < 10)
				for(GameObject obj : World.getAroundObjects(phantom, 800, 200))
					if(obj.isItem() && phantom.getAI().getIntention() != CtrlIntention.AI_INTENTION_ATTACK && phantom.getAI().getIntention() != CtrlIntention.AI_INTENTION_CAST)
						phantom.getAI().setIntention(CtrlIntention.AI_INTENTION_PICK_UP, obj);

			movementService.randomWalkIfIdle(phantom);
			startWalk(phantom);
		}
	}

	public class FantomTask implements Runnable
	{
		private final int task;
		public FantomTask(int task)
		{
			this.task = task;
		}

		public void run()
		{
			int count = 0;
			int targetCount = task == 1 ? Config.PHANTOM_PLAYERS_COUNT_FIRST : Config.PHANTOM_PLAYERS_COUNT_NEXT;
			debugService.info("PhantomPlayers: wave " + task + " spawn started");
			while(count < targetCount)
			{
				int objectId = getRandomPhantomNext();
				if(containsObject(registry.getWave(task), objectId))
					continue;
				Player phantom = spawnerService.spawn(objectId);
				if(phantom == null)
					continue;
				registry.getWave(task).add(phantom);
				if(Config.PHANTOM_PLAYERS_SOULSHOT_ANIM && Rnd.get(100) < 45)
				{
					if(Rnd.get(100) < 3)
						phantom.sitDown(null);
					phantom.broadcastPacket(new MagicSkillUse(phantom, phantom, 2154, 1, 0, 0));
					phantom.broadcastPacket(new MagicSkillUse(phantom, phantom, 2164, 1, 0, 0));
				}
				startWalk(phantom);
				count++;
				try
				{
					Thread.sleep(task == 1 ? Config.PHANTOM_PLAYERS_DELAY_SPAWN_FIRST : Config.PHANTOM_PLAYERS_DELAY_SPAWN_NEXT);
				}
				catch(InterruptedException e)
				{
				}
			}
			if(task == 1)
			{
				ThreadPoolManager.getInstance().schedule(new FantomTask(2), Config.PHANTOM_PLAYERS_DELAY_NEXT);
				ThreadPoolManager.getInstance().schedule(new Social(), 12000L);
				ThreadPoolManager.getInstance().schedule(new CheckCount(), 300000L);
			}
			ThreadPoolManager.getInstance().schedule(new FantomTaskDespawn(task), task == 1 ? Config.PHANTOM_PLAYERS_DESPAWN_FIRST : Config.PHANTOM_PLAYERS_DESPAWN_NEXT);
		}
	}

	public class FantomTaskDespawn implements Runnable
	{
		private final int task;
		public FantomTaskDespawn(int task)
		{
			this.task = task;
		}
		public void run()
		{
			for(Player fantom : registry.getWave(task))
			{
				if(fantom == null)
					continue;
				fantom.setOnlineStatus(false);
				registry.getWave(task).remove(fantom);
				if(getTotalPhantoms() > phantomsLimit)
					continue;
				Player next = spawnerService.spawn(getRandomPhantomNext());
				if(next != null)
				{
					registry.getWave(task).add(next);
					startWalk(next);
				}
			}
			ThreadPoolManager.getInstance().schedule(new FantomTaskDespawn(task), task == 1 ? Config.PHANTOM_PLAYERS_DESPAWN_FIRST : Config.PHANTOM_PLAYERS_DESPAWN_NEXT);
		}
	}

	public class CheckCount implements Runnable
	{
		@Override
		public void run()
		{
			for(Map.Entry<Integer, ConcurrentLinkedQueue<Player>> entry : registry.getWaves().entrySet())
			{
				int limit = entry.getKey().intValue() == 1 ? Config.PHANTOM_PLAYERS_COUNT_FIRST : Config.PHANTOM_PLAYERS_COUNT_NEXT;
				int overflow = entry.getValue().size() - limit;
				if(overflow < 1)
					continue;
				for(Player fantom : entry.getValue())
				{
					fantom.setOnlineStatus(false);
					entry.getValue().remove(fantom);
					overflow--;
					if(overflow == 0)
						break;
				}
			}
			ThreadPoolManager.getInstance().schedule(new CheckCount(), 300000L);
		}
	}

	public class Social implements Runnable
	{
		@Override
		public void run()
		{
			for(Map.Entry<Integer, ConcurrentLinkedQueue<Player>> entry : registry.getWaves().entrySet())
				for(Player player : entry.getValue())
				{
					if(player == null || Rnd.get(100) >= 65)
						continue;
					ItemInstance wpn = player.getActiveWeaponInstance();
					if(wpn != null)
					{
						int ench = wpn.getEnchantLevel();
						if(Rnd.get(100) < 45 && ench <= Config.PHANTOM_PLAYERS_ENCHANT_MAX)
							wpn.setEnchantLevel(ench + 1);
						else if(Rnd.get(100) < 70)
							wpn.setEnchantLevel(3);
						player.broadcastUserInfo(true);
					}
					if(Rnd.get(100) < 5)
					{
						player.moveToLocation(player.getX() + Rnd.get(30), player.getY() + Rnd.get(30), player.getZ(), 40, true);
						player.getAI().setNextAction(nextAction.INTERACT, null, null, false, false);
					}
				}
			ThreadPoolManager.getInstance().schedule(new Social(), 12000L);
		}
	}
}
