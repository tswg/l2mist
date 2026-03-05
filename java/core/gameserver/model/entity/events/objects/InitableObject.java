package core.gameserver.model.entity.events.objects;

import java.io.Serializable;

import core.gameserver.model.entity.events.GlobalEvent;

public interface InitableObject extends Serializable
{
	void initObject(GlobalEvent e);
}
