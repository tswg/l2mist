package core.gameserver.handler.voicecommands.impl;

import core.gameserver.data.htm.HtmCache;
import core.gameserver.handler.voicecommands.IVoicedCommandHandler;
import core.gameserver.model.Player;
import core.gameserver.model.World;
import core.gameserver.model.base.Experience;
import core.gameserver.network.l2.components.CustomMessage;
import core.gameserver.network.l2.s2c.RadarControl;
import core.gameserver.scripts.Functions;

/**
 * @Author: Abaddon
 */
public class Help extends Functions implements IVoicedCommandHandler
{
	private String[] _commandList = new String[] { "help", "exp", "whereis" };

	@Override
	public boolean useVoicedCommand(String command, Player activeChar, String args)
	{
		command = command.intern();
		if(command.equalsIgnoreCase("help"))
			return help(command, activeChar, args);
		if(command.equalsIgnoreCase("whereis"))
			return whereis(command, activeChar, args);
		if(command.equalsIgnoreCase("exp"))
			return exp(command, activeChar, args);

		return false;
	}

	private boolean exp(String command, Player activeChar, String args)
	{
		if(activeChar.getLevel() >= (activeChar.isSubClassActive() ? Experience.getMaxSubLevel() : Experience.getMaxLevel()))
			activeChar.sendMessage(new CustomMessage("voicedcommandhandlers.Help.MaxLevel", activeChar));
		else
		{
			long exp = Experience.LEVEL[activeChar.getLevel() + 1] - activeChar.getExp();
			activeChar.sendMessage(new CustomMessage("voicedcommandhandlers.Help.ExpLeft", activeChar).addNumber(exp));
		}
		return true;
	}

	private boolean whereis(String command, Player activeChar, String args)
	{
		Player friend = World.getPlayer(args);
		if(friend == null)
			return false;

		if(friend.getParty() == activeChar.getParty() || friend.getClan() == activeChar.getClan())
		{
			RadarControl rc = new RadarControl(0, 1, friend.getLoc());
			activeChar.sendPacket(rc);
			return true;
		}

		return false;
	}

	private boolean help(String command, Player activeChar, String args)
	{
		String dialog = HtmCache.getInstance().getNotNull("command/help.htm", activeChar);
		show(dialog, activeChar);
		return true;
	}

	@Override
	public String[] getVoicedCommandList()
	{
		return _commandList;
	}
}