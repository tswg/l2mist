package core.gameserver.model.phantom;

import org.apache.log4j.Logger;

public class PhantomDebugService
{
	private final Logger log;

	public PhantomDebugService(Logger log)
	{
		this.log = log;
	}

	public void info(String message)
	{
		log.info(message);
	}

	public void warn(String message, Throwable e)
	{
		log.warn(message, e);
	}
}
