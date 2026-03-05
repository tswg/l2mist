package core.gameserver.network.l2.c2s;

import core.gameserver.model.Player;
import core.gameserver.network.l2.s2c.RecipeBookItemList;

public class RequestRecipeBookOpen extends L2GameClientPacket
{
	private boolean isDwarvenCraft;

	@Override
	protected void readImpl()
	{
		if(_buf.hasRemaining())
			isDwarvenCraft = readD() == 0;
	}

	@Override
	protected void runImpl()
	{
		Player activeChar = getClient().getActiveChar();
		if(activeChar == null)
			return;

		sendPacket(new RecipeBookItemList(activeChar, isDwarvenCraft));
	}
}