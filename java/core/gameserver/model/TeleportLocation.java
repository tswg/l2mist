package core.gameserver.model;

import core.gameserver.data.xml.holder.ItemHolder;
import core.gameserver.templates.item.ItemTemplate;
import core.gameserver.utils.Location;

public class TeleportLocation extends Location
{
	private static final long serialVersionUID = 1L;
	private final long _price;
	private final ItemTemplate _item;
	private final int _name;
	private final int _castleId;
	private final String _stringName;
	private final String _stringNameLang;

	public TeleportLocation(int item, long price, int name, String StringName, String StringNameLang, int castleId)
	{
		_price = price;
		_name = name;
		_stringName = StringName;
		_stringNameLang = StringNameLang;
		_item = ItemHolder.getInstance().getTemplate(item);
		_castleId = castleId;
	}

	public long getPrice()
	{
		return _price;
	}

	public ItemTemplate getItem()
	{
		return _item;
	}

	public int getName()
	{
		return _name;
	}

	public int getCastleId()
	{
		return _castleId;
	}

	public String getStringName()
	{
		return _stringName;
	}

	public String getStringNameLang()
	{
		return _stringNameLang;
	}
}
