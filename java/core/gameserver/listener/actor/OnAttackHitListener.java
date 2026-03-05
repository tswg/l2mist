package core.gameserver.listener.actor;

import core.gameserver.listener.CharListener;
import core.gameserver.model.Creature;

public interface OnAttackHitListener extends CharListener
{
	public void onAttackHit(Creature actor, Creature attacker);
}
