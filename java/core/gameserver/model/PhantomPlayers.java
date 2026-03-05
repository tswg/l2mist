package core.gameserver.model;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.LineNumberReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import javolution.text.TextBuilder;
import javolution.util.FastList;
import javolution.util.FastMap;

import org.apache.log4j.Logger;

import core.commons.util.Rnd;
import core.gameserver.Config;
import core.gameserver.ThreadPoolManager;
import core.gameserver.ai.CtrlIntention;
import core.gameserver.ai.PlayableAI.nextAction;
import core.gameserver.database.DatabaseFactory;
import core.gameserver.geodata.GeoEngine;
import core.gameserver.model.base.InvisibleType;
import core.gameserver.model.base.Race;
import core.gameserver.model.instances.NpcInstance;
import core.gameserver.model.items.ItemInstance;
import core.gameserver.network.l2.components.ChatType;
import core.gameserver.network.l2.s2c.MagicSkillUse;
import core.gameserver.network.l2.s2c.Say2;
import core.gameserver.skills.AbnormalEffect;
import core.gameserver.tables.SkillTable;
import core.gameserver.templates.PlayerTemplate;
import core.gameserver.templates.item.CreateItem;
import core.gameserver.templates.item.ItemTemplate;
import core.gameserver.utils.ItemFunctions;
import core.gameserver.utils.Location;

/**
 * GrindTeam / FirstTeam H5 PhantomPlayers
 * Java 7 compatible.
 *
 * Adds:
 *  - phantom_spots.ini (grind spots by level with radius+weight)
 *  - roles: town idle/walker/trade/grind/roamer
 *  - teleport to spot/town (allowed by user)
 *  - per-phantom brain state
 *
 * Files:
 *  ./config/phantom/phantom_spots.ini
 *  ./config/phantom/phrases_trade.txt (optional, trade chat phrases)
 */
public class PhantomPlayers
{
	private static final Logger _log = Logger.getLogger(PhantomPlayers.class.getName());

	private static PhantomPlayers _instance;

	private static String _phantomAcc = Config.PHANTOM_PLAYERS_AKK;

	private static int _PhantomsLimit = 0;

	private static int _setsCount = 0;
	private static int _setsArcherCount = 0;
	private static int _setsOlyCount = 0;

	private static int _nameColCount = 0;
	private static int _titleColCount = 0;

	private static int _locsCount = 0;

	private static int _PhantomsEnchPhsCount = 0;
	private static int _PhantomsLastPhsCount = 0;
	private static int _PhantomsTradePhsCount = 0;

	private volatile int _PhantomsTownTotal = 0;

	private static FastList<Integer> _nameColors = new FastList<Integer>();
	private static FastList<Integer> _titleColors = new FastList<Integer>();

	private static FastList<L2Set> _sets = new FastList<L2Set>();
	private static FastList<L2Set> _setsArcher = new FastList<L2Set>();
	private static FastList<L2Set> _setsOly = new FastList<L2Set>();

	private static FastList<Location> _PhantomsTownLoc = new FastList<Location>();

	private static FastMap<Integer, L2Fantome> _phantoms = new FastMap<Integer, L2Fantome>();

	private static FastList<String> _PhantomsEnchPhrases = new FastList<String>();
	private static FastList<String> _PhantomsLastPhrases = new FastList<String>();
	private static FastList<String> _PhantomsTradePhrases = new FastList<String>();

	private static Map<Integer, ConcurrentLinkedQueue<Player>> _PhantomsTown = new ConcurrentHashMap<Integer, ConcurrentLinkedQueue<Player>>();

	/** Per phantom runtime brain. */
	private static final Map<Integer, PhantomBrain> _brains = new ConcurrentHashMap<Integer, PhantomBrain>();

	// -------------------- Roles --------------------
	private static enum PhantomRole {
		TOWN_IDLE,
		TOWN_WALKER,
		TOWN_TRADE,
		GRIND,
		ROAMER
	}

	// Default distribution (tweak as you like)
	private static final int ROLE_TOWN_IDLE_PCT  = 25;
	private static final int ROLE_TOWN_WALK_PCT  = 20;
	private static final int ROLE_TOWN_TRADE_PCT = 15;
	private static final int ROLE_GRIND_PCT      = 30;
	private static final int ROLE_ROAM_PCT       = 10;

	private static FastList<PhantomSpot> _spots = new FastList<PhantomSpot>();

	private static class PhantomBrain {
		final int objId;
		PhantomRole role;

		Location homeTown;
		Location spot;
		int spotRadius;

		long nextRoleChange;
		long nextChat;
		long nextAction;

		PhantomBrain(int objId) { this.objId = objId; }
	}

	// -------------------- Buffers / buffs --------------------
	private final static int[][] _mageBuff = new int[][]{
			{6, 75, 4322, 1}, // windwalk
			{6, 75, 4323, 1}, // shield
			{6, 75, 5637, 1}, // Magic Barrier 1
			{6, 75, 4328, 1}, // blessthesoul
			{6, 75, 4329, 1}, // acumen
			{6, 75, 4330, 1}, // concentration
			{6, 75, 4331, 1}, // empower
			{16, 34, 4338, 1}, // life cubic
	};

	private final static int[][] _warrBuff = new int[][]{
			{6, 75, 4322, 1}, // windwalk
			{6, 75, 4323, 1}, // shield
			{6, 75, 5637, 1}, // Magic Barrier 1
			{6, 75, 4324, 1}, // btb
			{6, 75, 4325, 1}, // vampirerage
			{6, 75, 4326, 1}, // regeneration
			{6, 39, 4327, 1}, // haste 1
			{40, 75, 5632, 1}, // haste 2
			{16, 34, 4338, 1}, // life cubic
	};

	private final static int[] _buffers = { 30598, 30599, 30600, 30601, 30602, 32135 };

	public static PhantomPlayers getInstance() { return _instance; }

	public static void init()
	{
		_instance = new PhantomPlayers();
		_instance.load();
	}

