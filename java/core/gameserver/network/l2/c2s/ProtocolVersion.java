package core.gameserver.network.l2.c2s;

import java.io.IOException;

import core.gameserver.Config;
import core.gameserver.network.l2.GameClient;
import core.gameserver.network.l2.s2c.KeyPacket;
import core.gameserver.network.l2.s2c.SendStatus;

import GameGuard.GameGuard;
import GameGuard.GGConfig;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProtocolVersion extends L2GameClientPacket
{
	private static final Logger _log = LoggerFactory.getLogger(ProtocolVersion.class);

	private int _version;
	private byte[] _check;
	private byte[] _data;
	private String _hwidHdd = "", _hwidMac = "", _hwidCPU = "";

	protected void readImpl()
	{
		GameClient client = getClient();
		_version = readD(); 
		if(_buf.remaining() > 260) 
		{ 
			_data = new byte[260]; 
			readB(_data);
			if(GameGuard.isProtectionOn())
			{
				_hwidHdd = readS();
				_hwidMac = readS();
				_hwidCPU = readS();
			}
		} 
		else if(GameGuard.isProtectionOn())
		{
			client.close(new KeyPacket(null));
		}
	}

	protected void runImpl() throws IOException
	{
		if(_version == -2)
		{
			_client.closeNow(false);
			return;
		}
		else if(_version == -3)
		{
			_log.info("Status request from IP : " + getClient().getIpAddr());
			getClient().close(new SendStatus());
			return;
		}
		else if(_version < Config.MIN_PROTOCOL_REVISION || _version > Config.MAX_PROTOCOL_REVISION)
		{
			_log.warn("Unknown protocol revision : " + _version + ", client : " + _client);
			getClient().close(new KeyPacket(null));
			return;
		}
		getClient().setRevision(_version);
		if(GameGuard.isProtectionOn())
		{
			switch(GGConfig.GET_CLIENT_HWID)
			{
				case 1:
					if(_hwidHdd == "")
					{
						_log.info("Status HWID HDD : NoPatch!!!");
						getClient().close(new KeyPacket(null));
					}
					else
					{
						getClient().setHWID(_hwidHdd);
					}
					break;
				case 2:
					if(_hwidMac == "")
					{
						_log.info("Status HWID MAC : NoPatch!!!");
						getClient().close(new KeyPacket(null));
					}
					else
					{
						getClient().setHWID(_hwidMac);
					}
					break;
				case 3:
					if(_hwidCPU == "")
					{
						_log.info("Status HWID : NoPatch!!!");
						getClient().close(new KeyPacket(null));
					}
					else
					{
						getClient().setHWID(_hwidCPU);
					}
					break;
			}
			if(getClient().checkHWIDBanned())
				getClient().closeNow(false);
		}
		else
			getClient().setHWID("NoGuard");
		sendPacket(new KeyPacket(_client.enableCrypt()));
	}

	public String getType()
	{
		return getClass().getSimpleName();
	}
}