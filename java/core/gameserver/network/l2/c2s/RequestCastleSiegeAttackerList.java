package core.gameserver.network.l2.c2s;

import core.gameserver.data.xml.holder.ResidenceHolder;
import core.gameserver.model.Player;
import core.gameserver.model.entity.residence.Residence;
import core.gameserver.network.l2.s2c.CastleSiegeAttackerList;

public class RequestCastleSiegeAttackerList extends L2GameClientPacket
{
	private int _unitId;

	@Override
	protected void readImpl()
	{
		_unitId = readD();
	}

	@Override
	protected void runImpl()
	{
		Player player = getClient().getActiveChar();
		if(player == null)
			return;

		Residence residence = ResidenceHolder.getInstance().getResidence(_unitId);
		if(residence != null)
			sendPacket(new CastleSiegeAttackerList(residence));
	}
}