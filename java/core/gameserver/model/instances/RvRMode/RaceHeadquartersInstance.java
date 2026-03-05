package core.gameserver.model.instances.RvRMode;

import java.util.List;

import core.commons.threading.RunnableImpl;
import core.gameserver.Config;
import core.gameserver.ThreadPoolManager;
import core.gameserver.geodata.GeoEngine;
import core.gameserver.instancemanager.ReflectionManager;
import core.gameserver.model.Creature;
import core.gameserver.model.GameObjectsStorage;
import core.gameserver.model.Player;
import core.gameserver.model.Skill;
import core.gameserver.model.World;
import core.gameserver.model.base.Race;
import core.gameserver.model.base.TeamType;
import core.gameserver.model.entity.RvRMode;
import core.gameserver.model.entity.olympiad.Olympiad;
import core.gameserver.model.instances.NpcInstance;
import core.gameserver.network.l2.components.SystemMsg;
import core.gameserver.network.l2.s2c.ConfirmDlg;
import core.gameserver.network.l2.s2c.MagicSkillUse;
import core.gameserver.network.l2.s2c.NpcHtmlMessage;
import core.gameserver.network.l2.s2c.PlaySound;
import core.gameserver.templates.npc.NpcTemplate;
import core.gameserver.utils.Location;
import core.gameserver.utils.MapUtils;
import core.gameserver.utils.NpcUtils;

public class RaceHeadquartersInstance extends NpcInstance
{
	private static final long serialVersionUID = 7796664260192812727L;
	
	private boolean firstAttack = true;
	private boolean scoutingPossible = true;
	private boolean patrolPossible = true;
	private boolean urgePossible = true;
	private boolean isWinPeriod = false;
	private static int RaceGuardNpcId = 50600;

	public RaceHeadquartersInstance(int objectId, NpcTemplate template)
	{
		super(objectId, template);
	}
	
