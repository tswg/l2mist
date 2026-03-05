package core.gameserver.scripts;

import core.gameserver.model.Player;
import core.gameserver.model.Skill;

public interface ScriptFile
{
	public void onLoad();
	public void onReload();
	public void onShutdown();
}