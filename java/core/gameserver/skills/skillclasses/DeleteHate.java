package core.gameserver.skills.skillclasses;

import java.util.List;

import core.commons.util.Rnd;
import core.gameserver.Config;
import core.gameserver.ai.CtrlIntention;
import core.gameserver.model.Creature;
import core.gameserver.model.Player;
import core.gameserver.model.Skill;
import core.gameserver.model.instances.NpcInstance;
import core.gameserver.network.l2.components.CustomMessage;
import core.gameserver.templates.StatsSet;

public class DeleteHate extends Skill
{
	public DeleteHate(StatsSet set)
	{
		super(set);
	}

	@Override
	public void useSkill(Creature activeChar, List<Creature> targets)
	{
		for(Creature target : targets)
			if(target != null)
			{

				if(target.isRaid())
					continue;

				if(getActivateRate() > 0)
				{
					if(Config.SKILLS_CHANCE_SHOW && activeChar.isPlayer() && ((Player)activeChar).getVarB("SkillsHideChance")  || ((Player) activeChar).isGM())
						activeChar.sendMessage(new CustomMessage("core.gameserver.skills.Formulas.Chance", (Player)activeChar).addString(getName()).addNumber(getActivateRate()));

					if(!Rnd.chance(getActivateRate()))
						return;
				}

				if(target.isNpc())
				{
					NpcInstance npc = (NpcInstance) target;
					npc.getAggroList().clear(false);
					npc.getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
				}

				getEffects(activeChar, target, false, false);
			}
	}
}
