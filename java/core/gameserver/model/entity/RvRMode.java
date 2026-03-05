package core.gameserver.model.entity;

import core.gameserver.instancemanager.MapRegionManager;
import core.gameserver.instancemanager.ServerVariables;
import core.gameserver.listener.actor.player.OnAnswerListener;
import core.gameserver.model.GameObjectsStorage;
import core.gameserver.model.Player;
import core.gameserver.model.base.Race;
import core.gameserver.model.instances.NpcInstance;
import core.gameserver.templates.mapregion.DomainArea;
import core.gameserver.utils.Location;
import core.gameserver.utils.MapUtils;

public class RvRMode
{
	public static String getRaceTownControl(Location loc)
	{
		String RaceTownControl = "NPC";
        DomainArea domain = MapRegionManager.getInstance().getRegionData(DomainArea.class, loc);
		
		if(domain != null)
			switch(domain.getId())
			{
				case 1: // Town of Gludio
					RaceTownControl = ServerVariables.getString("RaceGludioControl", "NPC");
					break;
				case 2: // Town of Dion
					RaceTownControl = ServerVariables.getString("RaceDionControl", "NPC");
					break;
				case 3: // Town of Giran
					RaceTownControl = ServerVariables.getString("RaceGiranControl", "NPC");
					break;
				case 4: // Town of Oren
					RaceTownControl = ServerVariables.getString("RaceOrenControl", "NPC");
					break;
				case 5: // Town of Aden
					RaceTownControl = ServerVariables.getString("RaceAdenControl", "NPC");
					break;
				case 6: // Town of Innadril
					RaceTownControl = ServerVariables.getString("RaceInnadrilControl", "NPC");
					break;
				case 7: // Town of Goddard
					RaceTownControl = ServerVariables.getString("RaceGoddardControl", "NPC");
					break;
				case 8: // Town of Rune
					RaceTownControl = ServerVariables.getString("RaceRuneControl", "NPC");
					break;
				case 9: // Town of Schuttgart
					RaceTownControl = ServerVariables.getString("RaceSchuttgartControl", "NPC");
					break;
			}
		
		return RaceTownControl;
	}
	
	public static String getTownName(Location loc, boolean isRusName)
	{
		String TownName = isRusName? "Другой" : "Other";
        DomainArea domain = MapRegionManager.getInstance().getRegionData(DomainArea.class, loc);
		
		if(domain != null)
			switch(domain.getId())
			{
				case 1: // Town of Gludio
					TownName = isRusName? "Глудио" : "Gludio";
					break;
				case 2: // Town of Dion
					TownName = isRusName? "Дион" : "Dion";
					break;
				case 3: // Town of Giran
					TownName = isRusName? "Гиран" : "Giran";
					break;
				case 4: // Town of Oren
					TownName = isRusName? "Орен" : "Oren";
					break;
				case 5: // Town of Aden
					TownName = isRusName? "Аден" : "Aden";
					break;
				case 6: // Town of Innadril
					TownName = isRusName? "Хейн" : "Innadril";
					break;
				case 7: // Town of Goddard
					TownName = isRusName? "Годдард" : "Goddard";
					break;
				case 8: // Town of Rune
					TownName = isRusName? "Руна" : "Rune";
					break;
				case 9: // Town of Schuttgart
					TownName = isRusName? "Шутгарт" : "Schuttgart";
					break;
			}
		
		return TownName;
	}
	
