package core.gameserver.instancemanager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import core.commons.threading.RunnableImpl;
import core.commons.util.Rnd;
import core.gameserver.Config;
import core.gameserver.ThreadPoolManager;
import core.gameserver.model.GameObjectsStorage;
import core.gameserver.model.Player;
import core.gameserver.network.l2.s2c.Earthquake;
import core.gameserver.network.l2.s2c.L2GameServerPacket;
import core.gameserver.utils.Location;
import core.gameserver.utils.MapUtils;

public class DragonValleyManager
{
	private static final Logger _log = LoggerFactory.getLogger(DragonValleyManager.class);
	private static DragonValleyManager _instance;
	private static final long spawnDelay = Config.DRAGON_MIGRATION_PERIOD * 60 * 1000L;
	private static final int spawnShance = Config.DRAGON_MIGRATION_CHANCE;
	private static final String[] migrationGroups = { "migration1", "migration2", "migration3", "migration4" };
	private boolean wasSpawned = false;
	
	public static DragonValleyManager getInstance()
	{
		if(_instance == null)
			_instance = new DragonValleyManager();
		return _instance;
	}
	
	public DragonValleyManager()
	{
		_log.info("DragonValley Manager: Loaded");
		manageSpawns();
	}
	
	private void manageSpawns()
	{
		for(String migrationGroup : migrationGroups)
			SpawnManager.getInstance().despawn(migrationGroup);
		
		ThreadPoolManager.getInstance().scheduleAtFixedRate(new RunnableImpl()
		{
			public void runImpl() throws Exception
			{
				if(Rnd.chance(spawnShance))
				{
					if(wasSpawned)
						for(String migrationGroup : migrationGroups)
							SpawnManager.getInstance().despawn(migrationGroup);
					
					for(String migrationGroup : migrationGroups)
					{
						SpawnManager.getInstance().spawn(migrationGroup);
						if(!wasSpawned)
							wasSpawned = true;
					}
					
					Location loc = new Location(101400, 117064, -3696); // Центр локации Dragon Valley
					L2GameServerPacket eq = new Earthquake(loc, 30, 12);
					
					int rx = MapUtils.regionX(loc.getX());
					int ry = MapUtils.regionY(loc.getY());
					for(Player player : GameObjectsStorage.getAllPlayersForIterate())
					{
						if(player.getReflection() != ReflectionManager.DEFAULT)
							continue;
						
						int tx = MapUtils.regionX(player);
						int ty = MapUtils.regionY(player);
						
						if(tx >= rx && tx <= rx && ty >= ry && ty <= ry)
							player.sendPacket(eq); // Играем землятресение для всех в локации при спавне
					}
				}
			}
		}, spawnDelay, spawnDelay);
	}
}