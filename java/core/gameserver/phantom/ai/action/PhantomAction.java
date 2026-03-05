package core.gameserver.phantom.ai.action;

import core.gameserver.phantom.ai.PhantomContext;

public interface PhantomAction {
    boolean canRun(PhantomContext ctx);
    int priority(PhantomContext ctx);
    void run(PhantomContext ctx);
}