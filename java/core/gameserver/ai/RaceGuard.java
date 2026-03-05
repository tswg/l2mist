package core.gameserver.ai;

import core.gameserver.model.Creature;
import core.gameserver.model.Player;
import core.gameserver.model.base.Race;
import core.gameserver.model.entity.RvRMode;
import core.gameserver.model.instances.NpcInstance;
import core.gameserver.tables.SkillTable;

public class RaceGuard extends Fighter
{
	
	public RaceGuard(NpcInstance actor)
	{
		super(actor);
	}
	
	private boolean firstSpawn = true;
	private long _arestReuseTime;
	
	protected void onEvtSpawn()
	{
		super.onEvtSpawn();
		setNpcRace();
	}
	
	protected void onEvtAggression(Creature target, int aggro)
	{
		if(canUseArest())
			getActor().doCast(SkillTable.getInstance().getInfo(402, 10), target, true);
		super.onEvtAggression(target, aggro);
	}
	
	private boolean canUseArest()
	{
		long currentMillis = System.currentTimeMillis();

		if(currentMillis - _arestReuseTime < 10000L)
			return false;

		_arestReuseTime = currentMillis;
		return true;
	}
	
	private void setNpcRace() 
	{
		NpcInstance actor = getActor();
		String RaceTownControl = RvRMode.getRaceTownControl(actor.getLoc());
		RvRMode.setNpcRace(actor, RaceTownControl);

		if(firstSpawn)
		{
			firstSpawn = false;
			actor.decayMe();
			actor.spawnMe();
		}
	}
	
	protected boolean checkAggression(Creature target)
	{
		Player player = target.getPlayer();
		Race creatureRace = getActor().getNpcRace();
		
		if(creatureRace != null && player != null)
			if(creatureRace == player.getRace()) // Нападать на всех, кроме своей расы.
				return false;

		return super.checkAggression(target);
	}
	
	public int getMaxAttackTimeout()
	{
		return 0;
	}
}
