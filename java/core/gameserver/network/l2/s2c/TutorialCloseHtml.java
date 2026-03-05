package core.gameserver.network.l2.s2c;

import core.gameserver.enums.HtmlActionScope;

public class TutorialCloseHtml extends L2GameServerPacket
{
	public static final L2GameServerPacket STATIC = new TutorialCloseHtml();
	
	public void runImpl()
	{
		getClient().getActiveChar().clearHtmlActions(HtmlActionScope.TUTORIAL_HTML);
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0xa9);
	}
}