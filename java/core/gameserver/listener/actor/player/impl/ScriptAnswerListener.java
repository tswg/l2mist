package core.gameserver.listener.actor.player.impl;

import core.commons.lang.reference.HardReference;
import core.gameserver.listener.actor.player.OnAnswerListener;
import core.gameserver.model.Player;
import core.gameserver.scripts.Scripts;

public class ScriptAnswerListener implements OnAnswerListener
{
	private HardReference<Player> _playerRef;
	private String _scriptName;
	private Object[] _arg;

	public ScriptAnswerListener(Player player, String scriptName, Object[] arg)
	{
		_scriptName = scriptName;
		_arg = arg;
		_playerRef = player.getRef();
	}

	@Override
	public void sayYes()
	{
		Player player = _playerRef.get();
		if(player == null)
			return;

		Scripts.getInstance().callScripts(player, _scriptName.split(":")[0], _scriptName.split(":")[1], _arg);
	}

	@Override
	public void sayNo()
	{
		//
	}
}
