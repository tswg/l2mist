package core.gameserver.model.items.listeners;

import core.gameserver.listener.inventory.OnEquipListener;
import core.gameserver.model.Playable;
import core.gameserver.model.Player;
import core.gameserver.model.Skill;
import core.gameserver.model.items.ItemInstance;
import core.gameserver.network.l2.s2c.SkillCoolTime;
import core.gameserver.network.l2.s2c.SkillList;
import core.gameserver.stats.Formulas;
import core.gameserver.tables.SkillTable;
import core.gameserver.templates.item.ItemTemplate;

public final class ItemSkillsListener implements OnEquipListener
{
	private static final ItemSkillsListener _instance = new ItemSkillsListener();

	public static ItemSkillsListener getInstance()
	{
		return _instance;
	}

	@Override
	public void onUnequip(int slot, ItemInstance item, Playable actor)
	{
		Player player = (Player)actor;

		Skill[] itemSkills = null;
		Skill enchant4Skill = null;

		ItemTemplate it = item.getTemplate();

		itemSkills = it.getAttachedSkills();

		enchant4Skill = it.getEnchant4Skill();

		player.removeTriggers(it);

		if(itemSkills != null && itemSkills.length > 0)
			for(Skill itemSkill : itemSkills)
				if(itemSkill.getId() >= 26046 && itemSkill.getId() <= 26048)
				{
					int level = player.getSkillLevel(itemSkill.getId());
					int newlevel = level - 1;
					if(newlevel > 0)
						player.addSkill(SkillTable.getInstance().getInfo(itemSkill.getId(), newlevel), false);
					else
						player.removeSkillById(itemSkill.getId());
				}
				else
				{
					player.removeSkill(itemSkill, false);
				}

		if(enchant4Skill != null)
			player.removeSkill(enchant4Skill, false);

		if(itemSkills.length > 0 || enchant4Skill != null)
		{
			player.sendPacket(new SkillList(player));
			player.updateStats();
		}
	}

	@Override
	public void onEquip(int slot, ItemInstance item, Playable actor)
	{
		Player player = (Player)actor;

		Skill[] itemSkills = null;
		Skill enchant4Skill = null;

		ItemTemplate it = item.getTemplate();

		itemSkills = it.getAttachedSkills();

		if(item.getEnchantLevel() >= 4)
			enchant4Skill = it.getEnchant4Skill();

		// Для оружия при несоотвествии грейда скилы не выдаем
		if(it.getType2() == ItemTemplate.TYPE2_WEAPON && player.getWeaponsExpertisePenalty() > 0)
			return;

		player.addTriggers(it);

		boolean needSendInfo = false;
		if(itemSkills.length > 0)
			for(Skill itemSkill : itemSkills)
				if(itemSkill.getId() >= 26046 && itemSkill.getId() <= 26048)
				{
					int level = player.getSkillLevel(itemSkill.getId());
					int newlevel = level;
					if(level > 0)
					{
						if(SkillTable.getInstance().getInfo(itemSkill.getId(), level + 1) != null)
							newlevel = level + 1;
					}
					else
						newlevel = 1;
					if(newlevel != level)
					{
						player.addSkill(SkillTable.getInstance().getInfo(itemSkill.getId(), newlevel), false);
					}
				}
				else if(player.getSkillLevel(itemSkill.getId()) < itemSkill.getLevel())
				{
					player.addSkill(itemSkill, false);

					if(itemSkill.isActive())
					{
						long reuseDelay = Formulas.calcSkillReuseDelay(player, itemSkill);
						reuseDelay = Math.min(reuseDelay, 30000);

						if(reuseDelay > 0 && !player.isSkillDisabled(itemSkill))
						{
							player.disableSkill(itemSkill, reuseDelay);
							needSendInfo = true;
						}
					}
				}

		if(enchant4Skill != null)
			player.addSkill(enchant4Skill, false);

		if(itemSkills.length > 0 || enchant4Skill != null)
		{
			player.sendPacket(new SkillList(player));
			player.updateStats();
			if (needSendInfo)
				player.sendPacket(new SkillCoolTime(player));
		}
	}
}