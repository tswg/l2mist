package core.gameserver.listener.actor.ai;

import core.gameserver.ai.CtrlIntention;
import core.gameserver.listener.AiListener;
import core.gameserver.model.Creature;

public interface OnAiIntentionListener extends AiListener
{
	public void onAiIntention(Creature actor, CtrlIntention intention, Object arg0, Object arg1);
}
