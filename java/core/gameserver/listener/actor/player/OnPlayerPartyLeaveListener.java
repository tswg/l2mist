package core.gameserver.listener.actor.player;

import core.gameserver.listener.PlayerListener;
import core.gameserver.model.Player;

public interface OnPlayerPartyLeaveListener extends PlayerListener
{
	public void onPartyLeave(Player player);
}
