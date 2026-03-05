package core.gameserver.listener.actor;

import core.gameserver.listener.CharListener;
import core.gameserver.model.Creature;

public interface OnDeathListener extends CharListener
{
	public void onDeath(Creature actor, Creature killer);
}
