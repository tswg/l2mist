package core.gameserver.model.entity.events.actions;

import core.gameserver.model.entity.events.EventAction;
import core.gameserver.model.entity.events.GlobalEvent;

public class AnnounceAction implements EventAction
{
	private int _id;

	public AnnounceAction(int id)
	{
		_id = id;
	}

	@Override
	public void call(GlobalEvent event)
	{
		event.announce(_id);
	}
}
