package core.gameserver.model;

import java.util.Iterator;

import core.commons.collections.EmptyIterator;
import core.gameserver.network.l2.components.IStaticPacket;

public interface PlayerGroup extends Iterable<Player>
{
	public static final PlayerGroup EMPTY = new PlayerGroup()
	{
		@Override
		public void broadCast(IStaticPacket... packet)
		{

		}

		@Override
		public Iterator<Player> iterator()
		{
			return EmptyIterator.getInstance();
		}
	};

	void broadCast(IStaticPacket... packet);
}