	private void load()
	{
		parceArmors();
		parceArcherArmors();
		parceOlyArmors();
		parceColors();
		cacheLastPhrases();
		cacheTradePhrases();
		parseSpots();

		if (Config.ALLOW_PHANTOM_PLAYERS)
		{
			parceTownLocs();
			cacheFantoms();

			cacheEnchantPhrases();

			_PhantomsLimit = Config.PHANTOM_PLAYERS_COUNT_FIRST + Config.PHANTOM_PLAYERS_COUNT_NEXT + 10;

			_PhantomsTown.put(1, new ConcurrentLinkedQueue<Player>());
			_PhantomsTown.put(2, new ConcurrentLinkedQueue<Player>());
		}
	}

	// -------------------- DB cache --------------------
	private void cacheFantoms() {
		new Thread(new Runnable() {
			public void run()
			{
				Connection con = null;
				try
				{
					con = DatabaseFactory.getInstance().getConnection();
					con.setTransactionIsolation(1);

					PreparedStatement st = con.prepareStatement(
							"SELECT obj_Id,char_name,title,x,y,z,clanid FROM characters WHERE account_name = ?"
					);
					st.setString(1, _phantomAcc);

					ResultSet rs = st.executeQuery();
					rs.setFetchSize(250);

					while (rs.next())
					{
						String name = rs.getString("char_name");
						_phantoms.put(Integer.valueOf(rs.getInt("obj_Id")),
								new L2Fantome(name, rs.getString("title"),
										rs.getInt("x"), rs.getInt("y"), rs.getInt("z"),
										rs.getInt("clanid")));
					}

					rs.close();
					st.close();
					con.close();

					_log.info("PhantomPlayers: Cached " + _phantoms.size() + " players.");
				}
				catch (Exception e)
				{
					_log.warn("PhantomPlayers: could not load chars from DB: " + e);
				}
				finally
				{
					con = null;
				}

				if (!_phantoms.isEmpty())
					ThreadPoolManager.getInstance().schedule(new FantomTask(1), Config.PHANTOM_PLAYERS_DELAY_FIRST * 1000L);
			}
		}).start();
	}

	// -------------------- Config parsers --------------------
	private void parceArmors()
	{
		_sets.clear();

		LineNumberReader lnr = null;
		BufferedReader br = null;
		FileReader fr = null;
		try
		{
			File Data = new File("./config/phantom/town_sets.ini");
			if (!Data.exists())
				return;

			fr = new FileReader(Data);
			br = new BufferedReader(fr);
			lnr = new LineNumberReader(br);

			String line;
			while ((line = lnr.readLine()) != null)
			{
				if (line.trim().length() == 0 || line.startsWith("#"))
					continue;

				String[] items = line.split(",");
				int custom = 0;
				try { custom = Integer.parseInt(items[5].trim()); } catch (Exception e) { custom = 0; }

				_sets.add(new L2Set(
						Integer.parseInt(items[0].trim()),
						Integer.parseInt(items[1].trim()),
						Integer.parseInt(items[2].trim()),
						Integer.parseInt(items[3].trim()),
						Integer.parseInt(items[4].trim()),
						Integer.parseInt(items[5].trim()),
						custom
				));
			}

			_setsCount = Math.max(0, _sets.size() - 1);
			_log.info("Load " + _sets.size() + " phantom armor sets");
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			try { if (fr != null) fr.close(); } catch (Exception e) {}
			try { if (br != null) br.close(); } catch (Exception e) {}
			try { if (lnr != null) lnr.close(); } catch (Exception e) {}
		}
	}

	private void parceArcherArmors()
	{
		_setsArcher.clear();

		LineNumberReader lnr = null;
		BufferedReader br = null;
		FileReader fr = null;
		try
		{
			File Data = new File("./config/phantom/archer_sets.ini");
			if (!Data.exists())
				return;

			fr = new FileReader(Data);
			br = new BufferedReader(fr);
			lnr = new LineNumberReader(br);

			String line;
			while ((line = lnr.readLine()) != null)
			{
				if (line.trim().length() == 0 || line.startsWith("#"))
					continue;

				String[] items = line.split(",");
				int custom = 0;
				try { custom = Integer.parseInt(items[5].trim()); } catch (Exception e) { custom = 0; }

				_setsArcher.add(new L2Set(
						Integer.parseInt(items[0].trim()),
						Integer.parseInt(items[1].trim()),
						Integer.parseInt(items[2].trim()),
						Integer.parseInt(items[3].trim()),
						Integer.parseInt(items[4].trim()),
						Integer.parseInt(items[5].trim()),
						custom
				));
			}

			_setsArcherCount = Math.max(0, _setsArcher.size() - 1);
			_log.info("Load " + _setsArcher.size() + " Aecher phantom armor sets");
		}
		catch (Exception e)
		{
			// ignore
		}
		finally
		{
			try { if (fr != null) fr.close(); } catch (Exception e) {}
			try { if (br != null) br.close(); } catch (Exception e) {}
			try { if (lnr != null) lnr.close(); } catch (Exception e) {}
		}
	}

	private void parceOlyArmors()
	{
		_setsOly.clear();

		LineNumberReader lnr = null;
		BufferedReader br = null;
		FileReader fr = null;
		try
		{
			File Data = new File("./config/phantom/oly_sets.ini");
			if (!Data.exists())
				return;

			fr = new FileReader(Data);
			br = new BufferedReader(fr);
			lnr = new LineNumberReader(br);

			String line;
			while ((line = lnr.readLine()) != null)
			{
				if (line.trim().length() == 0 || line.startsWith("#"))
					continue;

				String[] items = line.split(",");
				int custom = 0;
				try { custom = Integer.parseInt(items[6].trim()); } catch (Exception e) { custom = 0; }

				_setsOly.add(new L2Set(
						Integer.parseInt(items[0].trim()),
						Integer.parseInt(items[1].trim()),
						Integer.parseInt(items[2].trim()),
						Integer.parseInt(items[3].trim()),
						Integer.parseInt(items[4].trim()),
						Integer.parseInt(items[5].trim()),
						custom
				));
			}

			_setsOlyCount = Math.max(0, _setsOly.size() - 1);
			_log.info("Load " + _setsOly.size() + " phantom Only armor sets");
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			try { if (fr != null) fr.close(); } catch (Exception e) {}
			try { if (br != null) br.close(); } catch (Exception e) {}
			try { if (lnr != null) lnr.close(); } catch (Exception e) {}
		}
	}

