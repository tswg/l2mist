package core.gameserver.model.entity.events.objects;

import core.gameserver.model.entity.events.GlobalEvent;
import core.gameserver.model.instances.NpcInstance;
import core.gameserver.utils.Location;
import core.gameserver.utils.NpcUtils;

public class SpawnSimpleObject implements SpawnableObject
{
	private int _npcId;
	private Location _loc;

	private NpcInstance _npc;

	public SpawnSimpleObject(int npcId, Location loc)
	{
		_npcId = npcId;
		_loc = loc;
	}

	@Override
	public void spawnObject(GlobalEvent event)
	{
		_npc = NpcUtils.spawnSingle(_npcId, _loc, event.getReflection());
		_npc.addEvent(event);
	}

	@Override
	public void despawnObject(GlobalEvent event)
	{
		_npc.removeEvent(event);
		_npc.deleteMe();
	}

	@Override
	public void refreshObject(GlobalEvent event)
	{

	}
}
