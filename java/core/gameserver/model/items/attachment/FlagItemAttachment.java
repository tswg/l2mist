package core.gameserver.model.items.attachment;

import core.gameserver.model.Creature;
import core.gameserver.model.Player;
import core.gameserver.model.Skill;

public interface FlagItemAttachment extends PickableAttachment
{
	//FIXME [VISTALL] возможно переделать на слушатели игрока
	void onLogout(Player player);
	//FIXME [VISTALL] возможно переделать на слушатели игрока
	void onDeath(Player owner, Creature killer);

	void onOutTerritory(Player player);

	boolean canAttack(Player player);

	boolean canCast(Player player, Skill skill);
}