	private void parceTownLocs()
	{
		_PhantomsTownLoc.clear();

		LineNumberReader lnr = null;
		BufferedReader br = null;
		FileReader fr = null;

		try
		{
			File Data = new File("./config/phantom/town_locs.ini");
			if (!Data.exists())
				return;

			fr = new FileReader(Data);
			br = new BufferedReader(fr);
			lnr = new LineNumberReader(br);

			String line;
			while ((line = lnr.readLine()) != null)
			{
				if (line.trim().length() == 0 || line.startsWith("#"))
					continue;

				String[] items = line.split(",");
				_PhantomsTownLoc.add(new Location(
						Integer.parseInt(items[0].trim()),
						Integer.parseInt(items[1].trim()),
						Integer.parseInt(items[2].trim())
				));
			}

			_locsCount = Math.max(0, _PhantomsTownLoc.size() - 1);
			_log.info("Load " + _PhantomsTownLoc.size() + " phantom Town Locations");
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			try { if (fr != null) fr.close(); } catch (Exception e) {}
			try { if (br != null) br.close(); } catch (Exception e) {}
			try { if (lnr != null) lnr.close(); } catch (Exception e) {}
		}
	}

	private void parceColors()
	{
		_nameColors = Config.PHANTOM_PLAYERS_NAME_CLOLORS;
		_titleColors = Config.PHANTOM_PLAYERS_TITLE_CLOLORS;

		_nameColCount = Math.max(0, _nameColors.size() - 1);
		_titleColCount = Math.max(0, _titleColors.size() - 1);
	}

	private void cacheEnchantPhrases()
	{
		_PhantomsEnchPhrases.clear();

		LineNumberReader lnr = null;
		BufferedReader br = null;
		FileReader fr = null;
		try
		{
			File Data = new File("./config/phantom/phrases_enchant.txt");
			if (!Data.exists())
				return;

			fr = new FileReader(Data);
			br = new BufferedReader(fr);
			lnr = new LineNumberReader(br);

			String line;
			while ((line = lnr.readLine()) != null)
			{
				if (line.trim().length() == 0 || line.startsWith("#"))
					continue;
				_PhantomsEnchPhrases.add(line);
			}

			_PhantomsEnchPhsCount = Math.max(0, _PhantomsEnchPhrases.size() - 1);
			_log.info("Load " + _PhantomsEnchPhrases.size() + " phantom Ench");
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			try { if (fr != null) fr.close(); } catch (Exception e) {}
			try { if (br != null) br.close(); } catch (Exception e) {}
			try { if (lnr != null) lnr.close(); } catch (Exception e) {}
		}
	}

	private void cacheLastPhrases()
	{
		_PhantomsLastPhrases.clear();

		LineNumberReader lnr = null;
		BufferedReader br = null;
		FileReader fr = null;
		try
		{
			File Data = new File("./config/phantom/phrases_last.ini");
			if (!Data.exists())
				return;

			fr = new FileReader(Data);
			br = new BufferedReader(fr);
			lnr = new LineNumberReader(br);

			String line;
			while ((line = lnr.readLine()) != null)
			{
				if (line.trim().length() == 0 || line.startsWith("#"))
					continue;
				_PhantomsLastPhrases.add(line);
			}

			_PhantomsLastPhsCount = Math.max(0, _PhantomsLastPhrases.size() - 1);
			_log.info("Load " + _PhantomsLastPhrases.size() + " phantom Last");
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			try { if (fr != null) fr.close(); } catch (Exception e) {}
			try { if (br != null) br.close(); } catch (Exception e) {}
			try { if (lnr != null) lnr.close(); } catch (Exception e) {}
		}
	}

	private void cacheTradePhrases()
	{
		_PhantomsTradePhrases.clear();

		LineNumberReader lnr = null;
		BufferedReader br = null;
		FileReader fr = null;
		try
		{
			File Data = new File("./config/phantom/phrases_trade.txt");
			if (!Data.exists())
			{
				// optional
				return;
			}

			fr = new FileReader(Data);
			br = new BufferedReader(fr);
			lnr = new LineNumberReader(br);

			String line;
			while ((line = lnr.readLine()) != null)
			{
				if (line.trim().length() == 0 || line.startsWith("#"))
					continue;
				_PhantomsTradePhrases.add(line);
			}

			_PhantomsTradePhsCount = Math.max(0, _PhantomsTradePhrases.size() - 1);
			_log.info("PhantomPlayers: Loaded " + _PhantomsTradePhrases.size() + " trade phrases.");
		}
		catch (Exception e)
		{
			_log.warn("PhantomPlayers: error loading phrases_trade.txt", e);
		}
		finally
		{
			try { if (fr != null) fr.close(); } catch (Exception e) {}
			try { if (br != null) br.close(); } catch (Exception e) {}
			try { if (lnr != null) lnr.close(); } catch (Exception e) {}
		}
	}

