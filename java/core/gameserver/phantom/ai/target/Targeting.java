package core.gameserver.phantom.ai.target;

import core.gameserver.model.Player;
import core.gameserver.model.instances.NpcInstance;
import core.gameserver.phantom.PhantomAdapter;
import core.gameserver.phantom.PhantomConfig;

import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Targeting {
    private static final Logger _log = LoggerFactory.getLogger(Targeting.class);

    private Targeting() {}

    public static NpcInstance findBestMob(Player p, int radius) {
        List<NpcInstance> mobs = PhantomAdapter.getAroundMonsters(p, radius);
        int aroundCount = mobs != null ? mobs.size() : 0;
        if (_log.isDebugEnabled() && PhantomConfig.DEBUG)
            _log.debug("[PHANTOM][Targeting] actor={} objectId={} radius={} aroundMonsters={}",
                    p != null ? p.getName() : "null",
                    p != null ? p.getObjectId() : 0,
                    radius,
                    aroundCount);

        NpcInstance best = null;
        double bestScore = -1e18;
        NpcInstance fallback = null;
        double fallbackDist = Double.MAX_VALUE;
        String finalNullReason = "no-candidates";
        List<String> rejectReasons = new ArrayList<String>();

        if (mobs != null) {
            for (NpcInstance n : mobs) {
                StringBuilder reason = new StringBuilder();
                PhantomAdapter.ValidationResult strictResult = PhantomAdapter.validateFarmTarget(p, n, radius, true, reason);
                if (strictResult == PhantomAdapter.ValidationResult.OK) {
                    double d = PhantomAdapter.dist3D(p, n);
                    if (d < 1)
                        d = 1;

                    double score = 10000.0 / d;
                    if (score > bestScore) {
                        bestScore = score;
                        best = n;
                    }
                } else {
                    if (_log.isDebugEnabled() && PhantomConfig.DEBUG)
                        rejectReasons.add((n != null ? n.getName() + "(" + n.getObjectId() + ")" : "null") + "=" + reason.toString());
                }

                // Step B fallback: intentionally relaxed - no LOS and no strict validator,
                // only minimal runtime-safe checks.
                if (n != null && !n.isDead() && n.isVisible() && (n.isMonster() || n.isAttackable(p))) {
                    double fd = PhantomAdapter.dist3D(p, n);
                    if (fd < fallbackDist) {
                        fallbackDist = fd;
                        fallback = n;
                    }
                }
            }
        }

        if (best == null && fallback != null) {
            best = fallback;
            finalNullReason = "strict-rejected-all-used-relaxed-fallback";
            if (_log.isDebugEnabled() && PhantomConfig.DEBUG)
                _log.debug("[PHANTOM][Targeting] actor={} objectId={} fallbackTarget={} reason=no-strict-target",
                        p != null ? p.getName() : "null",
                        p != null ? p.getObjectId() : 0,
                        fallback.getName() + "(" + fallback.getObjectId() + ")");
        } else if (best == null) {
            finalNullReason = aroundCount == 0 ? "around-empty" : "strict-and-relaxed-fallback-failed";
            if (_log.isDebugEnabled() && PhantomConfig.DEBUG)
                _log.debug("[PHANTOM][Targeting] actor={} objectId={} selectedTarget=null reason={} aroundMonsters={} strictRejects={}",
                        p != null ? p.getName() : "null",
                        p != null ? p.getObjectId() : 0,
                        finalNullReason,
                        aroundCount,
                        rejectReasons.size());
        }

        if (_log.isDebugEnabled() && PhantomConfig.DEBUG && !rejectReasons.isEmpty())
            _log.debug("[PHANTOM][Targeting] actor={} objectId={} rejectReasons={}",
                    p != null ? p.getName() : "null",
                    p != null ? p.getObjectId() : 0,
                    rejectReasons);

        if (_log.isDebugEnabled() && PhantomConfig.DEBUG)
            _log.debug("[PHANTOM][Targeting] actor={} objectId={} selectedTarget={}",
                    p != null ? p.getName() : "null",
                    p != null ? p.getObjectId() : 0,
                    best != null ? (best.getName() + "(" + best.getObjectId() + ")") : "null");

        if (aroundCount > 0 && best == null)
            _log.warn("[PHANTOM][TargetingGuard] actor={} objectId={} aroundMonsters={} selectedTarget=null assert-guard reason={}",
                    p != null ? p.getName() : "null",
                    p != null ? p.getObjectId() : 0,
                    aroundCount,
                    finalNullReason);
        return best;
    }

}
