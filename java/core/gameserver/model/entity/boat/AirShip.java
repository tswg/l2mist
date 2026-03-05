package core.gameserver.model.entity.boat;

import core.gameserver.model.Player;
import core.gameserver.network.l2.s2c.ExAirShipInfo;
import core.gameserver.network.l2.s2c.ExGetOffAirShip;
import core.gameserver.network.l2.s2c.ExGetOnAirShip;
import core.gameserver.network.l2.s2c.ExMoveToLocationAirShip;
import core.gameserver.network.l2.s2c.ExMoveToLocationInAirShip;
import core.gameserver.network.l2.s2c.ExStopMoveAirShip;
import core.gameserver.network.l2.s2c.ExStopMoveInAirShip;
import core.gameserver.network.l2.s2c.ExValidateLocationInAirShip;
import core.gameserver.network.l2.s2c.L2GameServerPacket;
import core.gameserver.templates.CharTemplate;
import core.gameserver.utils.Location;

/**
 * @author VISTALL
 * @date  17:45/26.12.2010
 */
public class AirShip extends Boat
{
	public AirShip(int objectId, CharTemplate template)
	{
		super(objectId, template);
	}

	@Override
	public L2GameServerPacket infoPacket()
	{
		return new ExAirShipInfo(this);
	}

	@Override
	public L2GameServerPacket movePacket()
	{
		return new ExMoveToLocationAirShip(this);
	}

	@Override
	public L2GameServerPacket inMovePacket(Player player, Location src, Location desc)
	{
		return new ExMoveToLocationInAirShip(player,  this, src,desc);
	}

	@Override
	public L2GameServerPacket stopMovePacket()
	{
		return new ExStopMoveAirShip(this);
	}

	@Override
	public L2GameServerPacket inStopMovePacket(Player player)
	{
		return new ExStopMoveInAirShip(player);
	}

	@Override
	public L2GameServerPacket startPacket()
	{
		return null;
	}

	@Override
	public L2GameServerPacket checkLocationPacket()
	{
		return null;
	}

	@Override
	public L2GameServerPacket validateLocationPacket(Player player)
	{
		return new ExValidateLocationInAirShip(player);
	}

	@Override
	public L2GameServerPacket getOnPacket(Player player, Location location)
	{
		return new ExGetOnAirShip(player, this, location);
	}

	@Override
	public L2GameServerPacket getOffPacket(Player player, Location location)
	{
		return new ExGetOffAirShip(player, this, location);
	}

	@Override
	public boolean isAirShip()
	{
		return true;
	}

	@Override
	public void oustPlayers()
	{
		for(Player player : _players)
		{
			oustPlayer(player, getReturnLoc(), true);
		}
	}
}
