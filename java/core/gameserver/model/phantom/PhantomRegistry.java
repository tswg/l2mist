package core.gameserver.model.phantom;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import core.gameserver.model.Player;

public class PhantomRegistry
{
	private final Map<Integer, PhantomProfile> profiles = new ConcurrentHashMap<Integer, PhantomProfile>();
	private final Map<Integer, ConcurrentLinkedQueue<Player>> waves = new ConcurrentHashMap<Integer, ConcurrentLinkedQueue<Player>>();
	private final Map<Integer, ConcurrentLinkedQueue<Integer>> clanLists = new ConcurrentHashMap<Integer, ConcurrentLinkedQueue<Integer>>();

	public PhantomRegistry()
	{
		waves.put(Integer.valueOf(1), new ConcurrentLinkedQueue<Player>());
		waves.put(Integer.valueOf(2), new ConcurrentLinkedQueue<Player>());
	}

	public void registerProfile(PhantomProfile profile)
	{
		profiles.put(Integer.valueOf(profile.getObjectId()), profile);
	}

	public PhantomProfile getProfile(int objectId)
	{
		return profiles.get(Integer.valueOf(objectId));
	}

	public Map<Integer, PhantomProfile> getProfiles()
	{
		return profiles;
	}

	public ConcurrentLinkedQueue<Player> getWave(int wave)
	{
		return waves.get(Integer.valueOf(wave));
	}

	public Map<Integer, ConcurrentLinkedQueue<Player>> getWaves()
	{
		return waves;
	}

	public Map<Integer, ConcurrentLinkedQueue<Integer>> getClanLists()
	{
		return clanLists;
	}
}
