package core.gameserver.listener.zone.impl;

import core.gameserver.listener.zone.OnZoneEnterLeaveListener;
import core.gameserver.model.Creature;
import core.gameserver.model.Zone;
import core.gameserver.model.entity.boat.ClanAirShip;
import core.gameserver.model.instances.ClanAirShipControllerInstance;

public class AirshipControllerZoneListener implements OnZoneEnterLeaveListener
{
	private ClanAirShipControllerInstance _controllerInstance;

	@Override
	public void onZoneEnter(Zone zone, Creature actor)
	{
		if(_controllerInstance == null && actor instanceof ClanAirShipControllerInstance)
			_controllerInstance = (ClanAirShipControllerInstance) actor;
		else if(actor.isClanAirShip())
			_controllerInstance.setDockedShip((ClanAirShip) actor);
	}

	@Override
	public void onZoneLeave(Zone zone, Creature actor)
	{
		if(actor.isClanAirShip())
			_controllerInstance.setDockedShip(null);
	}
}
