package core.gameserver.listener.actor;

import core.gameserver.listener.CharListener;
import core.gameserver.model.Creature;
import core.gameserver.model.Skill;

public interface OnMagicUseListener extends CharListener
{
	public void onMagicUse(Creature actor, Skill skill, Creature target, boolean alt);
}
