package core.gameserver.network.authcomm.lspackets;


import core.gameserver.network.authcomm.AuthServerCommunication;
import core.gameserver.network.authcomm.ReceivablePacket;
import core.gameserver.network.authcomm.gspackets.OnlineStatus;
import core.gameserver.network.authcomm.gspackets.PlayerInGame;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuthResponse extends ReceivablePacket
{
	private static final Logger _log = LoggerFactory.getLogger(AuthResponse.class);

	private int _serverId;
	private String _serverName;

	@Override
	protected void readImpl()
	{
		_serverId = readC();
		_serverName = readS();
	}

	@Override
	protected void runImpl()
	{
		_log.info("Registered on authserver as " + _serverId + " [" + _serverName + "]");

		sendPacket(new OnlineStatus(true));

		String[] accounts = AuthServerCommunication.getInstance().getAccounts();
		for(String account : accounts)
			sendPacket(new PlayerInGame(account));
	}
}
