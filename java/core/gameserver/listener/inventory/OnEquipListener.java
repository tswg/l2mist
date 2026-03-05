package core.gameserver.listener.inventory;

import core.commons.listener.Listener;
import core.gameserver.model.Playable;
import core.gameserver.model.items.ItemInstance;

public interface OnEquipListener extends Listener<Playable>
{
	public void onEquip(int slot, ItemInstance item, Playable actor);

	public void onUnequip(int slot, ItemInstance item, Playable actor);
}
