package core.gameserver.network.l2.c2s;

import core.gameserver.data.xml.holder.HennaHolder;
import core.gameserver.templates.Henna;
import core.gameserver.model.Player;
import core.gameserver.network.l2.s2c.HennaUnequipInfo;

public class RequestHennaUnequipInfo extends L2GameClientPacket
{
	private int _symbolId;

	/**
	 * format: d
	 */
	@Override
	protected void readImpl()
	{
		_symbolId = readD();
	}

	@Override
	protected void runImpl()
	{
		Player player = getClient().getActiveChar();
		if(player == null)
			return;

		Henna henna = HennaHolder.getInstance().getHenna(_symbolId);
		if(henna != null)
			player.sendPacket(new HennaUnequipInfo(henna, player));
	}
}