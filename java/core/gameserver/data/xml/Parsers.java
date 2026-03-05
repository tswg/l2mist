package core.gameserver.data.xml;

import core.gameserver.data.StringHolder;
import core.gameserver.data.htm.HtmCache;
import core.gameserver.data.xml.holder.BuyListHolder;
import core.gameserver.data.xml.holder.MultiSellHolder;
import core.gameserver.data.xml.holder.ProductHolder;
import core.gameserver.data.xml.holder.RecipeHolder;
import core.gameserver.data.xml.parser.AirshipDockParser;
import core.gameserver.data.xml.parser.ArmorSetsParser;
import core.gameserver.data.xml.parser.CharTemplateParser;
import core.gameserver.data.xml.parser.CubicParser;
import core.gameserver.data.xml.parser.DomainParser;
import core.gameserver.data.xml.parser.DoorParser;
import core.gameserver.data.xml.parser.EnchantItemParser;
import core.gameserver.data.xml.parser.EventParser;
import core.gameserver.data.xml.parser.ExtractableItems;
//import core.gameserver.data.xml.parser.FishDataParser;
import core.gameserver.data.xml.parser.HennaParser;
import core.gameserver.data.xml.parser.InstantZoneParser;
import core.gameserver.data.xml.parser.ItemParser;
import core.gameserver.data.xml.parser.NpcParser;
import core.gameserver.data.xml.parser.OptionDataParser;
import core.gameserver.data.xml.parser.PetitionGroupParser;
import core.gameserver.data.xml.parser.ResidenceParser;
import core.gameserver.data.xml.parser.RestartPointParser;
import core.gameserver.data.xml.parser.SkillAcquireParser;
import core.gameserver.data.xml.parser.SoulCrystalParser;
import core.gameserver.data.xml.parser.SpawnParser;
import core.gameserver.data.xml.parser.StaticObjectParser;
import core.gameserver.data.xml.parser.ZoneParser;
import core.gameserver.instancemanager.ReflectionManager;
import core.gameserver.tables.SpawnTable;
import core.gameserver.tables.SkillTable;

public abstract class Parsers
{
	public static void parseAll()
	{
		HtmCache.getInstance().reload();
		StringHolder.getInstance().load();
		//
		SkillTable.getInstance().load(); // - SkillParser.getInstance();
		OptionDataParser.getInstance().load();
		ItemParser.getInstance().load();
		//
		ExtractableItems.getInstance();
		NpcParser.getInstance().load();

		DomainParser.getInstance().load();
		RestartPointParser.getInstance().load();

		StaticObjectParser.getInstance().load();
		DoorParser.getInstance().load();
		ZoneParser.getInstance().load();
		SpawnTable.getInstance();
		SpawnParser.getInstance().load();
		InstantZoneParser.getInstance().load();

		ReflectionManager.getInstance();
		//
		AirshipDockParser.getInstance().load();
		SkillAcquireParser.getInstance().load();
		//
		CharTemplateParser.getInstance().load();
		//
		ResidenceParser.getInstance().load();
		EventParser.getInstance().load();
		// support(cubic & agathion)
		CubicParser.getInstance().load();
		//
		BuyListHolder.getInstance();
		RecipeHolder.getInstance();
		MultiSellHolder.getInstance();
		ProductHolder.getInstance();
		// AgathionParser.getInstance();
		// item support
		HennaParser.getInstance().load();
		EnchantItemParser.getInstance().load();
		SoulCrystalParser.getInstance().load();
		ArmorSetsParser.getInstance().load();
		//FishDataParser.getInstance().load();

		// etc
		PetitionGroupParser.getInstance().load();
	}
}