	private void parseSpots()
	{
		_spots.clear();

		LineNumberReader lnr = null;
		BufferedReader br = null;
		FileReader fr = null;

		try
		{
			File Data = new File("./config/phantom/phantom_spots.ini");
			if (!Data.exists())
			{
				_log.warn("PhantomPlayers: phantom_spots.ini not found!");
				return;
			}

			fr = new FileReader(Data);
			br = new BufferedReader(fr);
			lnr = new LineNumberReader(br);

			String line;
			while ((line = lnr.readLine()) != null)
			{
				line = line.trim();
				if (line.length() == 0 || line.startsWith("#"))
					continue;

				// cut inline comments (# or ;)
				int hash = line.indexOf('#');
				if (hash >= 0) line = line.substring(0, hash);
				int semi = line.indexOf(';');
				if (semi >= 0) line = line.substring(0, semi);

				line = line.trim();
				if (line.length() == 0)
					continue;

				// min,max,x,y,z,radius,weight
				String[] p = line.split(",");
				if (p.length < 7)
					continue;

				int minLvl = Integer.parseInt(p[0].trim());
				int maxLvl = Integer.parseInt(p[1].trim());
				int x = Integer.parseInt(p[2].trim());
				int y = Integer.parseInt(p[3].trim());
				int z = Integer.parseInt(p[4].trim());
				int radius = Integer.parseInt(p[5].trim());
				int weight = Integer.parseInt(p[6].trim());

				if (weight < 1) weight = 1;
				if (radius < 100) radius = 100;

				_spots.add(new PhantomSpot(minLvl, maxLvl, x, y, z, radius, weight));
			}

			_log.info("PhantomPlayers: Loaded " + _spots.size() + " phantom spots.");
		}
		catch (Exception e)
		{
			_log.warn("PhantomPlayers: error loading phantom_spots.ini", e);
		}
		finally
		{
			try { if (fr != null) fr.close(); } catch (Exception e) {}
			try { if (br != null) br.close(); } catch (Exception e) {}
			try { if (lnr != null) lnr.close(); } catch (Exception e) {}
		}
	}

	// -------------------- Helpers --------------------
	private L2Set getRandomSet() { return _sets.get(Rnd.get(_setsCount)); }

	private Location getRandomLoc()
	{
		if (_PhantomsTownLoc.isEmpty())
			return new Location(83400, 147900, -3400);
		return _PhantomsTownLoc.get(Rnd.get(0, _locsCount));
	}

	private PhantomRole rollRole()
	{
		int r = Rnd.get(100);
		if (r < ROLE_TOWN_IDLE_PCT) return PhantomRole.TOWN_IDLE;
		r -= ROLE_TOWN_IDLE_PCT;
		if (r < ROLE_TOWN_WALK_PCT) return PhantomRole.TOWN_WALKER;
		r -= ROLE_TOWN_WALK_PCT;
		if (r < ROLE_TOWN_TRADE_PCT) return PhantomRole.TOWN_TRADE;
		r -= ROLE_TOWN_TRADE_PCT;
		if (r < ROLE_GRIND_PCT) return PhantomRole.GRIND;
		return PhantomRole.ROAMER;
	}

	private PhantomSpot getRandomSpotForLevel(int lvl)
	{
		if (_spots.isEmpty())
			return null;

		int total = 0;
		for (int i = 0; i < _spots.size(); i++)
		{
			PhantomSpot s = _spots.get(i);
			if (s != null && s.fits(lvl))
				total += s.weight;
		}
		if (total <= 0)
			return null;

		int r = Rnd.get(total);
		int sum = 0;
		for (int i = 0; i < _spots.size(); i++)
		{
			PhantomSpot s = _spots.get(i);
			if (s == null || !s.fits(lvl))
				continue;
			sum += s.weight;
			if (r < sum)
				return s;
		}
		return null;
	}

	private static int dist2D(Player p, Location loc)
	{
		int dx = p.getX() - loc.getX();
		int dy = p.getY() - loc.getY();
		long d2 = (long) dx * dx + (long) dy * dy;
		return (int) Math.sqrt((double) d2);
	}

	private String getRandomEnchantPhrase()
	{
		if (_PhantomsEnchPhrases.isEmpty()) return "...";
		return _PhantomsEnchPhrases.get(Rnd.get(_PhantomsEnchPhsCount));
	}

	public String getRandomLastPhrase()
	{
		if (_PhantomsLastPhrases.isEmpty()) return "...";
		return _PhantomsLastPhrases.get(Rnd.get(_PhantomsLastPhsCount));
	}

	private String getRandomTradePhrase()
	{
		if (_PhantomsTradePhrases.isEmpty())
			return "WTS / PM";
		return _PhantomsTradePhrases.get(Rnd.get(_PhantomsTradePhsCount));
	}

	private int getNameColor()
	{
		if (_nameColors.isEmpty()) return 0xFFFFFF;
		return (_nameColors.get(Rnd.get(_nameColCount))).intValue();
	}

	private int getTitleColor()
	{
		if (_titleColors.isEmpty()) return 0xFFFF77;
		return (_titleColors.get(Rnd.get(_titleColCount))).intValue();
	}

	private void tryBuffNearNpc(Player p)
	{
		if (Rnd.get(100) >= 80)
			return;

		for (NpcInstance npc : World.getAroundNpc(p, 100, 100))
		{
			for (int buffers_id : _buffers)
			{
				if (npc.getNpcId() != buffers_id || p.isSitting())
					continue;

				if (!p.getClassId().isMage() || p.getClassId().getRace() == Race.orc)
				{
					for (int[] buff : _warrBuff)
						npc.broadcastPacket(new MagicSkillUse(npc, p, buff[2], buff[3], 0, 0));
				}
				else
				{
					for (int[] buff : _mageBuff)
						npc.broadcastPacket(new MagicSkillUse(npc, p, buff[2], buff[3], 0, 0));
				}
				return;
			}
		}
	}

	private void tryChat(Player p, PhantomBrain b, long now)
	{
		if (!Config.ALLOW_PHANTOM_CHAT)
			return;
		if (now < b.nextChat)
			return;
		if (Rnd.get(100) >= Config.PHANTOM_CHAT_CHANSE)
			return;

		b.nextChat = now + Rnd.get(60_000, 240_000);

		ChatType type = ChatType.ALL;
		String text = getRandomEnchantPhrase();

		if (b.role == PhantomRole.TOWN_TRADE)
		{
			type = ChatType.TRADE;
			text = getRandomTradePhrase();
		}
		else if (Rnd.get(100) < 15)
		{
			type = ChatType.SHOUT;
		}

		Say2 cs = new Say2(p.getObjectId(), type, p.getName(), text);

		int r = (type == ChatType.SHOUT) ? 10000 : (type == ChatType.TRADE ? 5000 : 1200);
		int h = (type == ChatType.SHOUT) ? 3000 : (type == ChatType.TRADE ? 2000 : 1000);

		for (Player pl : World.getAroundPlayers(p, r, h))
			if (pl != null && !pl.isBlockAll())
				pl.sendPacket(cs);
	}

