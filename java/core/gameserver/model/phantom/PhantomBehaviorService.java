package core.gameserver.model.phantom;

import java.util.List;

import core.commons.util.Rnd;
import core.gameserver.Config;
import core.gameserver.geodata.GeoEngine;
import core.gameserver.model.Player;
import core.gameserver.model.World;
import core.gameserver.model.base.Race;
import core.gameserver.model.instances.NpcInstance;
import core.gameserver.network.l2.components.ChatType;
import core.gameserver.network.l2.s2c.MagicSkillUse;
import core.gameserver.network.l2.s2c.Say2;

public class PhantomBehaviorService
{
	private final List<String> enchantPhrases;
	private final int[] buffers;
	private final int[][] mageBuff;
	private final int[][] warrBuff;

	public PhantomBehaviorService(List<String> enchantPhrases, int[] buffers, int[][] mageBuff, int[][] warrBuff)
	{
		this.enchantPhrases = enchantPhrases;
		this.buffers = buffers;
		this.mageBuff = mageBuff;
		this.warrBuff = warrBuff;
	}

	public void processTownLife(Player phantom)
	{
		if(Rnd.get(100) < 10 && phantom.isSitting() && Rnd.get(100) < 10)
			phantom.standUp();
		applyNpcBuffs(phantom);
		sendRandomChat(phantom);
		castSoulAnimation(phantom);
	}

	private void applyNpcBuffs(Player phantom)
	{
		if(Rnd.get(100) >= 80)
			return;
		for(NpcInstance npc : World.getAroundNpc(phantom, 100, 100))
			for(int buffer : buffers)
				if(npc.getNpcId() == buffer && !phantom.isSitting())
				{
					if(phantom.getClassId().isMage() && phantom.getClassId().getRace() != Race.orc)
						broadcastBuffs(npc, phantom, mageBuff);
					else
						broadcastBuffs(npc, phantom, warrBuff);
				}
	}

	private void broadcastBuffs(NpcInstance npc, Player phantom, int[][] buffs)
	{
		for(int[] buff : buffs)
			npc.broadcastPacket(new MagicSkillUse(npc, phantom, buff[2], buff[3], 0, 0));
	}

	private void sendRandomChat(Player phantom)
	{
		if(!Config.ALLOW_PHANTOM_CHAT || enchantPhrases.isEmpty())
			return;
		if(Rnd.get(100) >= Config.PHANTOM_CHAT_CHANSE)
			return;
		String phrase = enchantPhrases.get(Rnd.get(enchantPhrases.size() - 1));
		ChatType type = ChatType.ALL;
		int hRange = 1200;
		int vRange = 1000;
		int roll = Rnd.get(1, 3);
		if(roll == 1)
		{
			type = ChatType.SHOUT;
			hRange = 10000;
			vRange = 3000;
		}
		else if(roll == 2)
		{
			type = ChatType.TRADE;
			hRange = 5000;
			vRange = 2000;
		}
		Say2 packet = new Say2(phantom.getObjectId(), type, phantom.getName(), phrase);
		for(Player player : World.getAroundPlayers(phantom, hRange, vRange))
			if(player != null && !player.isBlockAll())
				player.sendPacket(packet);
	}

	private void castSoulAnimation(Player phantom)
	{
		if(Rnd.get(100) >= 10)
			return;
		if(phantom.getClassId().isMage() && phantom.getClassId().getRace() != Race.orc)
			phantom.broadcastPacket(new MagicSkillUse(phantom, phantom, 2158, 1, 0, 0));
		else
			phantom.broadcastPacket(new MagicSkillUse(phantom, phantom, 2153, 1, 0, 0));
	}
}
