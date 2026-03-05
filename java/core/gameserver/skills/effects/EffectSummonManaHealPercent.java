package core.gameserver.skills.effects;

import core.gameserver.model.Effect;
import core.gameserver.network.l2.components.SystemMsg;
import core.gameserver.network.l2.s2c.SystemMessage2;
import core.gameserver.stats.Env;
import core.gameserver.stats.Stats;

public class EffectSummonManaHealPercent extends Effect
{
	private final boolean _ignoreMpEff;

	public EffectSummonManaHealPercent(Env env, EffectTemplate template)
	{
		super(env, template);
		_ignoreMpEff = template.getParam().getBool("ignoreMpEff", true);
	}
	
	@Override
	public void onStart()
	{
		super.onStart();

		if(_effected.isHealBlocked())
			return;

		double mp = calc() * _effected.getMaxMp() / 100.;
		double newMp = mp * (!_ignoreMpEff ? _effected.calcStat(Stats.MANAHEAL_EFFECTIVNESS, 100., _effector, getSkill()) : 100.) / 100.;
		double addToMp = Math.max(0, Math.min(newMp, _effected.calcStat(Stats.MP_LIMIT, null, null) * _effected.getMaxMp() / 100. - _effected.getCurrentMp()));

		_effected.sendPacket(new SystemMessage2(SystemMsg.S1_MP_HAS_BEEN_RESTORED).addInteger(Math.round(addToMp)));

		if(addToMp > 0)
			_effected.setCurrentMp(addToMp + _effected.getCurrentMp());
	}

	@Override
	public boolean onActionTime()
	{
		return false;
	}
}