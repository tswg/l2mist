package core.gameserver.phantom.ai.target;

import core.gameserver.model.Player;
import core.gameserver.model.instances.NpcInstance;
import core.gameserver.phantom.PhantomAdapter;

import java.util.List;

public final class Targeting {
    private Targeting() {}

    public static NpcInstance findBestMob(Player p, int radius) {
        List<NpcInstance> mobs = PhantomAdapter.getAroundMonsters(p, radius);

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
        return best;
    }
}
