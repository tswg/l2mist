package core.gameserver.network.l2.c2s;

import core.gameserver.instancemanager.QuestManager;
import core.gameserver.model.Player;
import core.gameserver.model.quest.Quest;
import core.gameserver.model.quest.QuestState;
import core.gameserver.network.l2.s2c.ExQuestNpcLogList;

/**
 * @author VISTALL
 * @date 14:47/26.02.2011
 */
public class RequestAddExpandQuestAlarm extends L2GameClientPacket
{
	private int _questId;

	@Override
	protected void readImpl() throws Exception
	{
		_questId = readD();
	}

	@Override
	protected void runImpl() throws Exception
	{
		Player player = getClient().getActiveChar();
		if(player == null)
			return;

		Quest quest = QuestManager.getQuest(_questId);
		if(quest == null)
			return;

		QuestState state = player.getQuestState(quest.getClass());
		if(state == null)
			return;

		player.sendPacket(new ExQuestNpcLogList(state));
	}
}
