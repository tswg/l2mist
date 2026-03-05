package core.loginserver.network.clientpackets;

import javax.crypto.Cipher;

import core.loginserver.Config;
import core.loginserver.GameServerManager;
import core.loginserver.IpBanManager;
import core.loginserver.accounts.Account;
import core.loginserver.accounts.SessionManager;
import core.loginserver.accounts.SessionManager.Session;
import core.loginserver.crypt.PasswordHash;
import core.loginserver.network.L2LoginClient;
import core.loginserver.network.L2LoginClient.LoginClientState;
import core.loginserver.network.gameservercon.GameServer;
import core.loginserver.network.gameservercon.lspackets.GetAccountInfo;
import core.loginserver.network.serverpackets.LoginOk;
import core.loginserver.network.serverpackets.LoginFail.LoginFailReason;
import core.loginserver.utils.Log;

/**
 * Format: b[128]ddddddhc
 * b[128]: the rsa encrypted block with the login an password
 */
public class RequestAuthLogin extends L2LoginClientPacket
{
	private byte[] _raw = new byte[128];

	@Override
	protected void readImpl()
	{
		readB(_raw);
		readD();
		readD();
		readD();
		readD();
		readD();
		readD();
		readH();
		readC();
	}

	@Override
	protected void runImpl() throws Exception
	{
		L2LoginClient client = getClient();

		byte[] decrypted;
		try
		{
			Cipher rsaCipher = Cipher.getInstance("RSA/ECB/nopadding");
			rsaCipher.init(Cipher.DECRYPT_MODE, client.getRSAPrivateKey());
			decrypted = rsaCipher.doFinal(_raw, 0x00, 0x80);
		}
		catch(Exception e)
		{
			client.closeNow(true);
			return;
		}

		String user = new String(decrypted, 0x5E, 14).trim();
		user = user.toLowerCase();
		String password = new String(decrypted, 0x6C, 16).trim();
		int ncotp = decrypted[0x7c];
		ncotp |= decrypted[0x7d] << 8;
		ncotp |= decrypted[0x7e] << 16;
		ncotp |= decrypted[0x7f] << 24;

		int currentTime = (int) (System.currentTimeMillis() / 1000L);

		Account account = new Account(user);
		account.restore();

		String passwordHash = Config.DEFAULT_CRYPT.encrypt(password);

		if(account.getPasswordHash() == null)
			if(Config.AUTO_CREATE_ACCOUNTS && user.matches(Config.ANAME_TEMPLATE) && password.matches(Config.APASSWD_TEMPLATE))
			{
				account.setAllowedIP("");
				account.setAllowedHwid("");
				account.setPasswordHash(passwordHash);
				account.save();
			}
			else
			{
				client.close(LoginFailReason.REASON_USER_OR_PASS_WRONG);
				return;
			}

		boolean passwordCorrect = account.getPasswordHash().equals(passwordHash);

		if(!passwordCorrect)
		{
			// проверяем не зашифрован ли пароль одним из устаревших но поддерживаемых алгоритмов
			for(PasswordHash c : Config.LEGACY_CRYPT)
				if(c.compare(password, account.getPasswordHash()))
				{
					passwordCorrect = true;
					account.setPasswordHash(passwordHash);
					break;
				}
		}

		if(!IpBanManager.getInstance().tryLogin(client.getIpAddress(), passwordCorrect))
		{
			client.closeNow(false);
			return;
		}

		if(!passwordCorrect)
		{
			if(!Config.FAKE_LOGIN_SERVER)
			{
				client.close(LoginFailReason.REASON_USER_OR_PASS_WRONG);
				return;
			}
			
		}

		if(account.getAccessLevel() < 0)
		{
			client.close(LoginFailReason.REASON_ACCESS_FAILED);
			return;
		}

		if(account.getBanExpire() > currentTime)
		{
			client.close(LoginFailReason.REASON_ACCESS_FAILED);
			return;
		}

		if(!account.isAllowedIP(client.getIpAddress()))
		{
			client.close(LoginFailReason.REASON_ATTEMPTED_RESTRICTED_IP);
			return;
		}

		for(GameServer gs : GameServerManager.getInstance().getGameServers())
			if(gs.getProtocol() >= 2 && gs.isAuthed())
				gs.sendPacket(new GetAccountInfo(user));

		account.setLastAccess(currentTime);
		account.setLastIP(client.getIpAddress());

		Log.LogAccount(account);
		
		Session session = SessionManager.getInstance().openSession(account);

		client.setAuthed(true);
		client.setLogin(user);
		client.setAccount(account);
		client.setSessionKey(session.getSessionKey());
		
		if(Config.FAKE_LOGIN_SERVER && !passwordCorrect)
			client.setState(LoginClientState.FAKE_LOGIN);
		else
			client.setState(LoginClientState.AUTHED);

		client.sendPacket(new LoginOk(client.getSessionKey()));
	}
}