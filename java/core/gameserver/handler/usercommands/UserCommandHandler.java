package core.gameserver.handler.usercommands;

import gnu.trove.map.hash.TIntObjectHashMap;

import core.commons.data.xml.AbstractHolder;
import core.gameserver.handler.usercommands.impl.ClanPenalty;
import core.gameserver.handler.usercommands.impl.ClanWarsList;
import core.gameserver.handler.usercommands.impl.CommandChannel;
import core.gameserver.handler.usercommands.impl.Escape;
import core.gameserver.handler.usercommands.impl.InstanceZone;
import core.gameserver.handler.usercommands.impl.Loc;
import core.gameserver.handler.usercommands.impl.MyBirthday;
import core.gameserver.handler.usercommands.impl.OlympiadStat;
import core.gameserver.handler.usercommands.impl.PartyInfo;
import core.gameserver.handler.usercommands.impl.SiegeStatus;
import core.gameserver.handler.usercommands.impl.Time;

public class UserCommandHandler extends AbstractHolder
{
	private static final UserCommandHandler _instance = new UserCommandHandler();

	public static UserCommandHandler getInstance()
	{
		return _instance;
	}

	private TIntObjectHashMap<IUserCommandHandler> _datatable = new TIntObjectHashMap<IUserCommandHandler>();

	private UserCommandHandler()
	{
		registerUserCommandHandler(new ClanWarsList());
		registerUserCommandHandler(new ClanPenalty());
		registerUserCommandHandler(new CommandChannel());
		registerUserCommandHandler(new Escape());
		registerUserCommandHandler(new Loc());
		registerUserCommandHandler(new MyBirthday());
		registerUserCommandHandler(new OlympiadStat());
		registerUserCommandHandler(new PartyInfo());
		registerUserCommandHandler(new SiegeStatus());
		registerUserCommandHandler(new InstanceZone());
		registerUserCommandHandler(new Time());
	}

	public final void registerUserCommandHandler(IUserCommandHandler handler)
	{
		int[] ids = handler.getUserCommandList();
		for(int element : ids)
			_datatable.put(element, handler);
	}

	public IUserCommandHandler getUserCommandHandler(int userCommand)
	{
		return _datatable.get(userCommand);
	}

	@Override
	public int size()
	{
		return _datatable.size();
	}

	@Override
	public void clear()
	{
		_datatable.clear();
	}
}
