package core.gameserver.listener.actor.npc;

import core.gameserver.listener.NpcListener;
import core.gameserver.model.instances.NpcInstance;

public interface OnSpawnListener extends NpcListener
{
	public void onSpawn(NpcInstance actor);
}
