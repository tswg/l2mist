package core.gameserver.utils;

import core.gameserver.data.xml.holder.ResidenceHolder;
import core.gameserver.model.Player;
import core.gameserver.model.entity.residence.Residence;
import core.gameserver.model.entity.residence.ResidenceType;
import core.gameserver.tables.SkillTable;

public class SiegeUtils
{
	public static void addSiegeSkills(Player character)
	{
		character.addSkill(SkillTable.getInstance().getInfo(246, 1), false);
		character.addSkill(SkillTable.getInstance().getInfo(247, 1), false);
		if(character.isNoble())
			character.addSkill(SkillTable.getInstance().getInfo(326, 1), false);

		if(character.getClan() != null && character.getClan().getCastle() > 0)
		{
			character.addSkill(SkillTable.getInstance().getInfo(844, 1), false);
			character.addSkill(SkillTable.getInstance().getInfo(845, 1), false);
		}
	}

	public static void removeSiegeSkills(Player character)
	{
		character.removeSkill(SkillTable.getInstance().getInfo(246, 1), false);
		character.removeSkill(SkillTable.getInstance().getInfo(247, 1), false);
		character.removeSkill(SkillTable.getInstance().getInfo(326, 1), false);

		if(character.getClan() != null && character.getClan().getCastle() > 0)
		{
			character.removeSkill(SkillTable.getInstance().getInfo(844, 1), false);
			character.removeSkill(SkillTable.getInstance().getInfo(845, 1), false);
		}
	}

	public static boolean getCanRide()
	{
		for(Residence residence : ResidenceHolder.getInstance().getResidences())
			if(residence != null && residence.getSiegeEvent().isInProgress() && residence.getType() != ResidenceType.ClanHall)
				return false;
		return true;
	}
}