	public static void setNpcRace(NpcInstance actor, String RaceTownControl)
	{
		String NpcName = "Race Guard";
		
		if(!RaceTownControl.equalsIgnoreCase("NPC"))
		{
			if(RaceTownControl.equalsIgnoreCase("human"))
			{
				actor.setNpcRace(Race.human);
				actor.setDisplayId(35124);
				actor.setLHandId(0);
				if(actor.getPhysicalAttackRange() < 41)
					actor.setRHandId(292);
				else
					actor.setRHandId(280);
				actor.setCollisionHeight(24.0);
				actor.setCollisionRadius(8.0);
				actor.setName(NpcName);
				actor.setTitle("Human");
			}
			if(RaceTownControl.equalsIgnoreCase("elf"))
			{
				actor.setNpcRace(Race.elf);
				actor.setDisplayId(35126);
				actor.setLHandId(0);
				if(actor.getPhysicalAttackRange() < 41)
					actor.setRHandId(301);
				else
					actor.setRHandId(285);
				actor.setCollisionHeight(23.5);
				actor.setCollisionRadius(8.0);
				actor.setName(NpcName);
				actor.setTitle("Elf");
			}
			if(RaceTownControl.equalsIgnoreCase("darkelf"))
			{
				actor.setNpcRace(Race.darkelf);
				actor.setDisplayId(35199);
				actor.setLHandId(0);
				if(actor.getPhysicalAttackRange() < 41)
					actor.setRHandId(302);
				else
					actor.setRHandId(284);
				actor.setCollisionHeight(25.0);
				actor.setCollisionRadius(8.0);
				actor.setName(NpcName);
				actor.setTitle("Dark Elf");
			}
			if(RaceTownControl.equalsIgnoreCase("orc"))
			{
				actor.setNpcRace(Race.orc);
				actor.setDisplayId(36205);
				actor.setLHandId(0);
				if(actor.getPhysicalAttackRange() < 41)
					actor.setRHandId(304);
				else
					actor.setRHandId(286);
				actor.setCollisionHeight(27.0);
				actor.setCollisionRadius(8.0);
				actor.setName(NpcName);
				actor.setTitle("Orc");
			}
			if(RaceTownControl.equalsIgnoreCase("dwarf"))
			{
				actor.setNpcRace(Race.dwarf);
				actor.setDisplayId(35330);
				actor.setLHandId(0);
				if(actor.getPhysicalAttackRange() < 41)
					actor.setRHandId(300);
				else
					actor.setRHandId(283);
				actor.setCollisionHeight(19.0);
				actor.setCollisionRadius(8.0);
				actor.setName(NpcName);
				actor.setTitle("Dwarf");
			}
			if(RaceTownControl.equalsIgnoreCase("kamael"))
			{
				actor.setNpcRace(Race.kamael);
				if(actor.getPhysicalAttackRange() < 41)
				{
					actor.setDisplayId(32180);
					actor.setLHandId(0);
					actor.setRHandId(9645);
					actor.setCollisionHeight(25.0);
					actor.setCollisionRadius(13.0);
				}
				else
				{
					actor.setDisplayId(32174);
					actor.setLHandId(0);
					actor.setRHandId(9644);
					actor.setCollisionHeight(22.5);
					actor.setCollisionRadius(13.0);
				}
				actor.setName(NpcName);
				actor.setTitle("Kamael");
			}
		}
		else
		{
			if(actor.getPhysicalAttackRange() < 41)
			{
				actor.setDisplayId(21587);
				actor.setLHandId(0);
				actor.setRHandId(234);
				actor.setCollisionHeight(29.0);
				actor.setCollisionRadius(10.0);
			}
			else
			{
				actor.setDisplayId(21590);
				actor.setLHandId(0);
				actor.setRHandId(99);
				actor.setCollisionHeight(28.0);
				actor.setCollisionRadius(10.0);
			}
			actor.setName(NpcName);
			actor.setTitle("Vampire");
		}
	}
	
	public static boolean FrendlyTown(Player player, Location loc)
	{
		boolean NearRaceHeadquarters = false;
		int rx = MapUtils.regionX(loc.getX());
		int ry = MapUtils.regionY(loc.getY());
		for(NpcInstance npc : GameObjectsStorage.getAllNpcsForIterate())
		{
			int tx = MapUtils.regionX(npc);
			int ty = MapUtils.regionY(npc);
			
			if(tx >= rx && tx <= rx && ty >= ry && ty <= ry)
				if(npc.getNpcId() == 50601)
				{
					NearRaceHeadquarters = true;
					break;
				}
		}
		
		if(NearRaceHeadquarters && !getRaceTownControl(loc).equalsIgnoreCase(String.valueOf(player.getRace())))
			return false;	
		return true;
	}
	
	public static class UrgeAnswerListener implements OnAnswerListener
	{
		Player player;
		Location loc;

		public UrgeAnswerListener(Player confederate, Location townloc) {
			player = confederate;
			loc = Location.coordsRandomize(townloc, 10, 150);
		}

		public void sayYes() {
			if(player != null && loc != null)
				player.teleToLocation(loc);	
		}

		public void sayNo() {
			// nothing
		}
	}
}