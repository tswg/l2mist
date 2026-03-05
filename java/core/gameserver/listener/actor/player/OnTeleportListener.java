package core.gameserver.listener.actor.player;

import core.gameserver.listener.PlayerListener;
import core.gameserver.model.Player;
import core.gameserver.model.entity.Reflection;

public interface OnTeleportListener extends PlayerListener
{
	public void onTeleport(Player player, int x, int y, int z, Reflection reflection);
}
