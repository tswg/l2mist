package core.gameserver.webserver;

import core.gameserver.model.GameObjectsStorage;

abstract class PageParser
{
	public static String parse(String s)
	{
		if(s.contains("%online%"))
			s = s.replaceAll("%online%", String.valueOf(GameObjectsStorage.getAllPlayersCount()));
		return s;
	}
}