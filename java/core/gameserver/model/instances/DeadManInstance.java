package core.gameserver.model.instances;

import core.gameserver.ai.CharacterAI;
import core.gameserver.model.Creature;
import core.gameserver.model.Skill;
import core.gameserver.network.l2.s2c.Die;
import core.gameserver.templates.npc.NpcTemplate;

public class DeadManInstance extends NpcInstance
{
	public DeadManInstance(int objectId, NpcTemplate template)
	{
		super(objectId, template);
		setAI(new CharacterAI(this));
	}

	@Override
	protected void onSpawn()
	{
		super.onSpawn();
		setCurrentHp(0, false);
		broadcastPacket(new Die(this));
		setWalking();
	}

	@Override
	public void reduceCurrentHp(double damage, Creature attacker, Skill skill, boolean awake, boolean standUp, boolean directHp, boolean canReflect, boolean transferDamage, boolean isDot, boolean sendMessage)
	{}

	@Override
	public boolean isInvul()
	{
		return true;
	}
	
	@Override
	public boolean isBlocked()
	{
		return true;
	}
}