	private NpcInstance findNearestMob(Player p, int radius)
	{
		NpcInstance best = null;
		int bestDist = Integer.MAX_VALUE;

		for (NpcInstance npc : World.getAroundNpc(p, radius, 300))
		{
			if (npc == null || !npc.isMonster() || npc.isDead())
				continue;
			if (!GeoEngine.canSeeTarget(p, npc, false))
				continue;

			int d = (int) p.getRealDistance3D(npc); // npc is GameObject => OK
			if (d < bestDist)
			{
				bestDist = d;
				best = npc;
			}
		}
		return best;
	}

	// -------------------- Data structs --------------------
	static class L2Set
	{
		public int _body;
		public int _gaiters;
		public int _gloves;
		public int _boots;
		public int _weapon;
		public int _custom;
		public int _grade;

		L2Set(int bod, int gaiter, int glove, int boot, int weapon, int grade, int custom)
		{
			_body = bod;
			_gaiters = gaiter;
			_gloves = glove;
			_boots = boot;
			_weapon = weapon;
			_grade = grade;
			_custom = custom;
		}
	}

	static class L2Fantome
	{
		public String name;
		public String title;
		public int x;
		public int y;
		public int z;
		public int clanId;

		L2Fantome(String name, String title, int x, int y, int z, int clanId)
		{
			this.name = name;
			this.title = title;
			this.x = x;
			this.y = y;
			this.z = z;
			this.clanId = clanId;
		}
	}

	// -------------------- Tasks --------------------
	public class FantomTask implements Runnable
	{
		public int _task;
		public FantomTask(int task) { _task = task; }

		public void run()
		{
			int count = 0;
			int PhantomObjId;

			switch (_task)
			{
				case 1:
				{
					_log.info("PhantomPlayers: 1st wave, spawn started.");

					while (count < Config.PHANTOM_PLAYERS_COUNT_FIRST)
					{
						Player fantom;
						PhantomObjId = getRandomPhantomAny();

                        try {
                            fantom = loadPhantom(PhantomObjId);
                        } catch (SQLException e) {
                            throw new RuntimeException(e);
                        }
                        if (fantom == null)
							continue;

						_PhantomsTown.get(1).add(fantom);

						if (Config.PHANTOM_PLAYERS_SOULSHOT_ANIM && Rnd.get(100) < 45)
						{
							try { Thread.sleep(900L); } catch (InterruptedException e) {}
							if (Rnd.get(100) < 5) fantom.sitDown(null);
							if (Rnd.get(100) < 30) fantom.startAbnormalEffect(AbnormalEffect.S_NAVIT);
						}

						try { Thread.sleep(Config.PHANTOM_PLAYERS_DELAY_SPAWN_FIRST); } catch (InterruptedException e) {}
						count++;
					}

					_log.info("PhantomPlayers: 1st wave, spawned " + count + " players.");

					ThreadPoolManager.getInstance().schedule(new FantomTaskDespawn(1), Config.PHANTOM_PLAYERS_DESPAWN_FIRST);
					ThreadPoolManager.getInstance().schedule(new FantomTask(2), Config.PHANTOM_PLAYERS_DELAY_NEXT);
					ThreadPoolManager.getInstance().schedule(new Social(), 12000L);
					ThreadPoolManager.getInstance().schedule(new CheckCount(), 300000L);
					break;
				}

				case 2:
				{
					_log.info("PhantomPlayers: 2nd wave, spawn started.");

					while (count < Config.PHANTOM_PLAYERS_COUNT_NEXT)
					{
						Player fantom2;
						PhantomObjId = getRandomPhantomAny();

                        try {
                            fantom2 = loadPhantom(PhantomObjId);
                        } catch (SQLException e) {
                            throw new RuntimeException(e);
                        }
                        if (fantom2 == null)
							continue;

						_PhantomsTown.get(2).add(fantom2);

						if (Config.PHANTOM_PLAYERS_SOULSHOT_ANIM && Rnd.get(100) < 45)
						{
							try { Thread.sleep(900L); } catch (InterruptedException e) {}
							if (Rnd.get(100) < 3) fantom2.sitDown(null);
							fantom2.broadcastPacket(new MagicSkillUse(fantom2, fantom2, 2154, 1, 0, 0));
							try { Thread.sleep(300L); } catch (InterruptedException e) {}
							fantom2.broadcastPacket(new MagicSkillUse(fantom2, fantom2, 2164, 1, 0, 0));
						}

						try { Thread.sleep(Config.PHANTOM_PLAYERS_DELAY_SPAWN_NEXT); } catch (InterruptedException e) {}
						count++;
					}

					_log.info("PhantomPlayers: 2nd wave, spawned " + count + " players.");
					ThreadPoolManager.getInstance().schedule(new FantomTaskDespawn(2), Config.PHANTOM_PLAYERS_DESPAWN_NEXT);
					break;
				}
			}
		}
	}

	public class FantomTaskDespawn implements Runnable
	{
		public int _task;
		public FantomTaskDespawn(int task) { _task = task; }

