package core.gameserver.listener.actor.player;

import core.gameserver.listener.PlayerListener;
import core.gameserver.model.Player;

public interface OnPlayerEnterListener extends PlayerListener
{
	public void onPlayerEnter(Player player);
}
