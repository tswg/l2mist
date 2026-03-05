package core.gameserver.model.items;

import core.gameserver.model.pledge.Clan;
import core.gameserver.model.items.ItemInstance.ItemLocation;

public final class ClanWarehouse extends Warehouse
{
	public ClanWarehouse(Clan clan)
	{
		super(clan.getClanId());
	}

	@Override
	public ItemLocation getItemLocation()
	{
		return ItemLocation.CLANWH;
	}
}