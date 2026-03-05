package core.gameserver.listener.reflection;

import core.commons.listener.Listener;
import core.gameserver.model.entity.Reflection;

public interface OnReflectionCollapseListener extends Listener<Reflection>
{
	public void onReflectionCollapse(Reflection reflection);
}
