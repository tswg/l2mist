package core.gameserver.model.phantom;

import core.gameserver.utils.Location;

public class PhantomProfile
{
	private final int objectId;
	private final String name;
	private final String title;
	private final int clanId;
	private final Location origin;

	public PhantomProfile(int objectId, String name, String title, int x, int y, int z, int clanId)
	{
		this.objectId = objectId;
		this.name = name;
		this.title = title;
		this.clanId = clanId;
		origin = new Location(x, y, z);
	}

	public int getObjectId()
	{
		return objectId;
	}

	public String getName()
	{
		return name;
	}

	public String getTitle()
	{
		return title;
	}

	public int getClanId()
	{
		return clanId;
	}

	public Location getOrigin()
	{
		return origin;
	}
}
