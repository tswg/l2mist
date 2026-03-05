package core.gameserver.model.items.listeners;

import core.gameserver.listener.inventory.OnEquipListener;
import core.gameserver.model.Playable;
import core.gameserver.model.Player;
import core.gameserver.model.items.Inventory;
import core.gameserver.model.items.ItemInstance;
import core.gameserver.templates.item.WeaponTemplate.WeaponType;

public final class BowListener implements OnEquipListener
{
	private static final BowListener _instance = new BowListener();

	public static BowListener getInstance()
	{
		return _instance;
	}

	@Override
	public void onUnequip(int slot, ItemInstance item, Playable actor)
	{
		if(!item.isEquipable() || slot != Inventory.PAPERDOLL_RHAND)
			return;

		Player player = (Player)actor;

		if(item.getItemType() == WeaponType.BOW || item.getItemType() == WeaponType.CROSSBOW || item.getItemType() == WeaponType.ROD)
			player.getInventory().setPaperdollItem(Inventory.PAPERDOLL_LHAND, null);
	}

	@Override
	public void onEquip(int slot, ItemInstance item, Playable actor)
	{
		if(!item.isEquipable() || slot != Inventory.PAPERDOLL_RHAND)
			return;

		Player player = (Player)actor;

		if(item.getItemType() == WeaponType.BOW)
		{
			ItemInstance arrow = player.getInventory().findArrowForBow(item.getTemplate());
			if(arrow != null)
				player.getInventory().setPaperdollItem(Inventory.PAPERDOLL_LHAND, arrow);
		}
		if(item.getItemType() == WeaponType.CROSSBOW)
		{
			ItemInstance bolt = player.getInventory().findArrowForCrossbow(item.getTemplate());
			if(bolt != null)
				player.getInventory().setPaperdollItem(Inventory.PAPERDOLL_LHAND, bolt);
		}
		if(item.getItemType() == WeaponType.ROD)
		{
			ItemInstance bait = player.getInventory().findEquippedLure();
			if(bait != null)
				player.getInventory().setPaperdollItem(Inventory.PAPERDOLL_LHAND, bait);
		}
	}
}