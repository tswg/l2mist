package core.gameserver.ai;

import java.util.List;

import core.gameserver.data.xml.holder.NpcHolder;
import core.gameserver.instancemanager.MapRegionManager;
import core.gameserver.instancemanager.ServerVariables;
import core.gameserver.model.Creature;
import core.gameserver.model.GameObjectsStorage;
import core.gameserver.model.Playable;
import core.gameserver.model.Player;
import core.gameserver.model.World;
import core.gameserver.model.base.Race;
import core.gameserver.model.entity.RvRMode;
import core.gameserver.model.instances.GuardInstance;
import core.gameserver.model.instances.NpcInstance;
import core.gameserver.network.l2.s2c.MagicSkillUse;
import core.gameserver.network.l2.s2c.PlaySound;
import core.gameserver.tables.SkillTable;
import core.gameserver.templates.mapregion.DomainArea;
import core.gameserver.utils.Location;
import core.gameserver.utils.MapUtils;
import core.gameserver.utils.NpcUtils;

public class RaceHeadquarters extends DefaultAI
{
	
	public RaceHeadquarters(NpcInstance actor)
	{
		super(actor);
		AI_TASK_ACTIVE_DELAY = 5000;
	}
	
	private String RaceWinner = "NPC";
	private boolean firstSpawn = true;
	private static int DisplayNpcId = 35062;
	private static int RaceGuardNpcId = 50600;
	private static String NpcName = "Race Headquarters";
	
	protected void onEvtSpawn()
	{
		super.onEvtSpawn();
		setNpcRace();
	}
	
	protected void onEvtThink()
	{
		NpcInstance actor = getActor();
		
		List<Playable> around = World.getAroundPlayables(actor, 1000, 300);
		if(around.isEmpty())
			return;

		for(Playable cha : around)
			if(!cha.isDead() && cha.getPlayer().getRace() != actor.getNpcRace())
			{
				actor.doCast(SkillTable.getInstance().getInfo(564, 4), actor, true);
				break;
			}
	}
	
	protected void onEvtAttacked(Creature attacker, int damage)
	{}
	
	protected void onEvtAggression(Creature target, int aggro)
	{}
	
	protected void onEvtDead(Creature killer)
	{
		doDieRaceGuard(killer);
		townControlSwitch(killer);
		super.onEvtDead(killer);
	}
	
	private void setNpcRace() 
	{
		NpcInstance actor = getActor();
		String RaceTownControl = RvRMode.getRaceTownControl(actor.getLoc());
		
		if(!RaceTownControl.equalsIgnoreCase("NPC"))
		{
			if(RaceTownControl.equalsIgnoreCase("human"))
			{
				actor.setNpcRace(Race.human);
				actor.setDisplayId(DisplayNpcId);
				actor.setName(NpcName);
				actor.setTitle("Human");
			}
			if(RaceTownControl.equalsIgnoreCase("elf"))
			{
				actor.setNpcRace(Race.elf);
				actor.setDisplayId(DisplayNpcId);
				actor.setName(NpcName);
				actor.setTitle("Elf");
			}
			if(RaceTownControl.equalsIgnoreCase("darkelf"))
			{
				actor.setNpcRace(Race.darkelf);
				actor.setDisplayId(DisplayNpcId);
				actor.setName(NpcName);
				actor.setTitle("Dark Elf");
			}
			if(RaceTownControl.equalsIgnoreCase("orc"))
			{
				actor.setNpcRace(Race.orc);
				actor.setDisplayId(DisplayNpcId);
				actor.setName(NpcName);
				actor.setTitle("Orc");
			}
			if(RaceTownControl.equalsIgnoreCase("dwarf"))
			{
				actor.setNpcRace(Race.dwarf);
				actor.setDisplayId(DisplayNpcId);
				actor.setName(NpcName);
				actor.setTitle("Dwarf");
			}
			if(RaceTownControl.equalsIgnoreCase("kamael"))
			{
				actor.setNpcRace(Race.kamael);
				actor.setDisplayId(DisplayNpcId);
				actor.setName(NpcName);
				actor.setTitle("Kamael");
			}
		}
		else
		{
			actor.setDisplayId(DisplayNpcId);
			actor.setName(NpcName);
			actor.setTitle("Vampire");
		}

		if(firstSpawn)
		{
			firstSpawn = false;
			actor.decayMe();
			actor.spawnMe();
			actor.broadcastPacket(new MagicSkillUse(actor, actor, 5103, 1, 1000, 0));
			spawnRaceGuard(actor);
		}
	}
	
	private void spawnRaceGuard(NpcInstance actor)
	{
		for(int i = 0; i < 4; i++)
			NpcUtils.spawnSingle(RaceGuardNpcId, Location.findPointToStay(actor.getLoc(), 100, 150));
	}
	
	private void townControlSwitch(Creature killer)
	{
		Player player = killer.getPlayer();
		NpcInstance actor = getActor();
		DomainArea domain = MapRegionManager.getInstance().getRegionData(DomainArea.class, actor.getLoc());
		
		if(player != null)
			RaceWinner = String.valueOf(player.getRace());
		if(domain != null)
			switch(domain.getId())
			{
				case 1: // Town of Gludio
					ServerVariables.set("RaceGludioControl", RaceWinner);
					break;
				case 2: // Town of Dion
					ServerVariables.set("RaceDionControl", RaceWinner);
					break;
				case 3: // Town of Giran
					ServerVariables.set("RaceGiranControl", RaceWinner);
					break;
				case 4: // Town of Oren
					ServerVariables.set("RaceOrenControl", RaceWinner);
					break;
				case 5: // Town of Aden
					ServerVariables.set("RaceAdenControl", RaceWinner);
					break;
				case 6: // Town of Innadril
					ServerVariables.set("RaceInnadrilControl", RaceWinner);
					break;
				case 7: // Town of Goddard
					ServerVariables.set("RaceGoddardControl", RaceWinner);
					break;
				case 8: // Town of Rune
					ServerVariables.set("RaceRuneControl", RaceWinner);
					break;
				case 9: // Town of Schuttgart
					ServerVariables.set("RaceSchuttgartControl", RaceWinner);
					break;
			}
		
		NpcUtils.spawnSingle(50601, actor.getLoc());
	}
	
	private void doDieRaceGuard(Creature killer)
	{
		NpcInstance actor = getActor();
		
		actor.broadcastPacketToOthers(new PlaySound(PlaySound.Type.MUSIC, "NS20_F", 1, 0, actor.getLoc()));
		
		int rx = MapUtils.regionX(actor.getX());
		int ry = MapUtils.regionY(actor.getY());
		for(NpcInstance npc : GameObjectsStorage.getAllNpcsForIterate())
		{
			if(npc.getReflection() != actor.getReflection())
				continue;
			
			int tx = MapUtils.regionX(npc);
			int ty = MapUtils.regionY(npc);
			
			if(tx >= rx && tx <= rx && ty >= ry && ty <= ry)
				if(!npc.isDead() && (npc.getNpcId() == RaceGuardNpcId || NpcHolder.getInstance().getTemplate(npc.getNpcId()).getInstanceClass() == GuardInstance.class))
					npc.doDie(actor);
		}
	}
}
