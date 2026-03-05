package core.gameserver.model.instances;

import core.commons.lang.reference.HardReference;
import core.gameserver.idfactory.IdFactory;
import core.gameserver.model.GameObject;
import core.gameserver.model.Player;
import core.gameserver.model.reference.L2Reference;
import core.gameserver.network.l2.s2c.MyTargetSelected;

public class ControlKeyInstance extends GameObject
{
	protected HardReference<ControlKeyInstance> reference;

	public ControlKeyInstance()
	{
		super(IdFactory.getInstance().getNextId());
		reference = new L2Reference<ControlKeyInstance>(this);
	}

	@Override
	public HardReference<ControlKeyInstance> getRef()
	{
		return reference;
	}

	@Override
	public void onAction(Player player, boolean shift)
	{
		if(player.getTarget() != this)
		{
			player.setTarget(this);
			player.sendPacket(new MyTargetSelected(getObjectId(), 0));
			return;
		}

		player.sendActionFailed();
	}
}
