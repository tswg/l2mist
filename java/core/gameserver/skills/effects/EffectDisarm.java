package core.gameserver.skills.effects;

import core.gameserver.model.Effect;
import core.gameserver.model.Player;
import core.gameserver.model.items.ItemInstance;
import core.gameserver.stats.Env;

public final class EffectDisarm extends Effect
{
	public EffectDisarm(Env env, EffectTemplate template)
	{
		super(env, template);
	}

	@Override
	public boolean checkCondition()
	{
		if(!_effected.isPlayer())
			return false;
		Player player = _effected.getPlayer();
		// Нельзя снимать/одевать проклятое оружие и флаги
		if(player.isCursedWeaponEquipped() || player.getActiveWeaponFlagAttachment() != null)
			return false;
		if(player._event != null)
		{
			ItemInstance wpn = player.getActiveWeaponInstance();
			if(wpn != null && (wpn.getItemId() == 13560 || wpn.getItemId() == 13561))
				return false;
		}
		return super.checkCondition();
	}

	@Override
	public void onStart()
	{
		super.onStart();
		Player player = (Player) _effected;

		ItemInstance wpn = player.getActiveWeaponInstance();
		if(wpn != null)
		{
			player.getInventory().unEquipItem(wpn);
			player.sendDisarmMessage(wpn);
		}
		player.startWeaponEquipBlocked();
	}

	@Override
	public void onExit()
	{
		super.onExit();
		_effected.stopWeaponEquipBlocked();
	}

	@Override
	public boolean onActionTime()
	{
		// just stop this effect
		return false;
	}
}