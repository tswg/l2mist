package core.gameserver.network.l2.c2s;

import core.gameserver.model.pledge.Clan;
import core.gameserver.model.Player;
import core.gameserver.model.pledge.RankPrivs;
import core.gameserver.network.l2.s2c.PledgePowerGradeList;

public class RequestPledgePowerGradeList extends L2GameClientPacket
{
	@Override
	protected void readImpl()
	{}

	@Override
	protected void runImpl()
	{
		Player activeChar = getClient().getActiveChar();
		if(activeChar == null)
			return;
		Clan clan = activeChar.getClan();
		if(clan != null)
		{
			RankPrivs[] privs = clan.getAllRankPrivs();
			activeChar.sendPacket(new PledgePowerGradeList(privs));
		}
	}
}