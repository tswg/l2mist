package core.gameserver.phantom;

import core.gameserver.phantom.ai.PhantomContext;

public interface PhantomAction {
    boolean canRun(PhantomContext ctx);
    void run(PhantomContext ctx);
    int priority(); // чем больше — тем важнее
}