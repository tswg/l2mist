package core.gameserver.model.phantom;

import core.gameserver.model.Player;

public class PhantomRespawnService
{
	public boolean ensureAliveState(Player phantom)
	{
		if(phantom.isDead())
		{
			phantom.kick();
			return false;
		}
		if(phantom.isInWater())
			phantom.teleToClosestTown();
		return true;
	}
}