	protected void onSpawn()
	{
		super.onSpawn();
		isWinPeriod = true;
		ThreadPoolManager.getInstance().schedule(new endWinPeriod(), Config.RVRMODE_SIEGE_WINNER_PERIOD * 60 * 1000);
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
	
	public boolean isImmobilized()
	{
		return true;
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

		if(command.equals("callGuards"))
		{
			String htmltext = null;
			int countAround = 0;
			
			List<NpcInstance> around = World.getAroundNpc(this);
			if(!around.isEmpty())
			{
				for(NpcInstance npc : around)
					if(!npc.isDead() && npc.getNpcId() == RaceGuardNpcId)
						countAround++;
			}
			
			if(player.isHero() && countAround > 3)
				htmltext = getNpcId() + "-nothing.htm";
			else if(player.isHero())
			{
				broadcastPacket(new MagicSkillUse(this, this, 2024, 1, 500, 1500));
				
				for(int i = 0; i < 4 - countAround; i++)
					NpcUtils.spawnSingle(RaceGuardNpcId, Location.findPointToStay(getLoc(), 100, 150));
				
				htmltext = getNpcId() + "-ready.htm";
			}
			else 
				htmltext = getNpcId() + "-no.htm";
			
			showChatWindow(player, "RvRMode/" + htmltext);
		}
		else if(command.equals("doScouting"))
		{
			if(!player.isHero())
				showChatWindow(player, "RvRMode/" + getNpcId() + "-no.htm");
			else if(!scoutingPossible)
				showChatWindow(player, "RvRMode/" + getNpcId() + "-noscouting.htm");
			else
			{
				int countHuman = 0, countElf = 0, countDarkElf = 0, countOrc = 0, countDwarf = 0, countKamael = 0;
				
				int rx = MapUtils.regionX(getX());
				int ry = MapUtils.regionY(getY());
				for(Player p : GameObjectsStorage.getAllPlayersForIterate())
				{
					if(p.getReflection() != getReflection())
						continue;
					
					int tx = MapUtils.regionX(p);
					int ty = MapUtils.regionY(p);
					
					if(tx >= rx && tx <= rx && ty >= ry && ty <= ry)
					{
						if(p.getRace() == Race.human)
							countHuman++;
						if(p.getRace() == Race.elf)
							countElf++;
						if(p.getRace() == Race.darkelf)
							countDarkElf++;
						if(p.getRace() == Race.orc)
							countOrc++;
						if(p.getRace() == Race.dwarf)
							countDwarf++;
						if(p.getRace() == Race.kamael)
							countKamael++;
					}
				}
				
				NpcHtmlMessage html = new NpcHtmlMessage(player, this);
				html.setFile("RvRMode/" + getNpcId() + "-scouting.htm");
				html.replace("%countHuman%", Integer.toString(countHuman));
				html.replace("%countElf%", Integer.toString(countElf));
				html.replace("%countDarkElf%", Integer.toString(countDarkElf));
				html.replace("%countOrc%", Integer.toString(countOrc));
				html.replace("%countDwarf%", Integer.toString(countDwarf));
				html.replace("%countKamael%", Integer.toString(countKamael));
				player.sendPacket(html);
				
				scoutingPossible = false;
				ThreadPoolManager.getInstance().schedule(new resetScoutingPossible(), Config.RVRMODE_SCOUTING_COMMAND_PERIOD * 60 * 1000);
			}
		}
		else if(command.equals("doPatrol"))
		{
			if(!player.isHero())
				showChatWindow(player, "RvRMode/" + getNpcId() + "-no.htm");
			else if(!patrolPossible)
				showChatWindow(player, "RvRMode/" + getNpcId() + "-nopatrol.htm");
			else
			{
				Player chaEnemy = null;
				int BusyGuard = 0;
				
				List<NpcInstance> around = World.getAroundNpc(this);
				if(!around.isEmpty())
				{
					for(NpcInstance npc : around)
					{
						if(!npc.isDead() && npc.getNpcId() == RaceGuardNpcId && BusyGuard < 2)
						{
							BusyGuard++;
							List<Player> around2 = World.getAroundPlayers(this);
							for(Player cha : around2)
							{
								if(!cha.isDead() && cha.getRace() != getNpcRace())
								{
									chaEnemy = cha.getPlayer();
									break;
								}
							}			
							if(chaEnemy != null)
							{
								npc.setRunning();
								tryMoveToEnemy(npc, chaEnemy);
								npc.getAggroList().addDamageHate(chaEnemy, 1, 1);
							}
						}
					}
				}
				
				if(BusyGuard == 0)
				{
					showChatWindow(player, "RvRMode/" + getNpcId() + "-noguardpatrol.htm");
					return;
				}
				else if(chaEnemy == null)
				{
					showChatWindow(player, "RvRMode/" + getNpcId() + "-notargetpatrol.htm");
					return;
				}
				else
				{
					showChatWindow(player, "RvRMode/" + getNpcId() + "-gopatrol.htm");
					patrolPossible = false;
					ThreadPoolManager.getInstance().schedule(new resetPatrolPossible(), Config.RVRMODE_PATROL_COMMAND_PERIOD * 60 * 1000);
				}
			}
		}
		else if(command.equals("urgeRace"))
		{
			if(!player.isHero())
				showChatWindow(player, "RvRMode/" + getNpcId() + "-no.htm");
			else if(!urgePossible)
				showChatWindow(player, "RvRMode/" + getNpcId() + "-nourge.htm");
			else
			{
				for(Player confederate : GameObjectsStorage.getAllPlayersForIterate())
					if(confederate != null && checkCondition(confederate))
					{
						confederate.sendPacket(new PlaySound("AmbSound2.Horn"));
						
						if(confederate == player)
							continue;
						
						ConfirmDlg packet = new ConfirmDlg(SystemMsg.S1, 60000).addString(confederate.isLangRus()? "Герой призывает Вас на защиту города " + RvRMode.getTownName(getLoc(), true) + "" : "Hero urges you to defend Town of " + RvRMode.getTownName(getLoc(), false) + "");
						confederate.ask(packet, new RvRMode.UrgeAnswerListener(confederate, getLoc()));			
					}
				
				showChatWindow(player, "RvRMode/" + getNpcId() + "-dourge.htm");
				urgePossible = false;
				ThreadPoolManager.getInstance().schedule(new resetUrgePossible(), Config.RVRMODE_URGE_COMMAND_PERIOD * 60 * 1000);
			}
		}
		
		super.onBypassFeedback(player, command);
	}
	
	private boolean checkCondition(Player player)
	{
		if(player.getReflection() != ReflectionManager.DEFAULT || player.isInOlympiadMode() || Olympiad.isRegistered(player) || player.isInOfflineMode() || player.getTeam() != TeamType.NONE)
			return false;
		if(player.getRace() != getNpcRace())
			return false;
		return true;
	}
	
	private void tryMoveToEnemy(NpcInstance npc, Player target)
	{
		if(!npc.followToCharacter(target, npc.getPhysicalAttackRange(), true))
		{
			npc.broadcastPacketToOthers(new MagicSkillUse(npc, npc, 2036, 1, 500, 0));
			Location loc = GeoEngine.moveCheckForAI(target.getLoc(), npc.getLoc(), npc.getGeoIndex());
			if(!GeoEngine.canMoveToCoord(npc.getX(), npc.getY(), npc.getZ(), loc.x, loc.y, loc.z, npc.getGeoIndex())) // Для подстраховки
				loc = target.getLoc();
			npc.teleToLocation(loc);
		}
	}
	
	private class resetUrgePossible extends RunnableImpl
	{
		public void runImpl() throws Exception
		{
			urgePossible = true;
		}
	}
	
	private class resetScoutingPossible extends RunnableImpl
	{
		public void runImpl() throws Exception
		{
			scoutingPossible = true;
		}
	}
	
	private class resetPatrolPossible extends RunnableImpl
	{
		public void runImpl() throws Exception
		{
			patrolPossible = true;
		}
	}
	
	private class resetPlayMusicTimer extends RunnableImpl
	{
		public void runImpl() throws Exception
		{
			firstAttack = true;
		}
	}
	
	private class endWinPeriod extends RunnableImpl
	{
		public void runImpl() throws Exception
		{
			isWinPeriod = false;
		}
	}
	
	protected void onReduceCurrentHp(double damage, Creature attacker, Skill skill, boolean awake, boolean standUp, boolean directHp)
	{
		if(isWinPeriod)
			return;
		
		if(!isRvREnemy(attacker))
			return;
		
		if(firstAttack)
		{
			firstAttack = false;
			broadcastPacketToOthers(new PlaySound(PlaySound.Type.MUSIC, "B03_F", 1, 0, getLoc()));
			ThreadPoolManager.getInstance().schedule(new resetPlayMusicTimer(), 130000);
		}

		super.onReduceCurrentHp(damage, attacker, skill, awake, standUp, directHp);
	}
}