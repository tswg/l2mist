package core.gameserver.listener.actor.player;

import core.gameserver.listener.PlayerListener;
import core.gameserver.model.Player;

public interface OnPlayerPartyInviteListener extends PlayerListener
{
	public void onPartyInvite(Player player);
}
