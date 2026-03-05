package core.gameserver.model.instances;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledFuture;

import core.commons.threading.RunnableImpl;
import core.commons.util.Rnd;
import core.gameserver.ThreadPoolManager;
import core.gameserver.model.Creature;
import core.gameserver.model.GameObjectTasks;
import core.gameserver.model.Player;
import core.gameserver.model.Skill;
import core.gameserver.model.pledge.Clan;
import core.gameserver.taskmanager.EffectTaskManager;
import core.gameserver.templates.npc.NpcTemplate;

public class SymbolInstance extends NpcInstance
{
	private final Creature _owner;
	private final Skill _skill;
	private ScheduledFuture<?> _targetTask;
	private ScheduledFuture<?> _destroyTask;

	public SymbolInstance(int objectId, NpcTemplate template, Creature owner, Skill skill)
	{
		super(objectId, template);
		_owner = owner;
		_skill = skill;

		setReflection(owner.getReflection());
		setLevel(owner.getLevel());
		setTitle(owner.getName());
	}

	public Creature getOwner()
	{
		return _owner;
	}

	@Override
	protected void onSpawn()
	{
		super.onSpawn();

		_destroyTask = ThreadPoolManager.getInstance().schedule(new GameObjectTasks.DeleteTask(this), 120000L);

		_targetTask = EffectTaskManager.getInstance().scheduleAtFixedRate(new RunnableImpl(){

			@Override
			public void runImpl() throws Exception
			{
				for(Creature target : getAroundCharacters(200, 200))
					if(_skill.checkTarget(_owner, target, null, false, false) == null)
					{
						List<Creature> targets = new ArrayList<Creature>();
						if(!_skill.isAoE() && _skill.checkTargetSkill(target, _owner, false))
							targets.add(target);
						else
							for(Creature t : getAroundCharacters(_skill.getSkillRadius(), 128))
								if((_skill.checkTarget(_owner, t, null, false, false) == null) && _skill.checkTargetSkill(target, _owner, false))
									targets.add(target);

						_skill.useSkill(SymbolInstance.this, targets);
					}
			}
		}, 1000L, Rnd.get(4000L, 7000L));
	}

	@Override
	protected void onDelete()
	{
		if(_destroyTask != null)
			_destroyTask.cancel(false);
		_destroyTask = null;
		if(_targetTask != null)
			_targetTask.cancel(false);
		_targetTask = null;
		super.onDelete();
	}

	@Override
	public int getPAtk(Creature target)
	{
		Creature owner = getOwner();
		return owner == null ? 0 : owner.getPAtk(target);
	}

	@Override
	public int getMAtk(Creature target, Skill skill)
	{
		Creature owner = getOwner();
		return owner == null ? 0 : owner.getMAtk(target, skill);
	}

	@Override
	public boolean hasRandomAnimation()
	{
		return false;
	}
	
	@Override
	public boolean isAutoAttackable(Creature attacker)
	{
		return false;
	}

	@Override
	public boolean isAttackable(Creature attacker)
	{
		return false;
	}

	@Override
	public boolean isInvul()
	{
		return true;
	}

	@Override
	public boolean isFearImmune()
	{
		return true;
	}

	@Override
	public boolean isParalyzeImmune()
	{
		return true;
	}

	@Override
	public boolean isLethalImmune()
	{
		return true;
	}

	@Override
	public void showChatWindow(Player player, int val, Object... arg)
	{}

	@Override
	public void showChatWindow(Player player, String filename, Object... replace)
	{}

	@Override
	public void onBypassFeedback(Player player, String command)
	{}

	@Override
	public void onAction(Player player, boolean shift)
	{
		player.sendActionFailed();
	}

	@Override
	public Clan getClan()
	{
		return null;
	}
}
