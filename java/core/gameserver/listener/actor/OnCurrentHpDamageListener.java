package core.gameserver.listener.actor;

import core.gameserver.listener.CharListener;
import core.gameserver.model.Creature;
import core.gameserver.model.Skill;

public interface OnCurrentHpDamageListener extends CharListener
{
	public void onCurrentHpDamage(Creature actor, double damage, Creature attacker, Skill skill);
}
