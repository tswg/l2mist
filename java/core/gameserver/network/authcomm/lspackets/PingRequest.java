package core.gameserver.network.authcomm.lspackets;

import core.gameserver.network.authcomm.AuthServerCommunication;
import core.gameserver.network.authcomm.ReceivablePacket;
import core.gameserver.network.authcomm.gspackets.PingResponse;

public class PingRequest extends ReceivablePacket
{
	@Override
	public void readImpl()
	{
		
	}

	@Override
	protected void runImpl()
	{
		AuthServerCommunication.getInstance().sendPacket(new PingResponse());
	}
}