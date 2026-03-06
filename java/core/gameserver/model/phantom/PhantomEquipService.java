package core.gameserver.model.phantom;

import java.util.List;

import core.commons.util.Rnd;
import core.gameserver.Config;
import core.gameserver.model.Player;
import core.gameserver.model.items.ItemInstance;
import core.gameserver.templates.PlayerTemplate;
import core.gameserver.templates.item.CreateItem;
import core.gameserver.templates.item.ItemTemplate;
import core.gameserver.utils.ItemFunctions;

public class PhantomEquipService
{
	private final List<PhantomSet> sets;

	public PhantomEquipService(List<PhantomSet> sets)
	{
		this.sets = sets;
	}

	public int randomLevelByGrade(PhantomSet set)
	{
		int grade = set.grade;
		if(grade == 0)
			return Rnd.get(1, 19);
		if(grade == 1)
			return Rnd.get(20, 39);
		if(grade == 2)
			return Rnd.get(40, 51);
		if(grade == 3)
			return Rnd.get(52, 60);
		if(grade == 4)
			return Rnd.get(61, 75);
		if(grade == 5)
			return Rnd.get(76, 80);
		return 1;
	}

	public PhantomSet randomSet()
	{
		if(sets.isEmpty())
			return new PhantomSet(0, 0, 0, 0, 0, 0, 0);
		return sets.get(Rnd.get(sets.size() - 1));
	}

	public void equip(Player phantom, PhantomSet set)
	{
		if(Config.ALLOW_PHANTOM_SETS)
		{
			equipItem(phantom, set.body);
			equipItem(phantom, set.gaiters);
			equipItem(phantom, set.gloves);
			equipItem(phantom, set.boots);
			equipItem(phantom, set.weapon);
			if(phantom.getActiveWeaponInstance() != null)
				return;
		}
		equipFromTemplate(phantom);
	}

	private void equipItem(Player phantom, int itemId)
	{
		if(itemId == 0)
			return;
		ItemInstance item = ItemFunctions.createItem(itemId);
		phantom.getInventory().addItem(item);
		phantom.getInventory().equipItem(item);
	}

	private void equipFromTemplate(Player phantom)
	{
		PlayerTemplate template = phantom.getTemplate();
		for(CreateItem i : template.getItems())
		{
			ItemInstance item = ItemFunctions.createItem(i.getItemId());
			phantom.getInventory().addItem(item);
			if(i.isEquipable() && item.isEquipable() && (phantom.getActiveWeaponItem() == null || item.getTemplate().getType2() != ItemTemplate.TYPE2_WEAPON))
				phantom.getInventory().equipItem(item);
		}
	}
}
