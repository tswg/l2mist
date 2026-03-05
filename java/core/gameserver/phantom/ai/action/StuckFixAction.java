package core.gameserver.phantom.ai.action;

import core.gameserver.model.Player;
import core.gameserver.phantom.PhantomAdapter;
import core.gameserver.phantom.PhantomConfig;
import core.gameserver.phantom.ai.PhantomContext;

public class StuckFixAction implements PhantomAction {
    @Override
    public boolean canRun(PhantomContext ctx) {
        Player p = ctx.actor;

        long now = System.currentTimeMillis();
        long dt = now - ctx.bot.lastMoveTs;
        if (dt < PhantomConfig.STUCK_CHECK_SEC * 1000L) return false;

        int x = PhantomAdapter.x(p);
		int y = PhantomAdapter.y(p);
		int z = PhantomAdapter.z(p);
        long dx = (long)x - ctx.bot.lastX;
        long dy = (long)y - ctx.bot.lastY;
        long dz = (long)z - ctx.bot.lastZ;
        long d2 = dx*dx + dy*dy + dz*dz;

        // почти не двигался
        return d2 < 35L * 35L;
    }

    @Override
    public int priority(PhantomContext ctx) { return 900; }

    @Override
    public void run(PhantomContext ctx) {
        Player p = ctx.actor;

        // обновить трекинг
        ctx.bot.lastX = PhantomAdapter.x(p);
        ctx.bot.lastY = PhantomAdapter.y(p);
        ctx.bot.lastZ = PhantomAdapter.z(p);
        ctx.bot.lastMoveTs = System.currentTimeMillis();

        // “пинок”: вернуться ближе к центру спота (или телепорт если хочешь)
        PhantomAdapter.moveTo(p,
                PhantomAdapter.loc(ctx.bot.spot.centerX, ctx.bot.spot.centerY, ctx.bot.spot.centerZ));
    }
}
