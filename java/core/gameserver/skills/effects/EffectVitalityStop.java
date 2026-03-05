package core.gameserver.skills.effects;

import core.gameserver.model.Effect;
import core.gameserver.model.Player;
import core.gameserver.stats.Env;

public final class EffectVitalityStop extends Effect
{
	public EffectVitalityStop(Env env, EffectTemplate template)
	{
		super(env, template);
	}

	@Override
	public void onStart()
	{
		super.onStart();
		Player player = _effected.getPlayer();
		player.VitalityStop(true);
	}

	@Override
	public void onExit()
	{
		super.onExit();
		Player player = _effected.getPlayer();
		player.VitalityStop(false);
	}

	@Override
	public boolean onActionTime()
	{
		return false;
	}
}