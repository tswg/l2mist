package core.gameserver.handler.beautyshop;

import core.gameserver.model.Player;
import core.gameserver.network.l2.components.SystemMsg;

public class BeautyShopWear
{
	public static void BeautyShopValidation(final Player player)
	{
		player.setWearFace(-1);
		player.setWearHairStyle(-1);
		player.setWearHairColor(-1);
		player.unsetBeautyShopWearing();
		if(player.getSchedulePlayerBeautyShopWearing() != null)
			player.resetSchedulePlayerBeautyShopWearing();
		player.sendPacket(SystemMsg.YOU_ARE_NO_LONGER_TRYING_ON_EQUIPMENT_);
		player.sendUserInfo(true);
	}
}
