package core.gameserver.listener.actor.npc;

import core.gameserver.listener.NpcListener;
import core.gameserver.model.instances.NpcInstance;

public interface OnDecayListener extends NpcListener
{
	public void onDecay(NpcInstance actor);
}
