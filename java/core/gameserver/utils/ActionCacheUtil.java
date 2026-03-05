package core.gameserver.utils;

import java.util.Locale;

import core.gameserver.enums.HtmlActionScope;
import core.gameserver.model.Player;

public class ActionCacheUtil
{
	public static final char VAR_PARAM_START_CHAR = '$';
	
	private static final void buildHtmlBypassCache(Player player, HtmlActionScope scope, String html)
	{
		String htmlLower = html.toLowerCase(Locale.ENGLISH);
		int bypassEnd = 0;
		int bypassStart = htmlLower.indexOf("=\"bypass ", bypassEnd);
		int bypassStartEnd;
		while (bypassStart != -1)
		{
			bypassStartEnd = bypassStart + 9;
			bypassEnd = htmlLower.indexOf("\"", bypassStartEnd);
			if (bypassEnd == -1)
			{
				break;
			}
			
			int hParamPos = htmlLower.indexOf("-h ", bypassStartEnd);
			String bypass;
			if ((hParamPos != -1) && (hParamPos < bypassEnd))
			{
				bypass = html.substring(hParamPos + 3, bypassEnd).trim();
			}
			else
			{
				bypass = html.substring(bypassStartEnd, bypassEnd).trim();
			}
			
			int firstParameterStart = bypass.indexOf(VAR_PARAM_START_CHAR);
			if(firstParameterStart != -1)
			{
				bypass = bypass.substring(0, firstParameterStart + 1);
			}
			
			player.addHtmlAction(scope, bypass);
			bypassStart = htmlLower.indexOf("=\"bypass ", bypassEnd);
		}
	}
	
	public static void buildHtmlActionCache(Player player, HtmlActionScope scope, String html)
	{
		if(player == null || scope == null || html == null)
		{
			throw new IllegalArgumentException();
		}
		
		buildHtmlBypassCache(player, scope, html);
	}
}