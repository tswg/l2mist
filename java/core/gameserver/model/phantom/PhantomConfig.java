package core.gameserver.model.phantom;

import javolution.util.FastList;

import core.gameserver.Config;

public class PhantomConfig
{
	private FastList<Integer> nameColors = new FastList<Integer>();
	private FastList<Integer> titleColors = new FastList<Integer>();

	public void load()
	{
		nameColors = Config.PHANTOM_PLAYERS_NAME_CLOLORS;
		titleColors = Config.PHANTOM_PLAYERS_TITLE_CLOLORS;
	}

	public String getAccountName()
	{
		return Config.PHANTOM_PLAYERS_AKK;
	}

	public FastList<Integer> getNameColors()
	{
		return nameColors;
	}

	public FastList<Integer> getTitleColors()
	{
		return titleColors;
	}
}