		public void run()
		{
			ConcurrentLinkedQueue<Player> players = _PhantomsTown.get(_task);
			if (players == null || players.isEmpty())
			{
				ThreadPoolManager.getInstance().schedule(new FantomTaskDespawn(_task),
						_task == 1 ? Config.PHANTOM_PLAYERS_DESPAWN_FIRST : Config.PHANTOM_PLAYERS_DESPAWN_NEXT);
				return;
			}

			for (Player fantom : players)
			{
				if (fantom == null)
					continue;

				fantom.setOnlineStatus(false);
				_brains.remove(fantom.getObjectId());
				_PhantomsTown.get(_task).remove(fantom);

				try {
					Thread.sleep(_task == 1 ? Config.PHANTOM_PLAYERS_DELAY_DESPAWN_FIRST : Config.PHANTOM_PLAYERS_DELAY_DESPAWN_NEXT);
				} catch (InterruptedException e) {}

				if (_PhantomsTownTotal > _PhantomsLimit)
					continue;

				int nextObjId = getRandomPhantomAny();
                Player next = null;
                try {
                    next = loadPhantom(nextObjId);
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
                if (next == null)
					continue;

				_PhantomsTown.get(_task).add(next);
			}

			ThreadPoolManager.getInstance().schedule(new FantomTaskDespawn(_task),
					_task == 1 ? Config.PHANTOM_PLAYERS_DESPAWN_FIRST : Config.PHANTOM_PLAYERS_DESPAWN_NEXT);
		}
	}

	public class CheckCount implements Runnable
	{
		public void run()
		{
			for (Map.Entry<Integer, ConcurrentLinkedQueue<Player>> entry : _PhantomsTown.entrySet())
			{
				Integer wave = entry.getKey();
				ConcurrentLinkedQueue<Player> players = entry.getValue();
				if (wave == null || players == null || players.isEmpty())
					continue;

				int limit = (wave.intValue() == 1 ? Config.PHANTOM_PLAYERS_COUNT_FIRST : Config.PHANTOM_PLAYERS_COUNT_NEXT);
				int overflow = players.size() - limit;
				if (overflow < 1)
					continue;

				for (Player fantom : players)
				{
					if (fantom == null) continue;
					fantom.setOnlineStatus(false);
					_brains.remove(fantom.getObjectId());
					_PhantomsTown.get(wave).remove(fantom);
					overflow--;
					if (overflow == 0)
						break;
				}
			}
			ThreadPoolManager.getInstance().schedule(new CheckCount(), 300000L);
		}
	}

	public class Social implements Runnable
	{
		public void run()
		{
			TextBuilder tb = new TextBuilder();

			for (Map.Entry<Integer, ConcurrentLinkedQueue<Player>> entry : _PhantomsTown.entrySet())
			{
				ConcurrentLinkedQueue<Player> players = entry.getValue();
				if (players == null || players.isEmpty())
					continue;

				int count = 0;
				for (Player player : players)
				{
					if (player == null)
						continue;

					if (Rnd.get(100) < 65)
					{
						switch (Rnd.get(2))
						{
							case 0:
							case 1:
								ItemInstance wpn = player.getActiveWeaponInstance();
								if (wpn != null)
								{
									int enh = wpn.getEnchantLevel();
									int nextench = enh + 1;

									if (Rnd.get(100) < 45 && enh <= Config.PHANTOM_PLAYERS_ENCHANT_MAX)
										wpn.setEnchantLevel(nextench);
									else if (Rnd.get(100) < 70)
									{
										wpn.setEnchantLevel(3);
										if (nextench > 13 && Rnd.get(100) < 2)
										{
											tb.append("!");
											for (int i = Rnd.get(2, 13); i > 0; i--)
												tb.append("!");
											tb.clear();
										}
									}
									player.broadcastUserInfo(true);
								}
								break;

							case 2:
								if (Rnd.get(100) >= 5)
									break;
								player.moveToLocation(player.getX() + Rnd.get(30), player.getY() + Rnd.get(30), player.getZ(), 40, true);
								player.getAI().setNextAction(nextAction.INTERACT, null, null, false, false);
								break;
						}
						try { Thread.sleep(Rnd.get(500, 1500)); } catch (InterruptedException e) {}
						count++;
					}

					if (count > 55)
						break;
				}
			}

			tb.clear();
			ThreadPoolManager.getInstance().schedule(new Social(), 12000L);
		}
	}

	// -------------------- Spawn / AI loop --------------------
	public void startWalk(Player phantom)
	{
		ThreadPoolManager.getInstance().schedule(new PhantomWalk(phantom), 10000L);
	}

	private class PhantomWalk implements Runnable
	{
		private final Player _phantom;

		public PhantomWalk(Player phantom)
		{
			_phantom = phantom;
		}

		public void run()
		{
			if (_phantom == null)
				return;

			if (_phantom.isDead())
			{
				_phantom.kick();
				return;
			}
			if (_phantom.isInWater())
				_phantom.teleToClosestTown();

			PhantomBrain b = _brains.get(_phantom.getObjectId());
			if (b == null)
			{
				startWalk(_phantom);
				return;
			}

			long now = System.currentTimeMillis();
			if (now < b.nextAction)
			{
				startWalk(_phantom);
				return;
			}
			b.nextAction = now + Rnd.get(3000, 8000);

			// role switch + teleport allowed
			if (now > b.nextRoleChange)
			{
				b.role = rollRole();
				b.nextRoleChange = now + Rnd.get(10 * 60_000, 35 * 60_000);

				if (b.role == PhantomRole.GRIND || b.role == PhantomRole.ROAMER)
				{
					PhantomSpot sp = getRandomSpotForLevel(_phantom.getLevel());
					if (sp != null)
					{
						b.spot = sp.loc;
						b.spotRadius = sp.radius;
					}
					else
					{
						b.spot = b.homeTown;
						b.spotRadius = 800;
					}

					_phantom.teleToLocation(
							b.spot.getX() + Rnd.get(-120, 120),
							b.spot.getY() + Rnd.get(-120, 120),
							b.spot.getZ()
					);
				}
				else
				{
					Location t = b.homeTown != null ? b.homeTown : getRandomLoc();
					_phantom.teleToLocation(
							t.getX() + Rnd.get(-80, 80),
							t.getY() + Rnd.get(-80, 80),
							t.getZ()
					);
				}
			}

			switch (b.role)
			{
				case TOWN_IDLE:
					doTownIdle(_phantom, b, now);
					break;
				case TOWN_WALKER:
					doTownWalk(_phantom, b, now);
					break;
				case TOWN_TRADE:
					doTownTrade(_phantom, b, now);
					break;
				case GRIND:
					doGrind(_phantom, b, now, false);
					break;
				case ROAMER:
					doGrind(_phantom, b, now, true);
					break;
			}

			startWalk(_phantom);
		}
	}

