package core.gameserver.network.l2.c2s;

import core.gameserver.model.Player;
import core.gameserver.network.l2.s2c.ExGetBookMarkInfo;

/**
 * SdS
 */
public class RequestSaveBookMarkSlot extends L2GameClientPacket
{
	private String name, acronym;
	private int icon;

	@Override
	protected void readImpl()
	{
		name = readS(32);
		icon = readD();
		acronym = readS(4);
	}

	@Override
	protected void runImpl()
	{
		Player activeChar = getClient().getActiveChar();
		if(activeChar != null && activeChar.bookmarks.add(name, acronym, icon))
			activeChar.sendPacket(new ExGetBookMarkInfo(activeChar));
	}
}