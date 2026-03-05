package core.gameserver.network.l2.c2s;

import core.gameserver.Config;
import core.gameserver.model.Player;
import core.gameserver.network.l2.components.CustomMessage;
import core.gameserver.network.l2.s2c.ExReplyPostItemList;
import core.gameserver.network.l2.s2c.ExShowReceivedPostList;

/**
 *  Нажатие на кнопку "send mail" в списке из {@link ExShowReceivedPostList}, запрос создания нового письма
 *  В ответ шлется {@link ExReplyPostItemList}
 */
public class RequestExPostItemList extends L2GameClientPacket
{
	@Override
	protected void readImpl()
	{
		//just a trigger
	}

	@Override
	protected void runImpl()
	{
		Player activeChar = getClient().getActiveChar();
		if(activeChar == null)
			return;

		if(activeChar.isActionsDisabled())
			activeChar.sendActionFailed();

		if(!Config.ALLOW_MAIL)
		{
			activeChar.sendMessage(new CustomMessage("mail.Disabled", activeChar));
			activeChar.sendActionFailed();
			return;
		}

		activeChar.sendPacket(new ExReplyPostItemList(activeChar));
	}
}