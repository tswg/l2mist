package core.loginserver.network.gameservercon.lspackets;

import core.loginserver.network.gameservercon.SendablePacket;

public class PingRequest extends SendablePacket
{
	@Override
	protected void writeImpl()
	{
		writeC(0xff);
	}
}