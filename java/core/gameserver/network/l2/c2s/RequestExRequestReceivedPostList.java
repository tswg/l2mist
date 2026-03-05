package core.gameserver.network.l2.c2s;

import core.gameserver.model.Player;
import core.gameserver.network.l2.components.SystemMsg;
import core.gameserver.network.l2.s2c.ExNoticePostArrived;
import core.gameserver.network.l2.s2c.ExShowReceivedPostList;

/**
 * Отсылается при нажатии на кнопку "почта", "received mail" или уведомление от {@link ExNoticePostArrived}, запрос входящих писем.
 * В ответ шлется {@link ExShowReceivedPostList}
 */
public class RequestExRequestReceivedPostList extends L2GameClientPacket
{
	@Override
	protected void readImpl()
	{
		//just a trigger
	}

	@Override
	protected void runImpl()
	{
		Player cha = getClient().getActiveChar();
		if(cha != null)
		{
			if(!cha.isInPeaceZone())
			{
				cha.sendPacket(SystemMsg.YOU_CANNOT_RECEIVE_IN_A_NONPEACE_ZONE_LOCATION);
			}
			cha.sendItemList(true);
			cha.sendItemList(false);
			cha.sendPacket(new ExShowReceivedPostList(cha));
		}
		
		
	}
}