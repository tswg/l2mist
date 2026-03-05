package core.gameserver.listener.actor.player;

import core.gameserver.listener.PlayerListener;
import core.gameserver.model.Player;

public interface OnPlayerExitListener extends PlayerListener
{
	public void onPlayerExit(Player player);
}
