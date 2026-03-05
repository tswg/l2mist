package core.loginserver.network;

import java.nio.channels.SocketChannel;

import core.loginserver.Config;
import core.loginserver.IpBanManager;
import core.loginserver.ThreadPoolManager;
import core.loginserver.network.serverpackets.Init;
import core.commons.net.nio.impl.IAcceptFilter;
import core.commons.net.nio.impl.IClientFactory;
import core.commons.net.nio.impl.IMMOExecutor;
import core.commons.net.nio.impl.MMOConnection;
import core.commons.threading.RunnableImpl;


public class SelectorHelper implements IMMOExecutor<L2LoginClient>, IClientFactory<L2LoginClient>, IAcceptFilter
{
	@Override
	public void execute(Runnable r)
	{
		ThreadPoolManager.getInstance().execute(r);
	}

	@Override
	public L2LoginClient create(MMOConnection<L2LoginClient> con)
	{
		final L2LoginClient client = new L2LoginClient(con);
		client.sendPacket(new Init(client));
		ThreadPoolManager.getInstance().schedule(new RunnableImpl()
		{
			@Override
			public void runImpl()
			{
				client.closeNow(false);
			}
		}, Config.LOGIN_TIMEOUT);
		return client;
	}

	@Override
	public boolean accept(SocketChannel sc)
	{
		return !IpBanManager.getInstance().isIpBanned(sc.socket().getInetAddress().getHostAddress());
	}
}