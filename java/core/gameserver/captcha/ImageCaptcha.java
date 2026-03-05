package core.gameserver.captcha;

import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Random;
import javax.imageio.ImageIO;

import core.gameserver.Config;
import core.gameserver.model.Player;
import core.gameserver.network.l2.s2c.NpcHtmlMessage;
import core.gameserver.network.l2.s2c.PledgeCrest;
import core.commons.util.Rnd;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author bloodshed (L2NextGen)
 * @date 25.04.2011
 * @time 4:08:02
 */
public class ImageCaptcha implements ICaptcha
{
	private static final Logger _log = LoggerFactory.getLogger(ImageCaptcha.class);
	private static final String chars = "1234567890";
	private static final String[] words = Config.CAPTCHA_IMAGE_WORDS;

	public ImageCaptcha()
	{
		_log.info("ImageCaptcha: loaded.");
	}

	@Override
	public void sendCaptchaPage(Player activeChar)
	{
		final int imgId = Rnd.get(1000000);
		final String html = "Crest.crest_1_" + imgId;
		try
		{
			String captcha = generateRandomString(4);
			ByteArrayOutputStream bts = new ByteArrayOutputStream();

			ImageIO.write(generateCaptcha(captcha), "png", bts);

			byte[] buffer = bts.toByteArray();
			ByteInputStream bis = new ByteInputStream(buffer, 0, buffer.length);
			PledgeCrest packet = new PledgeCrest(imgId, DDSConverter.convertToDDS(bis).array());
			activeChar.sendPacket(packet);

			NpcHtmlMessage captchapage = new NpcHtmlMessage(0);
			captchapage.setFile("common/captcha_image.htm");
			captchapage.replace("%captchatime%", Config.CAPTCHA_TIME + "");
			captchapage.replace("%captcha%", html);
			activeChar.sendPacket(captchapage);
			activeChar.setCaptcha(captcha);
		}
		catch(Exception e)
		{
			_log.error("", e);
		}
	}

	private String generateRandomString(int length)
	{
		Random random = new Random();
		String rndString = "";
		for(int i = 0; i < length; i++)
			rndString += chars.charAt(random.nextInt(chars.length()));
		return rndString;
	}

	private BufferedImage generateCaptcha(String randomString)
	{
		char[] charString = randomString.toCharArray();
		final int width = 256;
		final int height = 64;
		final BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		final Graphics2D g2d = bufferedImage.createGraphics();
		final Font font = new Font("verdana", Font.BOLD, 36);
		final Font fontSmall = new Font("Arial", Font.PLAIN, 10);
		final Font fontUpper = new Font("verdana", Font.BOLD | Font.ITALIC, 36);
		RenderingHints renderingHints = new RenderingHints(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		renderingHints.put(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		g2d.setRenderingHints(renderingHints);
		g2d.setFont(font);
		g2d.setColor(new Color(255, 255, 0));
		final GradientPaint gradientPaint = new GradientPaint(0, 0, Color.black, 0, height / 2, Color.black, true);
		g2d.setPaint(gradientPaint);
		g2d.fillRect(0, 0, width, height);
		g2d.setColor(new Color(255, 153, 0));
		int xCordinate = 0;
		int yCordinate = 0;
		for(int i = 0; i < charString.length; i++)
		{
			xCordinate = 55 * i + Rnd.get(25) + 10;
			if(xCordinate >= width - 5)
				xCordinate = 0;
			yCordinate = 30 + Rnd.get(34);
			if(Character.isDigit(charString[i]) || Character.isLowerCase(charString[i]))
				g2d.setFont(font);
			else
				g2d.setFont(fontUpper);

			g2d.drawChars(charString, i, 1, xCordinate, yCordinate);
		}
		for(int i = 0; i < 10; i++)
		{
			g2d.setColor(new Color(Rnd.get(255), Rnd.get(255), Rnd.get(255)));
			g2d.drawLine(Rnd.get(width), Rnd.get(height), Rnd.get(width), Rnd.get(height));
		}

		for(int i = 0; i < words.length * 2; i++)
		{
			g2d.setFont(fontSmall);
			xCordinate = 55 * i + Rnd.get(25) + 10;
			if(xCordinate >= width - 5)
				xCordinate = 0;
			yCordinate = 30 + Rnd.get(34);
			g2d.setColor(new Color(Rnd.get(255), Rnd.get(255), Rnd.get(255)));
			g2d.drawString(words[Rnd.get(words.length)], xCordinate, yCordinate);
		}

		g2d.dispose();
		return bufferedImage;
	}
}
