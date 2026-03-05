package core.gameserver.listener.zone;

import core.commons.listener.Listener;
import core.gameserver.model.Creature;
import core.gameserver.model.Zone;

public interface OnZoneEnterLeaveListener extends Listener<Zone>
{
	public void onZoneEnter(Zone zone, Creature actor);

	public void onZoneLeave(Zone zone, Creature actor);
}
