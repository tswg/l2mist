package core.gameserver;

import java.io.File;
import java.net.InetAddress;
import java.net.ServerSocket;

import net.sf.ehcache.CacheManager;
import core.commons.lang.StatsUtils;
import core.commons.listener.Listener;
import core.commons.listener.ListenerList;
import core.commons.net.nio.impl.SelectorThread;
import core.commons.versioning.Version;
import core.gameserver.cache.CrestCache;
import core.gameserver.captcha.CaptchaValidator;
import core.gameserver.dao.CharacterDAO;
import core.gameserver.dao.ItemsDAO;
import core.gameserver.dao.TopPlayersSystemDAO;
import core.gameserver.data.BoatHolder;
import core.gameserver.data.xml.Parsers;
import core.gameserver.data.xml.holder.EventHolder;
import core.gameserver.data.xml.holder.ResidenceHolder;
import core.gameserver.data.xml.holder.StaticObjectHolder;
import core.gameserver.database.DatabaseFactory;
import core.gameserver.geodata.GeoEngine;
import core.gameserver.handler.admincommands.AdminCommandHandler;
import core.gameserver.handler.items.ItemHandler;
import core.gameserver.handler.usercommands.UserCommandHandler;
import core.gameserver.handler.voicecommands.VoicedCommandHandler;
import core.gameserver.idfactory.IdFactory;
import core.gameserver.instancemanager.AutoAnnounce;
import core.gameserver.instancemanager.AutoSpawnManager;
import core.gameserver.instancemanager.BloodAltarManager;
import core.gameserver.instancemanager.CastleManorManager;
import core.gameserver.instancemanager.CoupleManager;
import core.gameserver.instancemanager.CursedWeaponsManager;
import core.gameserver.instancemanager.DimensionalRiftManager;
import core.gameserver.instancemanager.DragonValleyManager;
import core.gameserver.instancemanager.HellboundManager;
import core.gameserver.instancemanager.L2TopManager;
import core.gameserver.instancemanager.MMOTopManager;
import core.gameserver.instancemanager.PetitionManager;
import core.gameserver.instancemanager.PlayerMessageStack;
import core.gameserver.instancemanager.RaidBossSpawnManager;
import core.gameserver.instancemanager.SMSWayToPay;
import core.gameserver.instancemanager.SoDManager;
import core.gameserver.instancemanager.SoIManager;
import core.gameserver.instancemanager.SpawnManager;
import core.gameserver.instancemanager.games.FishingChampionShipManager;
import core.gameserver.instancemanager.games.LotteryManager;
import core.gameserver.instancemanager.games.MiniGameScoreManager;
import core.gameserver.instancemanager.itemauction.ItemAuctionManager;
import core.gameserver.instancemanager.naia.NaiaCoreManager;
import core.gameserver.instancemanager.naia.NaiaTowerManager;
import core.gameserver.listener.GameListener;
import core.gameserver.listener.game.OnShutdownListener;
import core.gameserver.listener.game.OnStartListener;
import core.gameserver.model.PhantomPlayers;
import core.gameserver.model.World;
import core.gameserver.model.entity.Hero;
import core.gameserver.model.entity.MonsterRace;
import core.gameserver.model.entity.SevenSigns;
import core.gameserver.model.entity.SevenSignsFestival.SevenSignsFestival;
import core.gameserver.model.entity.olympiad.Olympiad;
import core.gameserver.network.authcomm.AuthServerCommunication;
import core.gameserver.network.l2.GameClient;
import core.gameserver.network.l2.GamePacketHandler;
import core.gameserver.network.telnet.TelnetServer;
import core.gameserver.phantom.PhantomConfig;
import core.gameserver.phantom.PhantomManager;
import core.gameserver.scripts.Scripts;
import core.gameserver.tables.AugmentationData;
import core.gameserver.tables.ClanTable;
import core.gameserver.tables.EnchantHPBonusTable;
import core.gameserver.tables.FishTable;
import core.gameserver.tables.LevelUpTable;
import core.gameserver.tables.PetSkillsTable;
import core.gameserver.tables.SkillTreeTable;
import core.gameserver.taskmanager.ItemsAutoDestroy;
import core.gameserver.taskmanager.TaskManager;
import core.gameserver.taskmanager.tasks.RestoreOfflineTraders;
import core.gameserver.utils.MistWorldTeam;
import core.gameserver.utils.Strings;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GameServer
{
	public static final int AUTH_SERVER_PROTOCOL = 2;
	private static final Logger _log = LoggerFactory.getLogger(GameServer.class);

	public class GameServerListenerList extends ListenerList<GameServer>
	{
		public void onStart()
		{
			for(Listener<GameServer> listener : getListeners())
				if(OnStartListener.class.isInstance(listener))
					((OnStartListener) listener).onStart();
		}

		public void onShutdown()
		{
			for(Listener<GameServer> listener : getListeners())
				if(OnShutdownListener.class.isInstance(listener))
					((OnShutdownListener) listener).onShutdown();
		}
	}

	public static GameServer _instance;

	private final SelectorThread<GameClient> _selectorThreads[];
	private Version version;
	private TelnetServer statusServer;
	private final GameServerListenerList _listeners;

	private int _serverStarted;

	public SelectorThread<GameClient>[] getSelectorThreads()
	{
		return _selectorThreads;
	}

	public int time()
	{
		return (int) (System.currentTimeMillis() / 1000);
	}

	public int uptime()
	{
		return time() - _serverStarted;
	}

	@SuppressWarnings("unchecked")
	public GameServer() throws Exception
	{
		_instance = this;
		_serverStarted = time();
		_listeners = new GameServerListenerList();

		new File("./log/").mkdir();
		
		version = new Version(GameServer.class);

		_log.info("=================================================");
		_log.info("Revision: ................ " + version.getRevisionNumber());
		_log.info("Build date: .............. " + version.getBuildDate());
		_log.info("Compiler version: ........ " + version.getBuildJdk());
		_log.info("=================================================");

		// Initialize config
		Config.load();
		// Check binding address
		checkFreePorts();
		// Initialize database
		Class.forName(Config.DATABASE_DRIVER).newInstance();
		DatabaseFactory.getInstance().getConnection().close();

		IdFactory _idFactory = IdFactory.getInstance();
		if(!_idFactory.isInitialized())
		{
			_log.error("Could not read object IDs from DB. Please Check Your Data.");
			throw new Exception("Could not initialize the ID factory");
		}
		try
		{
			CacheManager.getInstance();
		}
		catch(Exception e)
		{
		}

		ThreadPoolManager.getInstance();
		Scripts.getInstance();
		GeoEngine.load();
		Strings.reload();
		GameTimeController.getInstance();
		World.init();
		Parsers.parseAll();
		ItemsDAO.getInstance();
		CrestCache.getInstance();
		CharacterDAO.getInstance();
		TopPlayersSystemDAO.getInstance();
		ClanTable.getInstance();
		FishTable.getInstance();
		SkillTreeTable.getInstance();
		AugmentationData.getInstance();
		EnchantHPBonusTable.getInstance();
		LevelUpTable.getInstance();
		PetSkillsTable.getInstance();
		ItemAuctionManager.getInstance();
		Scripts.getInstance().init();
		SpawnManager.getInstance().spawnAll();
		BoatHolder.getInstance().spawnAll();
		StaticObjectHolder.getInstance().spawnAll();
		RaidBossSpawnManager.getInstance();
		DimensionalRiftManager.getInstance();
		Announcements.getInstance();
		LotteryManager.getInstance();
		PlayerMessageStack.getInstance();
		if(Config.AUTODESTROY_ITEM_AFTER > 0)
			ItemsAutoDestroy.getInstance();
		MonsterRace.getInstance();
		SevenSigns.getInstance();
		SevenSignsFestival.getInstance();
		SevenSigns.getInstance().updateFestivalScore();
		AutoSpawnManager.getInstance();
		SevenSigns.getInstance().spawnSevenSignsNPC();
		if(Config.ENABLE_OLYMPIAD)
		{
			Olympiad.load();
			Hero.getInstance();
		}
		if(Config.ENABLE_TOP_PLAYERS_SYSTEM)
		{
			_log.info("TopPlayersSystem: Calculation results.");
			TopPlayersSystemDAO.getInstance().updatePvP();
			TopPlayersSystemDAO.getInstance().updatePK();
			TopPlayersSystemDAO.getInstance().updateLevel();
			TopPlayersSystemDAO.getInstance().updateCoins();
			try {
				Thread.sleep(1000L);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			TopPlayersSystemDAO.getInstance().insertPvP();
			TopPlayersSystemDAO.getInstance().insertPK();
			TopPlayersSystemDAO.getInstance().insertLevel();
			TopPlayersSystemDAO.getInstance().insertCoins();
			_log.info("TopPlayersSystem: Calculation done.");
		}
		PetitionManager.getInstance();
		CursedWeaponsManager.getInstance();
		if(!Config.ALLOW_WEDDING)
		{
			CoupleManager.getInstance();
			_log.info("CoupleManager initialized");
		}
		ItemHandler.getInstance();
		AdminCommandHandler.getInstance().log();
		UserCommandHandler.getInstance().log();
		VoicedCommandHandler.getInstance().log();
		TaskManager.getInstance();

		_log.info("=[Events]=========================================");
		ResidenceHolder.getInstance().callInit();
		EventHolder.getInstance().callInit();
		_log.info("==================================================");

		CastleManorManager.getInstance();
		Runtime.getRuntime().addShutdownHook(Shutdown.getInstance());
		_log.info("IdFactory: Free ObjectID's remaining: " + IdFactory.getInstance().size());

		CoupleManager.getInstance();

		if(Config.ALT_FISH_CHAMPIONSHIP_ENABLED)
			FishingChampionShipManager.getInstance();

		DragonValleyManager.getInstance();
		HellboundManager.getInstance();

		NaiaTowerManager.getInstance();
		NaiaCoreManager.getInstance();

		SoDManager.getInstance();
		SoIManager.getInstance();
		BloodAltarManager.getInstance();

		MiniGameScoreManager.getInstance();

		L2TopManager.getInstance();

		MMOTopManager.getInstance();

		SMSWayToPay.getInstance();
		
		Shutdown.getInstance().schedule(Config.RESTART_AT_TIME, Shutdown.RESTART);
		_log.info("GameServer Started");
		_log.info("Maximum Numbers of Connected Players: " + Config.MAXIMUM_ONLINE_USERS);
		
		if(Config.CAPTCHA_ENABLE)
		{
			CaptchaValidator.getInstance();
			_log.info("Anti-bot and Anti-autoclick System Enabled");
		}

		GamePacketHandler gph = new GamePacketHandler();

		InetAddress serverAddr = Config.GAMESERVER_HOSTNAME.equalsIgnoreCase("*") ? null : InetAddress.getByName(Config.GAMESERVER_HOSTNAME);

		_selectorThreads = new SelectorThread[Config.PORTS_GAME.length];
		for(int i = 0; i < Config.PORTS_GAME.length; i++)
		{
			_selectorThreads[i] = new SelectorThread<GameClient>(Config.SELECTOR_CONFIG, gph, gph, gph, null);
			_selectorThreads[i].openServerSocket(serverAddr, Config.PORTS_GAME[i]);
			_selectorThreads[i].start();
		}

		AuthServerCommunication.getInstance().start();

		if(Config.SERVICES_OFFLINE_TRADE_RESTORE_AFTER_RESTART)
			ThreadPoolManager.getInstance().schedule(new RestoreOfflineTraders(), 30000L);
		
		ThreadPoolManager.getInstance().scheduleAtFixedRate(new AutoAnnounce(), 60000, 60000);

		getListeners().onStart();

		if(Config.IS_TELNET_ENABLED)
			statusServer = new TelnetServer();
		else
			_log.info("Telnet server is currently disabled.");

		_log.info("=================================================");
		String memUsage = new StringBuilder().append(StatsUtils.getMemUsage()).toString();
		for(String line : memUsage.split("\n"))
			_log.info(line);

		MistWorldTeam.info();
		System.out.println("[PHANTOM] Loading phantom config before init...");
		try
		{
			PhantomConfig.load("config/phantom/phantom.properties");
			System.out.println("[PHANTOM] Phantom config loaded. ENABLED=" + PhantomConfig.ENABLED);
		}
		catch (Exception e)
		{
			PhantomConfig.ENABLED = false;
			System.err.println("[PHANTOM][ERROR] Failed to load config/phantom/phantom.properties. Phantom module disabled.");
			e.printStackTrace();
		}

		System.out.println("[PHANTOM] Calling PhantomManager.init()...");
		PhantomManager.getInstance().init();
	}

	public GameServerListenerList getListeners()
	{
		return _listeners;
	}

	public static GameServer getInstance()
	{
		return _instance;
	}

	public <T extends GameListener> boolean addListener(T listener)
	{
		return _listeners.add(listener);
	}

	public <T extends GameListener> boolean removeListener(T listener)
	{
		return _listeners.remove(listener);
	}

	public static void checkFreePorts()
	{
		boolean binded = false;
		while(!binded)
			for(int PORT_GAME : Config.PORTS_GAME)
				try
				{
					ServerSocket ss;
					if(Config.GAMESERVER_HOSTNAME.equalsIgnoreCase("*"))
						ss = new ServerSocket(PORT_GAME);
					else
						ss = new ServerSocket(PORT_GAME, 50, InetAddress.getByName(Config.GAMESERVER_HOSTNAME));
					ss.close();
					binded = true;
				}
				catch(Exception e)
				{
					_log.warn("Port " + PORT_GAME + " is allready binded. Please free it and restart server.");
					binded = false;
					try
					{
						Thread.sleep(1000);
					}
					catch(InterruptedException e2)
					{}
				}
	}

	public static void main(String[] args) throws Exception
	{
		new GameServer();
	}

	public Version getVersion()
	{
		return version;
	}

	public TelnetServer getStatusServer()
	{
		return statusServer;
	}
}
