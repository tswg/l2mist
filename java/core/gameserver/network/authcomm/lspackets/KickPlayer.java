package core.gameserver.network.authcomm.lspackets;

import core.gameserver.cache.Msg;
import core.gameserver.model.Player;
import core.gameserver.network.authcomm.AuthServerCommunication;
import core.gameserver.network.authcomm.ReceivablePacket;
import core.gameserver.network.l2.GameClient;
import core.gameserver.network.l2.s2c.ServerClose;

public class KickPlayer extends ReceivablePacket
{
	String account;

	@Override
	public void readImpl()
	{
		account = readS();
	}

	@Override
	protected void runImpl()
	{
		GameClient client = AuthServerCommunication.getInstance().removeWaitingClient(account);
		if(client == null)		
			client = AuthServerCommunication.getInstance().removeAuthedClient(account);
		if(client == null)
			return;

		Player activeChar = client.getActiveChar();
		if(activeChar != null)
		{
			//FIXME [G1ta0] сообщение чаще всего не показывается, т.к. при закрытии соединения очередь на отправку очищается
			activeChar.sendPacket(Msg.ANOTHER_PERSON_HAS_LOGGED_IN_WITH_THE_SAME_ACCOUNT);
			activeChar.kick();
		}
		else
		{
			client.close(ServerClose.STATIC);
		}
	}
}