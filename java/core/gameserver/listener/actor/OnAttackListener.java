package core.gameserver.listener.actor;

import core.gameserver.listener.CharListener;
import core.gameserver.model.Creature;

public interface OnAttackListener extends CharListener
{
	public void onAttack(Creature actor, Creature target);
}
