package core.gameserver.phantom.ai;

import core.gameserver.phantom.model.PhantomBot;
import core.gameserver.phantom.ai.action.*;

import java.util.ArrayList;
import java.util.List;

public class PhantomBrain {
	private final List<PhantomAction> actions = new ArrayList<PhantomAction>();

	public PhantomBrain()
	{
		actions.add(new RetreatAction());
		actions.add(new UsePotionsAction());
		actions.add(new StuckFixAction());

		actions.add(new FindTargetAction());

		actions.add(new UseShotsAction());
		actions.add(new MoveToTargetAction());
		actions.add(new AttackAction());

		actions.add(new RoamAction());
	}

    public void tick(PhantomBot bot) {
        PhantomContext ctx = PhantomContext.from(bot);

        PhantomAction best = null;
        int bestPr = Integer.MIN_VALUE;
        for (PhantomAction a : actions) {
            if (!a.canRun(ctx)) continue;
            int pr = a.priority(ctx);
            if (pr > bestPr) { bestPr = pr; best = a; }
        }

        if (best != null)
            best.run(ctx);

        ctx.syncBack();
    }
}
