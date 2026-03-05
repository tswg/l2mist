package core.gameserver.network.l2.c2s;

import core.gameserver.model.Player;
import core.gameserver.model.entity.olympiad.Olympiad;
import core.gameserver.network.l2.components.SystemMsg;
import core.gameserver.network.l2.s2c.ExReceiveOlympiad;

public class RequestOlympiadMatchList extends L2GameClientPacket
{
	@Override
	protected void readImpl() throws Exception
	{
		// trigger
	}

	@Override
	protected void runImpl() throws Exception
	{
		Player player = getClient().getActiveChar();
		if(player == null)
			return;

		if(!Olympiad.inCompPeriod() || Olympiad.isOlympiadEnd())
		{
			player.sendPacket(SystemMsg.THE_GRAND_OLYMPIAD_GAMES_ARE_NOT_CURRENTLY_IN_PROGRESS);
			return;
		}

		player.sendPacket(new ExReceiveOlympiad.MatchList());
	}
}
