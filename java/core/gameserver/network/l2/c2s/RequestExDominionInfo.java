package core.gameserver.network.l2.c2s;

import core.gameserver.data.xml.holder.EventHolder;
import core.gameserver.model.Player;
import core.gameserver.model.entity.events.EventType;
import core.gameserver.model.entity.events.impl.DominionSiegeRunnerEvent;
import core.gameserver.network.l2.s2c.ExReplyDominionInfo;
import core.gameserver.network.l2.s2c.ExShowOwnthingPos;

public class RequestExDominionInfo extends L2GameClientPacket
{
	@Override
	protected void readImpl()
	{}

	@Override
	protected void runImpl()
	{
		Player activeChar = getClient().getActiveChar();
		if(activeChar == null)
			return;

		activeChar.sendPacket(new ExReplyDominionInfo());

		DominionSiegeRunnerEvent runnerEvent = EventHolder.getInstance().getEvent(EventType.MAIN_EVENT, 1);
		if(runnerEvent.isInProgress())
			activeChar.sendPacket(new ExShowOwnthingPos());
	}
}