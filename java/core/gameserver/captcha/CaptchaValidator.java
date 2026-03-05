package core.gameserver.captcha;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import core.gameserver.Config;
import core.gameserver.model.Player;
import core.gameserver.network.l2.s2c.NpcHtmlMessage;
import core.gameserver.utils.HWID.HWIDComparator;
import core.gameserver.utils.Log;

/**
 * @author bloodshed (L2NextGen)
 * @date 18.07.2010
 * @time 22:39:58
 */
public class CaptchaValidator
{
	private static final Logger _log = LoggerFactory.getLogger(CaptchaValidator.class);

	private static CaptchaValidator instance = null;

	private ICaptcha _captchaEngine;

	/**
	 * @return instance of CaptchaValidator
	 */
	public static CaptchaValidator getInstance()
	{
		if(instance == null)
			instance = new CaptchaValidator();
		return instance;
	}

	/**
	 * cannot be called outside the class
	 */
	private CaptchaValidator()
	{
		if(Config.CAPTCHA_ENABLE)
			try
			{
				if(Config.CAPTCHA_TYPE.equalsIgnoreCase("IMAGE"))
					_captchaEngine = new ImageCaptcha();
				else
					_captchaEngine = new ASCIICaptcha();
			}
			catch(final Exception e)
			{
				_log.error("CaptchaValidator: ", e);
			}
		else
			_captchaEngine = null;
	}

	/**
	 * Used to send next page showed on startup to player
	 * 
	 * @param activeChar
	 *            the player to whom the page is sent
	 */
	private static void sendNextPage(final Player activeChar, final boolean actionPerformed)
	{
		if(Config.SHOW_HTML_WELCOME && !(activeChar.getClan() != null && activeChar.getClan().isNoticeEnabled() && activeChar.getClan().getNotice() != ""))
		{
			final NpcHtmlMessage html = new NpcHtmlMessage(1);
			html.setFile("welcome/index.htm");
			activeChar.sendPacket(html);
		}
		else if(actionPerformed)
		{
			final NpcHtmlMessage html = new NpcHtmlMessage(1);
			html.setFile("common/captcha_successful.htm");
			activeChar.resetCaptchaAtemptRequest();
			activeChar.sendPacket(html);
			activeChar.unblock();
			activeChar.unCaptchaChatBlock();
		}
	}

	/**
	 * Initiates captcha passing procedure for the player
	 * 
	 * @param activeChar
	 *            - the player by which captcha must be passed
	 */
	public void sendCaptcha(final Player activeChar)
	{
		// Проверяем привязку по HWID
		if(activeChar.getNetConnection() != null && activeChar.getNetConnection().protect_used && activeChar.getAllowHWID() != null && Config.LOCK_ACCOUNT_HWID_COMPARATOR.compare(activeChar.getAllowHWID(), activeChar.getHWID()) != HWIDComparator.EQUALS)
		{
			final NpcHtmlMessage html = new NpcHtmlMessage(1);
			html.setFile("command/lock_denied.htm");
			activeChar.block();
			activeChar.sendPacket(html);
			activeChar.startKickTask(5 * 1000);
			Log.add("Attempting enter to locked account: " + activeChar.toFullString() + ", HWID: " + activeChar.getHWID().Full, "protect");
			return;
		}

		if(!Config.CAPTCHA_ENABLE || !Config.CAPTCHA_SHOW_PLAYERS_WITH_PA && activeChar.hasBonus())
		{
			sendNextPage(activeChar, false);
			return;
		}

		_captchaEngine.sendCaptchaPage(activeChar);

		if(activeChar.getSchedulePlayerCaptchaValidation() != null)
			activeChar.resetSchedulePlayerCaptchaValidation();
		activeChar.setSchedulePlayerCaptchaValidation();
	}

	/**
	 * Processes bypass string sent by player as an answer to capctha verification dialog.
	 * 
	 * @param command
	 *            string bypassed by player. Must start with "captcha"
	 * @param player
	 *            the player who sent bypass
	 */
	public static void processCaptchaBypass(final String command, final Player player)
	{
		if(command.length() <= 9)
		{
			Log.add(player.toString() + " write empty captcha", "captcha");
			CaptchakickPlayer(player);
		}
		else
		{
			final String playercaptcha = command.substring(9);
			if(player.getCaptcha().compareTo(playercaptcha) == 0)
			{
				player.setCaptcha("");
				sendNextPage(player, true);
			}
			else
			{
				Log.add(player.toString() + " write '" + playercaptcha + "', need '" + player.getCaptcha() + "'", "captcha");
				CaptchakickPlayer(player);
			}
		}
	}

	/**
	 * @param player
	 *            - disconnect that player from the server
	 */
	public static void CaptchakickPlayer(final Player player)
	{
		player.addCaptchaAtemptRequest();
		if(player.getCaptchaAtemptRequest() < 6)
		{
			if(player.isCaptchaChatBlocked())
				CaptchaValidator.getInstance().sendCaptcha(player);
			else
			{
				player.block();
				player.CaptchaChatBlock();
				CaptchaValidator.getInstance().sendCaptcha(player);
			}
		}
		else
		{
			final NpcHtmlMessage html = new NpcHtmlMessage(1);
			html.setFile("common/captcha_attemptend.htm");
			player.sendPacket(html);
			if(player.getSchedulePlayerCaptchaValidation() != null)
				player.resetSchedulePlayerCaptchaValidation();
		}
	}
}
