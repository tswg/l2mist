package core.gameserver.network.l2.s2c;

import core.gameserver.network.l2.components.NpcString;

public abstract class NpcStringContainer extends L2GameServerPacket
{
	private final NpcString _npcString;
	private final String[] _parameters = new String[5];

	protected NpcStringContainer(NpcString npcString, String... arg)
	{
		_npcString = npcString;
		System.arraycopy(arg, 0, _parameters, 0, arg.length);
	}

	protected void writeElements()
	{
		writeD(_npcString.getId());
		for(String st : _parameters)
			writeS(st);
	}
}
