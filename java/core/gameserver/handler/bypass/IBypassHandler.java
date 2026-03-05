package core.gameserver.handler.bypass;

import core.gameserver.model.Player;
import core.gameserver.model.instances.NpcInstance;

public interface IBypassHandler
{
	String[] getBypasses();

	void onBypassFeedback(NpcInstance npc, Player player, String command);
}
