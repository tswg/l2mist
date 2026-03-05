package core.gameserver.network.l2.components;

import core.gameserver.model.Player;
import core.gameserver.network.l2.s2c.L2GameServerPacket;

public interface IStaticPacket
{
	L2GameServerPacket packet(Player player);
}
