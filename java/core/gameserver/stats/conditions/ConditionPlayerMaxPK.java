package core.gameserver.stats.conditions;

import core.gameserver.model.Player;
import core.gameserver.stats.Env;

public class ConditionPlayerMaxPK extends Condition
{
	private final int _pk;

	public ConditionPlayerMaxPK(int pk)
	{
		_pk = pk;
	}

	@Override
	protected boolean testImpl(Env env)
	{
		if(env.character.isPlayer())
			return ((Player) env.character).getPkKills() <= _pk;
		return false;
	}
}