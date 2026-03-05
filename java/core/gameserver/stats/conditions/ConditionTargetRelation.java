package core.gameserver.stats.conditions;

import core.gameserver.model.Creature;
import core.gameserver.model.Player;
import core.gameserver.model.base.TeamType;
import core.gameserver.stats.Env;

public class ConditionTargetRelation extends Condition
{
	private final Relation _state;

	public static enum Relation
	{
		Neutral,
		Friend,
		Enemy;
	}

	public ConditionTargetRelation(Relation state)
	{
		_state = state;
	}

	@Override
	protected boolean testImpl(Env env)
	{
		return getRelation(env.character, env.target) == _state;
	}

	public static Relation getRelation(Creature activeChar, Creature aimingTarget)
	{
		if(activeChar.isPlayable() && activeChar.getPlayer() != null)
		{
			if(aimingTarget.isMonster())
				return Relation.Enemy;

			if(aimingTarget.isPlayable() && aimingTarget.getPlayer() != null)
			{
				Player player = activeChar.getPlayer();
				Player target = aimingTarget.getPlayer();

				if(player.getParty() != null && target.getParty() != null && player.getParty() == target.getParty())
					return Relation.Friend;
				if(player.getClanId() != 0 && player.getClanId() == target.getClanId() && !player.isInOlympiadMode())
					return Relation.Friend;
				if(player.getAlliance() != null && target.getAlliance() != null && player.getAlliance() == target.getAlliance() && !player.isInOlympiadMode())
					return Relation.Friend;
				if(player.isInOlympiadMode() && player.isOlympiadCompStart() && player.getOlympiadSide() == target.getOlympiadSide())
					return Relation.Friend;
				if(player.getTeam() != TeamType.NONE && target.getTeam() != TeamType.NONE && player.getTeam() == target.getTeam())
					return Relation.Friend;
			}
		}
		return Relation.Neutral;
	}
}