package core.gameserver.listener.event;

import core.gameserver.listener.EventListener;
import core.gameserver.model.entity.events.GlobalEvent;

public interface OnStartStopListener extends EventListener
{
	void onStart(GlobalEvent event);

	void onStop(GlobalEvent event);
}
