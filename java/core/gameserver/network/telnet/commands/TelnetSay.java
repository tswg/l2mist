package core.gameserver.network.telnet.commands;

import java.util.LinkedHashSet;
import java.util.Set;

import core.gameserver.Announcements;
import core.gameserver.model.Player;
import core.gameserver.model.World;
import core.gameserver.network.l2.components.ChatType;
import core.gameserver.network.l2.s2c.Say2;
import core.gameserver.network.telnet.TelnetCommand;
import core.gameserver.network.telnet.TelnetCommandHolder;


public class TelnetSay implements TelnetCommandHolder
{
	private Set<TelnetCommand> _commands = new LinkedHashSet<TelnetCommand>();

	public TelnetSay()
	{
		_commands.add(new TelnetCommand("announce", "ann")
		{
			@Override
			public String getUsage()
			{
				return "announce <text>";
			}

			@Override
			public String handle(String[] args)
			{
				if(args.length == 0)
					return null;
				
				Announcements.getInstance().announceToAll(args[0]);
				
				return "Announcement sent.\n\r";
			}			
		});
		_commands.add(new TelnetCommand("message", "msg")
		{
			@Override
			public String getUsage()
			{
				return "message <player> <text>";
			}

			@Override
			public String handle(String[] args)
			{
				if(args.length < 2)
					return null;

				Player player = World.getPlayer(args[0]);
				if(player == null)
					return "Player not found.\n\r";

				Say2 cs = new Say2(0, ChatType.TELL, "[Admin]", args[1]);
				player.sendPacket(cs);

				return "Message sent.\n\r";
			}
			
		});
	}

	@Override
	public Set<TelnetCommand> getCommands()
	{
		return _commands;
	}
}