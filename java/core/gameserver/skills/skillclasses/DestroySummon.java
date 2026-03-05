package core.gameserver.skills.skillclasses;

import java.util.List;

import core.gameserver.model.Creature;
import core.gameserver.model.Skill;
import core.gameserver.model.Summon;
import core.gameserver.network.l2.components.SystemMsg;
import core.gameserver.network.l2.s2c.SystemMessage2;
import core.gameserver.stats.Formulas;
import core.gameserver.templates.StatsSet;

public class DestroySummon extends Skill
{
	public DestroySummon(StatsSet set)
	{
		super(set);
	}

	@Override
	public void useSkill(Creature activeChar, List<Creature> targets)
	{
		for(Creature target : targets)
			if(target != null)
			{

				if(getActivateRate() > 0 && !Formulas.calcSkillSuccess(activeChar, target, this, getActivateRate()))
				{
					activeChar.sendPacket(new SystemMessage2(SystemMsg.C1_HAS_RESISTED_YOUR_S2).addString(target.getName()).addSkillName(getId(), getLevel()));
					continue;
				}

				if(target.isSummon())
				{
					((Summon) target).saveEffects();
					((Summon) target).unSummon();
					getEffects(activeChar, target, getActivateRate() > 0, false);
				}
			}

		if(isSSPossible())
			activeChar.unChargeShots(isMagic());
	}
}