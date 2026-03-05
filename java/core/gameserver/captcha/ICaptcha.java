package core.gameserver.captcha;

import core.gameserver.model.Player;

/**
 * @author bloodshed (L2NextGen)
 * @date 25.04.2011
 * @time 4:38:56
 */
public interface ICaptcha
{
	public void sendCaptchaPage(final Player activeChar);
}
