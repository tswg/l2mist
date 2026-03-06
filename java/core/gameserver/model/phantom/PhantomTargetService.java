package core.gameserver.model.phantom;

import core.gameserver.geodata.GeoEngine;
import core.gameserver.model.Player;
import core.gameserver.model.World;
import core.gameserver.model.instances.NpcInstance;

public class PhantomTargetService
{
	public Player findPvpTarget(Player phantom, int range)
	{
		for(Player player : World.getAroundPlayers(phantom, range, range))
			if(GeoEngine.canSeeTarget(phantom, player, false) && !player.isDead() && !player.isInZonePeace() && !phantom.isInZonePeace() && (player.getKarma() != 0 || player.getPvpFlag() > 0))
				return player;
		return null;
	}

	public NpcInstance findPveTarget(Player phantom, int range)
	{
		for(NpcInstance npc : World.getAroundNpc(phantom, range, 200))
			if(GeoEngine.canSeeTarget(phantom, npc, false) && npc.isMonster() && !npc.isDead())
				return npc;
		return null;
	}
}
