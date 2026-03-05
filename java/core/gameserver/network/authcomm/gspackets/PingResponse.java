package core.gameserver.network.authcomm.gspackets;

import core.gameserver.network.authcomm.SendablePacket;

public class PingResponse extends SendablePacket
{
    @Override
	protected void writeImpl()
	{
		writeC(0xff);
		writeQ(System.currentTimeMillis());
	}
}