package core.gameserver.model.entity.events.objects;

import java.util.List;

import core.gameserver.model.Creature;
import core.gameserver.model.Player;
import core.gameserver.model.Zone;
import core.gameserver.model.entity.Reflection;
import core.gameserver.model.entity.events.GlobalEvent;

public class ZoneObject implements InitableObject
{
	private String _name;
	private Zone _zone;

	public ZoneObject(String name)
	{
		_name = name;
	}

	@Override
	public void initObject(GlobalEvent e)
	{
		Reflection r = e.getReflection();

		_zone = r.getZone(_name);
	}

	public void setActive(boolean a)
	{
		_zone.setActive(a);
	}

	public void setActive(boolean a, GlobalEvent event)
	{
		setActive(a);

		//
	}

	public Zone getZone()
	{
		return _zone;
	}

	public List<Player> getInsidePlayers()
	{
		return _zone.getInsidePlayers();
	}

	public boolean checkIfInZone(Creature c)
	{
		return _zone.checkIfInZone(c);
	}
}
