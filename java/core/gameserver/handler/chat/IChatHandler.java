package core.gameserver.handler.chat;

import core.gameserver.network.l2.components.ChatType;

public interface IChatHandler
{
	void say();

	ChatType getType();
}
