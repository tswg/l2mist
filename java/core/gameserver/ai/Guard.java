package core.gameserver.ai;

import java.util.List;

import core.gameserver.Config;
import core.gameserver.geodata.GeoEngine;
import core.gameserver.model.AggroList.AggroInfo;
import core.gameserver.model.Creature;
import core.gameserver.model.GameObjectsStorage;
import core.gameserver.model.Player;
import core.gameserver.model.World;
import core.gameserver.model.base.Race;
import core.gameserver.model.entity.RvRMode;
import core.gameserver.model.instances.MonsterInstance;
import core.gameserver.model.instances.NpcInstance;
import core.gameserver.tables.SkillTable;
import core.gameserver.utils.MapUtils;

public class Guard extends Fighter
{
	public Guard(NpcInstance actor)
	{
		super(actor);
	}
	
	private boolean firstSpawn = true;
	boolean NearRaceHeadquarters = false;
	private long _arestReuseTime;
	
	protected void onEvtSpawn()
	{
		super.onEvtSpawn();
		if(Config.RVRMODE_ENABLE)
			setNpcRace();
	}
	
	protected void onEvtAggression(Creature target, int aggro)
	{
		if(NearRaceHeadquarters && canUseArest())
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
		
		int rx = MapUtils.regionX(actor.getX());
		int ry = MapUtils.regionY(actor.getY());
		for(NpcInstance npc : GameObjectsStorage.getAllNpcsForIterate())
		{
			if(npc.getReflection() != actor.getReflection())
				continue;
			
			int tx = MapUtils.regionX(npc);
			int ty = MapUtils.regionY(npc);
			
			if(tx >= rx && tx <= rx && ty >= ry && ty <= ry)
				if(npc.getNpcId() == 50601)
				{
					NearRaceHeadquarters = true;
					break;
				}
		}
		
		if(NearRaceHeadquarters)
		{
			String RaceTownControl = RvRMode.getRaceTownControl(actor.getLoc());
			RvRMode.setNpcRace(actor, RaceTownControl);

			if(firstSpawn)
			{
				firstSpawn = false;
				actor.decayMe();
				actor.spawnMe();
			}
		}
	}

	public boolean canAttackCharacter(Creature target)
	{
		NpcInstance actor = getActor();
		if(getIntention() == CtrlIntention.AI_INTENTION_ATTACK)
		{
			AggroInfo ai = actor.getAggroList().get(target);
			return ai != null && ai.hate > 0;
		}
		return target.isMonster() || target.isPlayable();
	}
	
	public boolean checkAggression(Creature target)
	{
		if(NearRaceHeadquarters)
		{
			Player player = target.getPlayer();
			Race creatureRace = getActor().getNpcRace();
			
			if(creatureRace != null && player != null)
				if(creatureRace == player.getRace()) // Нападать на всех, кроме своей расы.
					return false;
		}
		else
		{
			NpcInstance actor = getActor();
			if(getIntention() != CtrlIntention.AI_INTENTION_ACTIVE || !isGlobalAggro())
				return false;

			if(target.isPlayable())
			{
				if(target.getKarma() == 0 || (actor.getParameter("evilGuard", false) && target.getPvpFlag() > 0))
					return false;
			}
			if(target.isMonster())
			{
				if(!((MonsterInstance)target).isAggressive())
					return false;
			}
			List<Creature> around = World.getAroundCharacters(actor, 1000, 300);
			if(around.isEmpty())
				return false;

			for(Creature cha : around)
				if(GeoEngine.canSeeTarget(actor, cha, false) && !cha.isDead() && cha.getKarma() > 0)
				{
					if(actor.getRealDistance3D(cha) <= actor.getPhysicalAttackRange() + 40)
					{
						actor.setRunning();
						actor.doAttack(cha);
					}
					else
					{
						actor.setRunning();
						tryMoveToTarget(cha);
					}
				}
		}

		return super.checkAggression(target);
	}

	public int getMaxAttackTimeout()
	{
		return 0;
	}
	
	@Override
	protected boolean randomWalk()
	{
		return false;
	}
}