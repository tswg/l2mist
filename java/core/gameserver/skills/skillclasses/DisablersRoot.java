package core.gameserver.skills.skillclasses;

import java.util.List;

import core.gameserver.model.Creature;
import core.gameserver.model.Effect;
import core.gameserver.model.Skill;
import core.gameserver.skills.EffectType;
import core.gameserver.templates.StatsSet;

public class DisablersRoot extends Skill
{
	public DisablersRoot(StatsSet set)
	{
		super(set);
	}

	@Override
	public void useSkill(Creature activeChar, List<Creature> targets)
	{
		Effect[] effects = activeChar.getPlayer().getEffectList().getAllFirstEffects();
		for(Effect effect : effects)
			if(effect != null && effect.getEffectType() == EffectType.Root)
			{
				effect.exit();
			}
	}
}