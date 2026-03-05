package core.gameserver.model.instances.RvRMode;

import core.gameserver.model.Creature;
import core.gameserver.model.Player;
import core.gameserver.model.Skill;
import core.gameserver.model.base.Race;
import core.gameserver.model.instances.NpcInstance;
import core.gameserver.network.l2.s2c.MagicSkillUse;
import core.gameserver.templates.npc.NpcTemplate;

public class RaceGuardInstance extends NpcInstance
{
	private static final long serialVersionUID = 4910555385191515782L;
	
	private double _upgradePAtkMul = 1.0;

	public RaceGuardInstance(int objectId, NpcTemplate template)
	{
		super(objectId, template);
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
		return isRvREnemy(attacker);
	}

	public boolean isAutoAttackable(Creature attacker)
	{
		return isRvREnemy(attacker);
	}

	public boolean isInvul()
	{
		return false;
	}

	public boolean isLethalImmune()
	{
		return true;
	}
	
	public boolean isHealBlocked()
	{
		return true;
	}

	public boolean isEffectImmune()
	{
		return true;
	}
	
	public String getHtmlPath(int npcId, int val, Player player)
	{
		String pom;
		if(val == 0)
			pom = "" + npcId;
		else
			pom = npcId + "-" + val;
		return "RvRMode/" + pom + ".htm";
	}
	
	public void onBypassFeedback(Player player, String command)
	{
		if(!canBypassCheck(player, this))
			return;

		if(command.equals("upgradeGuards"))
		{
			String htmltext = null;
			
			if(player.isHero() && getEnchantEffect() != 0)
				htmltext = getNpcId() + "-nothing.htm";
			else if(player.isHero())
			{
				setEnchantEffect(15);
				_upgradePAtkMul = 1.5;
				htmltext = getNpcId() + "-ready.htm";
				decayMe();
				spawnMe();
				broadcastPacket(new MagicSkillUse(this, this, 528, 1, 500, 1500));
			}
			else 
				htmltext = getNpcId() + "-no.htm";
			
			showChatWindow(player, "RvRMode/" + htmltext);
		}
		
		super.onBypassFeedback(player, command);
	}
	
	public int getPAtk(Creature target)
	{
		return (int) (super.getPAtk(target) * _upgradePAtkMul);
	}
	
	protected void onReduceCurrentHp(double damage, Creature attacker, Skill skill, boolean awake, boolean standUp, boolean directHp)
	{
		if(!isRvREnemy(attacker))
			return;
		
		getAggroList().addDamageHate(attacker, (int)damage, 0);

		super.onReduceCurrentHp(damage, attacker, skill, awake, standUp, directHp);
	}
}