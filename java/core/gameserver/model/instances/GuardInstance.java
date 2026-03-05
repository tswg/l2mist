package core.gameserver.model.instances;

import core.gameserver.Config;
import core.gameserver.model.Creature;
import core.gameserver.model.GameObjectsStorage;
import core.gameserver.model.Player;
import core.gameserver.model.Skill;
import core.gameserver.model.base.Race;
import core.gameserver.templates.npc.NpcTemplate;
import core.gameserver.utils.MapUtils;

public class GuardInstance extends NpcInstance
{
	private static final long serialVersionUID = 6999848078498272594L;

	public GuardInstance(int objectId, NpcTemplate template)
	{
		super(objectId, template);
	}
	
	boolean NearRaceHeadquarters = false;
	protected void onSpawn()
	{
		if(Config.RVRMODE_ENABLE)
		{
			int rx = MapUtils.regionX(getX());
			int ry = MapUtils.regionY(getY());
			for(NpcInstance npc : GameObjectsStorage.getAllNpcsForIterate())
			{
				if(npc.getReflection() != getReflection())
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
		}
		super.onSpawn();
	}
	
	private boolean isRvREnemy(Creature attacker)
	{
		Player player = attacker.getPlayer();
		Race creatureRace = getNpcRace();
		
		if(creatureRace == null || player == null)
			return true;
		if(player.getRace() != creatureRace)
			return true;
		return false;
	}
	
	public boolean isAttackable(Creature attacker)
	{
		if(NearRaceHeadquarters)
			return isRvREnemy(attacker);
		else
			return super.isAttackable(attacker);
	}

	@Override
	public boolean isAutoAttackable(Creature attacker)
	{
		if(NearRaceHeadquarters)
			return isRvREnemy(attacker);
		else
			return attacker.isMonster() && ((MonsterInstance)attacker).isAggressive() || attacker.isPlayable() && attacker.getKarma() > 0;
	}

	@Override
	public String getHtmlPath(int npcId, int val, Player player)
	{
		String pom;
		if(val == 0)
			pom = "" + npcId;
		else
			pom = npcId + "-" + val;
		return "guard/" + pom + ".htm";
	}

	@Override
	public boolean isInvul()
	{
		return false;
	}

	@Override
	public boolean isFearImmune()
	{
		return true;
	}

	@Override
	public boolean isParalyzeImmune()
	{
		return true;
	}
	
	public boolean isHealBlocked()
	{
		if(NearRaceHeadquarters)
			return true;
		return false;
	}
	
	@Override
	protected void onReduceCurrentHp(double damage, Creature attacker, Skill skill, boolean awake, boolean standUp, boolean directHp)
	{
		if(NearRaceHeadquarters)
			if(!isRvREnemy(attacker))
				return;
		
		getAggroList().addDamageHate(attacker, (int)damage, 0);

		super.onReduceCurrentHp(damage, attacker, skill, awake, standUp, directHp);
	}
}