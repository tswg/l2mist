package core.gameserver.phantom.ai.target;

import core.gameserver.model.Player;
import core.gameserver.model.instances.NpcInstance;
import core.gameserver.phantom.PhantomAdapter;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Targeting {
    private static final Logger _log = LoggerFactory.getLogger(Targeting.class);

    private Targeting() {}

    public static NpcInstance findBestMob(Player p, int radius) {
        List<NpcInstance> mobs = PhantomAdapter.getAroundMonsters(p, radius);
        if (_log.isInfoEnabled())
            _log.info("[PHANTOM][Targeting] actor={} objectId={} radius={} aroundMonsters={}",
                    p != null ? p.getName() : "null",
                    p != null ? p.getObjectId() : 0,
                    radius,
                    mobs != null ? mobs.size() : 0);

        NpcInstance best = null;
        double bestScore = -1e18;

        for (NpcInstance n : mobs) {
            if (!PhantomAdapter.isValidFarmTarget(p, n)) continue;

            double d = PhantomAdapter.dist3D(p, n);
            if (d < 1) d = 1;

            // скоринг: ближе = лучше, можно добавить “не в бою”, “не в пачке” и т.д.
            double score = 10000.0 / d;

            if (score > bestScore) {
                bestScore = score;
                best = n;
            }
        }
        if (_log.isInfoEnabled())
            _log.info("[PHANTOM][Targeting] actor={} objectId={} selectedTarget={}",
                    p != null ? p.getName() : "null",
                    p != null ? p.getObjectId() : 0,
                    best != null ? (best.getName() + "(" + best.getObjectId() + ")") : "null");
        return best;
    }
}
