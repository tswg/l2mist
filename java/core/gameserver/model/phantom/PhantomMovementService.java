package core.gameserver.model.phantom;

import core.gameserver.ai.CtrlIntention;
import core.gameserver.model.Creature;
import core.gameserver.model.Player;

public class PhantomMovementService
{
	public void pursueOrAttack(Player phantom, Creature target)
	{
		if(target == null)
			return;
		double distance = phantom.getRealDistance3D(target);
		int attackRange = phantom.getPhysicalAttackRange() + 40;
		if(distance <= attackRange)
		{
			phantom.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, target);
			return;
		}

		// dead-zone fix: force move/attack intention when target is visible but out of hit range
		phantom.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, target);
		phantom.moveToLocation(target.getLoc(), 0, false);
	}

	public void randomWalkIfIdle(Player phantom)
	{
		if(phantom.getAI().getIntention() != CtrlIntention.AI_INTENTION_ATTACK && phantom.getAI().getIntention() != CtrlIntention.AI_INTENTION_CAST)
			phantom.rndWalk();
	}
}
