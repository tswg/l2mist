package core.loginserver.network.gameservercon.lspackets;

import core.loginserver.network.gameservercon.SendablePacket;

public class KickPlayer extends SendablePacket
{
	private String account;
	
	public KickPlayer(String login)
	{
		this.account = login;
	}

	@Override
	protected void writeImpl()
	{
		writeC(0x03);
		writeS(account);
	}
}