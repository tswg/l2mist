package core.gameserver.model.entity.boat;

import core.gameserver.model.Player;
import core.gameserver.network.l2.s2c.GetOffVehicle;
import core.gameserver.network.l2.s2c.GetOnVehicle;
import core.gameserver.network.l2.s2c.L2GameServerPacket;
import core.gameserver.network.l2.s2c.MoveToLocationInVehicle;
import core.gameserver.network.l2.s2c.StopMove;
import core.gameserver.network.l2.s2c.StopMoveToLocationInVehicle;
import core.gameserver.network.l2.s2c.ValidateLocationInVehicle;
import core.gameserver.network.l2.s2c.VehicleCheckLocation;
import core.gameserver.network.l2.s2c.VehicleDeparture;
import core.gameserver.network.l2.s2c.VehicleInfo;
import core.gameserver.network.l2.s2c.VehicleStart;
import core.gameserver.templates.CharTemplate;
import core.gameserver.utils.Location;

/**
 * @author VISTALL
 * @date  17:46/26.12.2010
 */
public class Vehicle extends Boat
{
	public Vehicle(int objectId, CharTemplate template)
	{
		super(objectId, template);
	}

	@Override
	public L2GameServerPacket startPacket()
	{
		return new VehicleStart(this);
	}

	@Override
	public L2GameServerPacket validateLocationPacket(Player player)
	{
		return new ValidateLocationInVehicle(player);
	}

	@Override
	public L2GameServerPacket checkLocationPacket()
	{
		return new VehicleCheckLocation(this);
	}

	@Override
	public L2GameServerPacket infoPacket()
	{
		return new VehicleInfo(this);
	}

	@Override
	public L2GameServerPacket movePacket()
	{
		return new VehicleDeparture(this);
	}

	@Override
	public L2GameServerPacket inMovePacket(Player player, Location src, Location desc)
	{
		return new MoveToLocationInVehicle(player, this, src, desc);
	}

	@Override
	public L2GameServerPacket stopMovePacket()
	{
		return new StopMove(this);
	}

	@Override
	public L2GameServerPacket inStopMovePacket(Player player)
	{
		return new StopMoveToLocationInVehicle(player);
	}

	@Override
	public L2GameServerPacket getOnPacket(Player player, Location location)
	{
		return new GetOnVehicle(player, this, location);
	}

	@Override
	public L2GameServerPacket getOffPacket(Player player, Location location)
	{
		return new GetOffVehicle(player, this, location);
	}

	@Override
	public void oustPlayers()
	{
		//
	}

	@Override
	public boolean isVehicle()
	{
		return true;
	}
}
