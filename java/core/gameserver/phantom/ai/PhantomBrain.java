package core.gameserver.phantom.ai;

import core.gameserver.phantom.AttackAction;
import core.gameserver.phantom.FindTargetAction;
import core.gameserver.phantom.PhantomAction;
import core.gameserver.phantom.RetreatAction;
import core.gameserver.phantom.model.PhantomBot;
import core.gameserver.phantom.ai.action.*;

import java.util.List;

public class PhantomBrain {
    private final List<PhantomAction> actions = List.of(
            new RetreatAction(),
            new UsePotionsAction(),
            new StuckFixAction(),

            new FindTargetAction(),

            new UseShotsAction(),
            new MoveToTargetAction(),
            new AttackAction(),

            new RoamAction()
    );

    public void tick(PhantomBot bot) {
        PhantomContext ctx = PhantomContext.from(bot);

        PhantomAction best = null;
        int bestPr = Integer.MIN_VALUE;
        for (PhantomAction a : actions) {
            if (!a.canRun(ctx)) continue;
            int pr = a.priority(ctx);
            if (pr > bestPr) { bestPr = pr; best = a; }
        }
        if (best != null) best.run(ctx);
    }
}