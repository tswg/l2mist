package core.gameserver.phantom;

import core.gameserver.model.PhantomSpot;
import core.gameserver.model.Player;
import core.gameserver.model.phantom.PhantomProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Legacy compatibility wrapper.
 *
 * <p>The active phantom implementation lives in {@code core.gameserver.model.phantom}
 * services. This class is kept only so old references can compile.</p>
 */
public final class PhantomSpawner
{
	private static final Logger LOG = LoggerFactory.getLogger(PhantomSpawner.class);

	public void prepare()
	{
		LOG.warn("[PHANTOM] Legacy PhantomSpawner.prepare() called; no-op.");
	}

	public Player spawn(PhantomSpot spot, PhantomProfile profile)
	{
		throw new UnsupportedOperationException("Legacy core.gameserver.phantom.PhantomSpawner is deprecated. Use PhantomSpawnerService.");
	}
}
