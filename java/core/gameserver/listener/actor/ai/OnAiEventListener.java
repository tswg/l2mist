package core.gameserver.listener.actor.ai;

import core.gameserver.ai.CtrlEvent;
import core.gameserver.listener.AiListener;
import core.gameserver.model.Creature;

public interface OnAiEventListener extends AiListener
{
	public void onAiEvent(Creature actor, CtrlEvent evt, Object[] args);
}
