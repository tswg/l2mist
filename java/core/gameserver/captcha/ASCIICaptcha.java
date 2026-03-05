package core.gameserver.captcha;

import java.io.File;
import java.util.StringTokenizer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import core.gameserver.Config;
import core.gameserver.model.Player;
import core.gameserver.network.l2.s2c.NpcHtmlMessage;
import core.commons.util.Rnd;
import de.jave.figlet.Figlet;

/**
 * @author bloodshed (L2NextGen)
 * @date 19.07.2010
 * @time 0:26:18
 */
public class ASCIICaptcha implements ICaptcha
{
	private static final Logger _log = LoggerFactory.getLogger(ASCIICaptcha.class);

	private final File dir = new File(Config.DATAPACK_ROOT, "data/fonts");
	private final Figlet figlet;
	private final String[] fonts;

	public ASCIICaptcha() throws Exception
	{
		figlet = new Figlet(dir);
		fonts = figlet.getFileLibrary().getAllFontNames();

		_log.info("ASCIICaptcha: loaded " + fonts.length + " fonts.");
	}

	/**
	 * Initiates captcha passing procedure for the player
	 * 
	 * @param activeChar
	 *            - the player by which captcha must be passed
	 */
	@Override
	public void sendCaptchaPage(final Player activeChar)
	{
		NpcHtmlMessage captchapage = new NpcHtmlMessage(0);
		captchapage.setFile("common/captcha_ascii.htm");
		captchapage.replace("%captchatime%", Config.CAPTCHA_TIME + "");

		String cap1 = String.valueOf(Rnd.get(10));
		String captcha1 = stringToRandomASCII(cap1);

		String cap2 = String.valueOf(Rnd.get(10));
		String captcha2 = stringToRandomASCII(cap2);

		captchapage.replace("%captcha1%", captcha1);
		captchapage.replace("%captcha2%", captcha2);
		activeChar.sendPacket(captchapage);
		String captcha = cap1 + cap2;
		activeChar.setCaptcha(captcha);
	}

	public String stringToRandomASCII(String source)
	{
		// выбираем шрифт
		String fontName = fonts[Rnd.get(fonts.length)];
		// делаем ASCII рисунок
		String result = null;
		try
		{
			result = figlet.figletize(source, fontName);
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		result = result.replace("\n\n", "");
		StringTokenizer st = new StringTokenizer(result, "\n", false);
		String result2 = "<table border=0 cellspacing=0 cellpadding=0>\n";
		while (st.hasMoreTokens())
		{
			result2 += " <tr>";
			String row = st.nextToken();
			for(int i = 0; i < row.length(); i++)
			{
				result2 += "<td>" + row.substring(i, i + 1) + "</td>";
			}
			result2 += "</tr>\n";

		}
		result2 += "</table>";
		return result2;
	}
}
