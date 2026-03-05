package core.gameserver.taskmanager.tasks;

import core.gameserver.model.GameObjectsStorage;
import core.gameserver.model.Player;
import core.gameserver.taskmanager.Task;
import core.gameserver.taskmanager.TaskManager;
import core.gameserver.taskmanager.TaskManager.ExecutedTask;
import core.gameserver.taskmanager.TaskTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TaskNevitSystem extends Task
{
	private static final Logger _log = LoggerFactory.getLogger(TaskNevitSystem.class);
	private static final String NAME = "sp_navitsystem";

	@Override
	public String getName()
	{
		return NAME;
	}

	@Override
	public void onTimeElapsed(ExecutedTask task)
	{
		_log.info("Navit System Global Task: launched.");
		for(Player player : GameObjectsStorage.getAllPlayersForIterate())
			player.getNevitSystem().restartSystem();
		_log.info("Navit System Task: completed.");
	}

	@Override
	public void initializate()
	{
		TaskManager.addUniqueTask(NAME, TaskTypes.TYPE_GLOBAL_TASK, "1", "06:30:00", "");
	}
}
