package core.gameserver.model.phantom;

import core.gameserver.model.Player;
import core.gameserver.model.base.Race;

public class PhantomRoleResolver
{
	public enum Role
	{
		FIGHTER,
		MAGE
	}

	public Role resolve(Player player)
	{
		if(player.getClassId().isMage() && player.getClassId().getRace() != Race.orc)
			return Role.MAGE;
		return Role.FIGHTER;
	}
}
