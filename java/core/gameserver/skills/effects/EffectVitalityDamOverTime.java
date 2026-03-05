package core.gameserver.skills.effects;

import core.gameserver.cache.Msg;
import core.gameserver.model.Effect;
import core.gameserver.model.Player;
import core.gameserver.network.l2.s2c.SystemMessage;
import core.gameserver.stats.Env;

public class EffectVitalityDamOverTime extends Effect {
	public EffectVitalityDamOverTime(Env env, EffectTemplate template){
		super(env, template);
	}

		public boolean onActionTime() {
			if (_effected.isDead() || _effected.isPlayer())
			{
				return false;
			}
		Player _pEffected = (Player)_effected;

		double vitDam = calc();
		if (vitDam > _pEffected.getVitality() && getSkill().isToggle()){
			_pEffected.sendPacket(Msg.NOT_ENOUGH_MATERIALS);
			_pEffected.sendPacket(new SystemMessage(749).addSkillName(getSkill().getId(), getSkill().getDisplayLevel()));
				return false;
		}

		_pEffected.setVitality(Math.max(0.0D, _pEffected.getVitality() - vitDam));
			return true;
	}
}