	private void doTownIdle(Player p, PhantomBrain b, long now)
	{
		if (Rnd.get(100) < 12 && !p.isSitting())
			p.sitDown(null);
		if (Rnd.get(100) < 8 && p.isSitting())
			p.standUp();

		tryBuffNearNpc(p);
		tryChat(p, b, now);

		if (Rnd.get(100) < 4)
			p.broadcastPacket(new MagicSkillUse(p, p, 2154, 1, 0, 0));
	}

	private void doTownWalk(Player p, PhantomBrain b, long now)
	{
		tryBuffNearNpc(p);
		tryChat(p, b, now);

		if (p.getAI().getIntention() != CtrlIntention.AI_INTENTION_ATTACK
				&& p.getAI().getIntention() != CtrlIntention.AI_INTENTION_CAST)
		{
			if (Rnd.get(100) < 80)
				p.rndWalk();
		}
	}

	private void doTownTrade(Player p, PhantomBrain b, long now)
	{
		if (!p.isSitting())
			p.sitDown(null);

		// if your core supports private store API - place it here.
		// now just imitate trader by trade chat sometimes.
		if (Config.ALLOW_PHANTOM_CHAT && now > b.nextChat && Rnd.get(100) < 35)
		{
			b.nextChat = now + Rnd.get(60_000, 180_000);
			Say2 cs = new Say2(p.getObjectId(), ChatType.TRADE, p.getName(), getRandomTradePhrase());
			for (Player pl : World.getAroundPlayers(p, 3000, 1500))
				if (pl != null && !pl.isBlockAll())
					pl.sendPacket(cs);
		}
	}

	private void doGrind(Player p, PhantomBrain b, long now, boolean roamer)
	{
		if (b.spot == null)
		{
			PhantomSpot sp = getRandomSpotForLevel(p.getLevel());
			if (sp != null)
			{
				b.spot = sp.loc;
				b.spotRadius = sp.radius;
			}
			else
			{
				b.spot = b.homeTown != null ? b.homeTown : getRandomLoc();
				b.spotRadius = 800;
			}
		}

		// keep near spot, teleport back if too far
		if (dist2D(p, b.spot) > b.spotRadius * 2)
		{
			p.teleToLocation(
					b.spot.getX() + Rnd.get(-120, 120),
					b.spot.getY() + Rnd.get(-120, 120),
					b.spot.getZ()
			);
			return;
		}

		// small chance to chat even on grind (rare)
		if (Rnd.get(100) < 10)
			tryChat(p, b, now);

		// target mob
		NpcInstance mob = findNearestMob(p, 900);
		if (mob != null)
		{
			p.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, mob);

			// simple mage cast imitation
			if (p.getClassId().isMage() && p.getClassId().getRace() != Race.orc && Rnd.get(100) < 35)
				p.doCast(SkillTable.getInstance().getInfo(1177, 2), mob, true);

			return;
		}

		// move around spot
		if (Rnd.get(100) < 70)
		{
			p.moveToLocation(
					b.spot.getX() + Rnd.get(-b.spotRadius, b.spotRadius),
					b.spot.getY() + Rnd.get(-b.spotRadius, b.spotRadius),
					b.spot.getZ(),
					40,
					true
			);
			p.getAI().setNextAction(nextAction.INTERACT, null, null, false, false);
		}

		// roamer sometimes changes spot
		if (roamer && Rnd.get(100) < 3)
		{
			PhantomSpot sp = getRandomSpotForLevel(p.getLevel());
			if (sp != null)
			{
				b.spot = sp.loc;
				b.spotRadius = sp.radius;
				p.teleToLocation(sp.loc.getX(), sp.loc.getY(), sp.loc.getZ());
			}
		}
	}

	// -------------------- Phantom creation --------------------
	private int getRandomPhantomAny()
	{
		// Your original range
		return Rnd.get(600000000, 600006081);
	}

	// --- helpers: load level from DB ---
	private int loadLevelForClass(Connection con, int objId, int classId) throws SQLException
	{
		try (PreparedStatement st = con.prepareStatement(
				"SELECT level FROM character_subclasses WHERE char_obj_id=? AND class_id=? LIMIT 1"))
		{
			st.setInt(1, objId);
			st.setInt(2, classId);
			try (ResultSet rs = st.executeQuery())
			{
				return rs.next() ? rs.getInt("level") : -1;
			}
		}
	}

	private int loadActiveOrBaseLevel(Connection con, int objId) throws SQLException
	{
		// 1) active
		try (PreparedStatement st = con.prepareStatement(
				"SELECT level FROM character_subclasses WHERE char_obj_id=? AND active=1 LIMIT 1"))
		{
			st.setInt(1, objId);
			try (ResultSet rs = st.executeQuery())
			{
				if (rs.next())
					return rs.getInt("level");
			}
		}

		// 2) base
		try (PreparedStatement st = con.prepareStatement(
				"SELECT level FROM character_subclasses WHERE char_obj_id=? AND isBase=1 LIMIT 1"))
		{
			st.setInt(1, objId);
			try (ResultSet rs = st.executeQuery())
			{
				if (rs.next())
					return rs.getInt("level");
			}
		}

		return 1;
	}

