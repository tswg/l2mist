package core.gameserver.model.phantom;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import core.commons.util.Rnd;
import core.gameserver.Config;
import core.gameserver.database.DatabaseFactory;
import core.gameserver.model.GameObjectsStorage;
import core.gameserver.model.Player;
import core.gameserver.model.base.InvisibleType;
import core.gameserver.utils.Location;

public class PhantomSpawnerService
{
	private final PhantomRegistry registry;
	private final PhantomSpotService spotService;
	private final PhantomEquipService equipService;

	public PhantomSpawnerService(PhantomRegistry registry, PhantomSpotService spotService, PhantomEquipService equipService)
	{
		this.registry = registry;
		this.spotService = spotService;
		this.equipService = equipService;
	}

	public void loadProfiles(String account)
	{
		Connection con = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			PreparedStatement st = con.prepareStatement("SELECT obj_Id,char_name,title,x,y,z,clanid FROM characters WHERE account_name = ?");
			st.setString(1, account);
			ResultSet rs = st.executeQuery();
			while(rs.next())
				registry.registerProfile(new PhantomProfile(rs.getInt("obj_Id"), rs.getString("char_name"), rs.getString("title"), rs.getInt("x"), rs.getInt("y"), rs.getInt("z"), rs.getInt("clanid")));
			rs.close();
			st.close();
		}
		catch(Exception e)
		{
		}
		finally
		{
			if(con != null)
				con = null;
		}
	}

	public Player spawn(int objectId)
	{
		if(GameObjectsStorage.getAllPlayersCount() >= Config.MAXIMUM_ONLINE_USERS)
			return null;
		PhantomProfile profile = registry.getProfile(objectId);
		if(profile == null)
			return null;
		PhantomSet set = equipService.randomSet();
		int setLevel = equipService.randomLevelByGrade(set);
		Player phantom = Player.restorePhantom(objectId, setLevel, 0, false);
		if(phantom == null)
			return null;

		phantom.setOfflineMode(false);
		phantom.setIsOnline(true);
		phantom.updateOnlineStatus();
		Location baseLoc = spotService.randomSpot();
		if(baseLoc == null)
			baseLoc = profile.getOrigin();
		phantom.setPhantomLoc(baseLoc.getX(), baseLoc.getY(), baseLoc.getZ());
		Location spawn = new Location(baseLoc.getX() + Rnd.get(150), baseLoc.getY() + Rnd.get(150), baseLoc.getZ());
		phantom.setXYZ(spawn.getX(), spawn.getY(), spawn.getZ());
		phantom.setOnlineStatus(true);
		phantom.setInvisibleType(InvisibleType.NONE);
		phantom.setNonAggroTime(Long.MAX_VALUE);
		phantom.spawnMe(spawn);
		phantom.setCurrentHpMp(phantom.getMaxHp(), phantom.getMaxMp());
		phantom.setCurrentCp(phantom.getMaxCp());
		equipService.equip(phantom, set);
		phantom.broadcastCharInfo();
		return phantom;
	}
}
