package core.gameserver.utils;

import core.commons.util.Rnd;
import core.gameserver.Config;
import core.gameserver.data.xml.holder.ResidenceHolder;
import core.gameserver.instancemanager.MapRegionManager;
import core.gameserver.instancemanager.ReflectionManager;
import core.gameserver.model.Player;
import core.gameserver.model.base.Race;
import core.gameserver.model.base.RestartType;
import core.gameserver.model.entity.Reflection;
import core.gameserver.model.pledge.Clan;
import core.gameserver.templates.mapregion.RestartArea;
import core.gameserver.templates.mapregion.RestartPoint;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TeleportUtils
{
	private static final Logger _log = LoggerFactory.getLogger(TeleportUtils.class);

	public final static Location DEFAULT_RESTART = new Location(17817, 170079, -3530);
	private final static int minOffset = 10;
	private final static int maxOffset = 150;

	private TeleportUtils()
	{
	}

	public static Location getRestartLocation(Player player, RestartType restartType)
	{
		return getRestartLocation(player, player.getLoc(), restartType);
	}

	public static Location getRestartLocation(Player player, Location from, RestartType restartType)
	{
		Reflection r = player.getReflection();
		if(r != ReflectionManager.DEFAULT)
			if(r.getCoreLoc() != null)
				return r.getCoreLoc();
			else if(r.getReturnLoc() != null)
				return r.getReturnLoc();

		Clan clan = player.getClan();

		if(clan != null)
		{
			// If teleport to clan hall
			if(restartType == RestartType.TO_CLANHALL && clan.getHasHideout() != 0)
				return ResidenceHolder.getInstance().getResidence(clan.getHasHideout()).getOwnerRestartPoint();

			// If teleport to castle
			if(restartType == RestartType.TO_CASTLE && clan.getCastle() != 0)
				return ResidenceHolder.getInstance().getResidence(clan.getCastle()).getOwnerRestartPoint();

			// If teleport to fortress
			if(restartType == RestartType.TO_FORTRESS && clan.getHasFortress() != 0)
				return ResidenceHolder.getInstance().getResidence(clan.getHasFortress()).getOwnerRestartPoint();
		}
		
		if(Config.RVRMODE_ENABLE && restartType == RestartType.TO_VILLAGE)
		{
			if(player.getRace() == Race.human)
				return Location.coordsRandomize(new Location(-82687, 243157, -3734), minOffset, maxOffset); // Говорящий остров
			if(player.getRace() == Race.elf)
				return Location.coordsRandomize(new Location(45873, 49288, -3064), minOffset, maxOffset); // Деревня Светлых Эльфов
			if(player.getRace() == Race.darkelf)
				return Location.coordsRandomize(new Location(12428, 16551, -4588), minOffset, maxOffset); // Деревня Тёмных Эльфов
			if(player.getRace() == Race.orc)
				return Location.coordsRandomize(new Location(-44133, -113911, -244), minOffset, maxOffset); // Деревня Орков
			if(player.getRace() == Race.dwarf)
				return Location.coordsRandomize(new Location(116551, -182493, -1525), minOffset, maxOffset); // Деревня Гномов
			if(player.getRace() == Race.kamael)
				return Location.coordsRandomize(new Location(-118079, 45047, 341), minOffset, maxOffset); // Деревня Камаэль
		}

		if(player.getKarma() > 1)
		{
			if(player.getPKRestartPoint() != null)
				return player.getPKRestartPoint();
		}
		else
		{
			if(player.getRestartPoint() != null)
				return player.getRestartPoint();
		}

		RestartArea ra = MapRegionManager.getInstance().getRegionData(RestartArea.class, from);
		if(ra != null)
		{
			RestartPoint rp = ra.getRestartPoint().get(player.getRace());

			Location restartPoint = Rnd.get(rp.getRestartPoints());
			Location PKrestartPoint = Rnd.get(rp.getPKrestartPoints());

			return player.getKarma() > 1 ? PKrestartPoint : restartPoint;
		}

		_log.warn("Cannot find restart location from coordinates: " + from + "!");

		return DEFAULT_RESTART;
	}
}