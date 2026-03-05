package core.gameserver.model.items;

import core.gameserver.model.Player;
import core.gameserver.model.items.ItemInstance.ItemLocation;

public class PcWarehouse extends Warehouse
{
	public PcWarehouse(Player owner)
	{
		super(owner.getObjectId());
	}

	public PcWarehouse(int ownerId)
	{
		super(ownerId);
	}

	@Override
	public ItemLocation getItemLocation()
	{
		return ItemLocation.WAREHOUSE;
	}
}