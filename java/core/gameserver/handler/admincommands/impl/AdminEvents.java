package core.gameserver.handler.admincommands.impl;

import core.gameserver.handler.admincommands.IAdminCommandHandler;
import core.gameserver.model.Player;
import core.gameserver.network.l2.s2c.NpcHtmlMessage;

public class AdminEvents implements IAdminCommandHandler
{
	private static enum Commands
	{
		admin_events
	}

	@Override
	public boolean useAdminCommand(Enum comm, String[] wordList, String fullString, Player activeChar)
	{
		Commands command = (Commands) comm;

		if(!activeChar.getPlayerAccess().IsEventGm)
			return false;

		switch(command)
		{
			case admin_events:
				if(wordList.length == 1)
					activeChar.sendPacket(new NpcHtmlMessage(5).setFile("admin/events/events.htm"));
				else
					activeChar.sendPacket(new NpcHtmlMessage(5).setFile("admin/events/" + wordList[1].trim()));
				break;
		}

		return true;
	}

	@Override
	public Enum[] getAdminCommandEnum()
	{
		return Commands.values();
	}
}