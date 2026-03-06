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
        int valid = 0;

        for (NpcInstance n : mobs) {
            if (!PhantomAdapter.isValidFarmTarget(p, n)) {
                if (_log.isDebugEnabled())
                    _log.debug("[PHANTOM][targeting] actor={} reject npc={} reason=invalidFarmTarget", p.getName(), n != null ? n.getName() : "null");
                continue;
            }

            valid++;
            double d = PhantomAdapter.dist3D(p, n);
            if (d < 1) d = 1;

            // скоринг: ближе = лучше, можно добавить “не в бою”, “не в пачке” и т.д.
            double score = 10000.0 / d;

            if (score > bestScore) {
                bestScore = score;
                best = n;
            }
        }

        if (_log.isDebugEnabled()) {
            _log.debug("[PHANTOM][targeting] actor={} around={} valid={} selected={} score={}",
                    p.getName(), mobs.size(), valid, best != null ? best.getName() : "null", bestScore);
        }

        // safety: если после всех проверок цель не выбрана, но список есть — берем ближайшую первую
        if (best == null && !mobs.isEmpty()) {
            best = mobs.get(0);
            if (_log.isDebugEnabled())
                _log.debug("[PHANTOM][targeting] actor={} fallbackTarget={} cause=noBestFromFilter", p.getName(), best.getName());
        }

        return best;
    }
}