	private Player loadPhantom(int objId) throws SQLException
	{
		int nbPlayerIG = GameObjectsStorage.getAllPlayersCount();
		if (nbPlayerIG >= Config.MAXIMUM_ONLINE_USERS)
			return null;

		L2Fantome phantom = _phantoms.get(objId);
		if (phantom == null)
			return null;

		// --- choose equipment set ---
		L2Set set = getRandomSet();

		ItemInstance body = (set._body != 0) ? ItemFunctions.createItem(set._body) : null;
		ItemInstance gaiters = (set._gaiters != 0) ? ItemFunctions.createItem(set._gaiters) : null;
		ItemInstance gloves = (set._gloves != 0) ? ItemFunctions.createItem(set._gloves) : null;
		ItemInstance boots = (set._boots != 0) ? ItemFunctions.createItem(set._boots) : null;
		ItemInstance weapon = (set._weapon != 0) ? ItemFunctions.createItem(set._weapon) : null;

		// --- your "starting level suggestion" from grade (only used if DB has no level) ---
		int grade = set._grade;
		int suggestedLevel = 1;
		if (grade == 0) suggestedLevel = Rnd.get(1, 19);
		else if (grade == 1) suggestedLevel = Rnd.get(20, 39);
		else if (grade == 2) suggestedLevel = Rnd.get(40, 51);
		else if (grade == 3) suggestedLevel = Rnd.get(52, 60);
		else if (grade == 4) suggestedLevel = Rnd.get(61, 75);
		else if (grade == 5) suggestedLevel = Rnd.get(76, 80);

		// --- IMPORTANT: choose phantom classId here (3rd prof, etc.) ---
		// Сейчас у тебя всегда classId=0 => это ошибка, если ты хочешь 3-ю профу.
		// Тут ставь свой выбор: рандом из списка 3 проф, или бери из БД active/base class.
		int classId = 0; // TODO: заменить на реальный classId (3 профа)

		int levelToUse = suggestedLevel;

		try (Connection con = DatabaseFactory.getInstance().getConnection())
		{
			// 1) если ты задаёшь конкретный classId (3 профа) — пробуем взять уровень именно этого класса
			int dbLevelForClass = (classId > 0) ? loadLevelForClass(con, objId, classId) : -1;

			if (dbLevelForClass > 0)
			{
				levelToUse = dbLevelForClass; // НЕ затираем прогресс
			}
			else
			{
				// 2) иначе берём active/base уровень персонажа
				int activeOrBase = loadActiveOrBaseLevel(con, objId);
				levelToUse = Math.max(activeOrBase, suggestedLevel);
			}
		}

		Player fantom = Player.restorePhantom(objId, levelToUse, classId, false);
		if (fantom == null)
			return null;

		// allow EXP/leveling
		fantom.unsetVar("NoExp");

		fantom.setOfflineMode(false);
		fantom.setIsOnline(true);
		fantom.updateOnlineStatus();

		// --- brain ---
		PhantomBrain b = new PhantomBrain(objId);
		b.role = rollRole();
		b.homeTown = getRandomLoc();

		int realLevel = fantom.getLevel(); // уже реальный уровень после restore
		if (b.role == PhantomRole.GRIND || b.role == PhantomRole.ROAMER)
		{
			PhantomSpot sp = getRandomSpotForLevel(realLevel);
			if (sp != null)
			{
				b.spot = sp.loc;
				b.spotRadius = sp.radius;
			}
			else
			{
				b.spot = b.homeTown;
				b.spotRadius = 800;
			}
		}

		long now = System.currentTimeMillis();
		b.nextChat = now + Rnd.get(30_000, 120_000);
		b.nextAction = now + Rnd.get(2_000, 7_000);
		b.nextRoleChange = now + Rnd.get(10 * 60_000, 35 * 60_000);

		// лучше ключить по реальному objectId, а не по objId-шнику из БД
		_brains.put(fantom.getObjectId(), b);

		// --- spawn loc ---
		Location spawnLoc;
		if (b.role == PhantomRole.GRIND || b.role == PhantomRole.ROAMER)
		{
			spawnLoc = new Location(
					b.spot.getX() + Rnd.get(-120, 120),
					b.spot.getY() + Rnd.get(-120, 120),
					b.spot.getZ()
			);
		}
		else
		{
			spawnLoc = new Location(
					b.homeTown.getX() + Rnd.get(-80, 80),
					b.homeTown.getY() + Rnd.get(-80, 80),
					b.homeTown.getZ()
			);
		}

		fantom.setPhantomLoc(spawnLoc.getX(), spawnLoc.getY(), spawnLoc.getZ());
		fantom.setXYZ(spawnLoc.getX(), spawnLoc.getY(), spawnLoc.getZ());

		fantom.setOnlineStatus(true);
		fantom.setInvisibleType(InvisibleType.NONE);
		fantom.setNonAggroTime(Long.MAX_VALUE);

		fantom.spawnMe(spawnLoc);
		fantom.setCurrentHpMp(fantom.getMaxHp(), fantom.getMaxMp());
		fantom.setCurrentCp(fantom.getMaxCp());

		// --- Equip ---
		if (Config.ALLOW_PHANTOM_SETS && fantom.getClassId().getRace() != Race.kamael)
		{
			if (body != null)   { fantom.getInventory().addItem(body);   fantom.getInventory().equipItem(body); }
			if (gaiters != null){ fantom.getInventory().addItem(gaiters); fptomEquip(fantom, gaiters); }
			if (gloves != null) { fantom.getInventory().addItem(gloves);  fptomEquip(fantom, gloves); }
			if (boots != null)  { fantom.getInventory().addItem(boots);   fptomEquip(fantom, boots); }
			if (weapon != null) { fantom.getInventory().addItem(weapon);  fantom.getInventory().equipItem(weapon); }
		}
		else
		{
			PlayerTemplate template = fantom.getTemplate();
			for (CreateItem i : template.getItems())
			{
				ItemInstance item = ItemFunctions.createItem(i.getItemId());
				fantom.getInventory().addItem(item);

				if (i.isEquipable() && item.isEquipable()
						&& (fantom.getActiveWeaponItem() == null || item.getTemplate().getType2() != ItemTemplate.TYPE2_WEAPON))
					fantom.getInventory().equipItem(item);
			}
		}

		fantom.broadcastCharInfo();

		if (b.role == PhantomRole.TOWN_TRADE && !fantom.isSitting())
			fantom.sitDown(null);

		startWalk(fantom);
		return fantom;
	}

	private void fptomEquip(Player p, ItemInstance item)
	{
		try { p.getInventory().equipItem(item); } catch (Exception e) {}
	}
}