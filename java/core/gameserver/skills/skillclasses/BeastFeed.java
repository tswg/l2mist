package core.gameserver.skills.skillclasses;

import java.util.List;

import core.commons.threading.RunnableImpl;
import core.gameserver.ThreadPoolManager;
import core.gameserver.model.Creature;
import core.gameserver.model.Player;
import core.gameserver.model.Skill;
import core.gameserver.model.instances.FeedableBeastInstance;
import core.gameserver.templates.StatsSet;

public class BeastFeed extends Skill
{
	public BeastFeed(StatsSet set)
	{
		super(set);
	}

	@Override
	public void useSkill(final Creature activeChar, List<Creature> targets)
	{
		for(final Creature target : targets)
		{
			ThreadPoolManager.getInstance().execute(new RunnableImpl()
			{
				@Override
				public void runImpl() throws Exception
				{
					if(target instanceof FeedableBeastInstance)
						((FeedableBeastInstance) target).onSkillUse((Player) activeChar, _id);
				}
			});
		}
	}
}
