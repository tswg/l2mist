package core.gameserver.skills.effects;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

import core.commons.threading.RunnableImpl;
import core.commons.util.Rnd;
import core.gameserver.Config;
import core.gameserver.ThreadPoolManager;
import core.gameserver.ai.CtrlEvent;
import core.gameserver.data.xml.holder.CubicHolder;
import core.gameserver.model.Creature;
import core.gameserver.model.Effect;
import core.gameserver.model.GameObject;
import core.gameserver.model.Player;
import core.gameserver.model.Skill;
import core.gameserver.network.l2.s2c.MagicSkillLaunched;
import core.gameserver.network.l2.s2c.MagicSkillUse;
import core.gameserver.stats.Env;
import core.gameserver.stats.Formulas;
import core.gameserver.templates.CubicTemplate;

public class EffectCubic extends Effect
{
	private class ActionTask extends RunnableImpl
	{
		@Override
		public void runImpl() throws Exception
		{
			if(!isActive())
				return;

			Player player = _effected != null && _effected.isPlayer() ? (Player) _effected : null;
			if(player == null)
				return;

			doAction(player);
		}
	}

	private final CubicTemplate _template;
	private Future<?> _task = null;

	public EffectCubic(Env env, EffectTemplate template)
	{
		super(env, template);
		_template = CubicHolder.getInstance().getTemplate(getTemplate().getParam().getInteger("cubicId"), getTemplate().getParam().getInteger("cubicLevel"));
	}

	@Override
	public void onStart()
	{
		super.onStart();
		Player player = _effected.getPlayer();
		if(player == null)
			return;

		player.addCubic(this);
		_task = ThreadPoolManager.getInstance().scheduleAtFixedRate(new ActionTask(), 1000L, 1000L);
	}

	@Override
	public void onExit()
	{
		super.onExit();
		Player player = _effected.getPlayer();
		if(player == null)
			return;

		player.removeCubic(getId());
		_task.cancel(true);
		_task = null;
	}

	public void doAction(Player player)
	{
		for(Map.Entry<Integer, List<CubicTemplate.SkillInfo>> entry : _template.getSkills())
			if(Rnd.chance(entry.getKey()))
			{
				for(CubicTemplate.SkillInfo skillInfo : entry.getValue())
				{
					if(player.isSkillDisabled(skillInfo.getSkill()))
						continue;
					if(_effected.getPlayer().isInCombat() || _effected.getPlayer().isAttackingNow() || _effected.getPlayer().isInDuel() || _effected.getPlayer().isInOlympiadMode())
					switch(skillInfo.getActionType())
					{
						case ATTACK:
							if(_effected.getPlayer().isInCombat() || _effected.getPlayer().isAttackingNow() || _effected.getPlayer().isInDuel() || _effected.getPlayer().isInOlympiadMode())
								doAttack(player, skillInfo, _template.getDelay());
							break;
						case DEBUFF:
							if(_effected.getPlayer().isInCombat() || _effected.getPlayer().isAttackingNow() || _effected.getPlayer().isInDuel() || _effected.getPlayer().isInOlympiadMode())
								doDebuff(player, skillInfo, _template.getDelay());
							break;
						case HEAL:
							doHeal(player, skillInfo, _template.getDelay());
							break;
						case CANCEL:
							doCancel(player, skillInfo, _template.getDelay());
							break;
					}
				}
				break;
			}
	}

	@Override
	protected boolean onActionTime()
	{
		return false;
	}

	@Override
	public boolean isHidden()
	{
		return true;
	}

	@Override
	public boolean isCancelable()
	{
		return false;
	}

	public int getId()
	{
		return _template.getId();
	}

	private static void doHeal(final Player player, CubicTemplate.SkillInfo info, final int delay)
	{
		final Skill skill = info.getSkill();
		Creature target = null;
		if(player.getParty() == null)
		{
			if(!player.isCurrentHpFull() && !player.isDead())
				target = player;
		}
		else
		{
			double currentHp = Integer.MAX_VALUE;
			for(Player member : player.getParty().getPartyMembers())
			{
				if(member == null)
					continue;

				if(player.isInRange(member, info.getSkill().getCastRange()) && !member.isCurrentHpFull() && !member.isDead() && member.getCurrentHp() < currentHp)
				{
					currentHp = member.getCurrentHp();
					target = member;
				}
			}
		}

		if(target == null)
			return;

		int chance = info.getChance((int) target.getCurrentHpPercents());

		if(!Rnd.chance(chance))
			return;

		final Creature aimTarget = target;
		player.broadcastPacket(new MagicSkillUse(player, aimTarget, skill.getDisplayId(), skill.getDisplayLevel(), skill.getHitTime(), 0));
		player.disableSkill(skill, delay * 1000L);
		ThreadPoolManager.getInstance().schedule(new RunnableImpl()
		{
			@Override
			public void runImpl() throws Exception
			{
				final List<Creature> targets = new ArrayList<Creature>(1);
				targets.add(aimTarget);
				player.broadcastPacket(new MagicSkillLaunched(player.getObjectId(), skill.getDisplayId(), skill.getDisplayLevel(), targets));
				player.callSkill(skill, targets, false);
			}
		}, skill.getHitTime());
	}

	private static void doAttack(final Player player, final CubicTemplate.SkillInfo info, final int delay)
	{
		if(!Rnd.chance(info.getChance()))
			return;

		final Skill skill = info.getSkill();
		Creature target = null;
		if(player.isInCombat())
		{
			GameObject object = player.getTarget();
			target = object != null && object.isCreature() ? (Creature) object : null;
		}
		if(target == null || target.isDead() || (target.isDoor() && !info.isCanAttackDoor()) || !player.isInRangeZ(target, skill.getCastRange()) || !target.isAutoAttackable(player))
			return;
		if(target.isPlayer() && target.getPvpFlag() == 0 && !target.getPlayer().isInOlympiadMode())
			return;
		final Creature aimTarget = target;
		player.broadcastPacket(new MagicSkillUse(player, target, skill.getDisplayId(), skill.getDisplayLevel(), skill.getHitTime(), 0));
		player.disableSkill(skill, delay * 1000L);
		ThreadPoolManager.getInstance().schedule(new RunnableImpl()
		{
			@Override
			public void runImpl() throws Exception
			{
				final List<Creature> targets = new ArrayList<Creature>(1);
				targets.add(aimTarget);

				player.broadcastPacket(new MagicSkillLaunched(player.getObjectId(), skill.getDisplayId(), skill.getDisplayLevel(), targets));
				player.callSkill(skill, targets, false);

				if(aimTarget.isNpc())
					if(aimTarget.paralizeOnAttack(player))
					{
						if(Config.PARALIZE_ON_RAID_DIFF)
							player.paralizeMe(aimTarget);
					}
					else
					{
						int damage = skill.getEffectPoint() != 0 ? skill.getEffectPoint() : (int) skill.getPower();
						aimTarget.getAI().notifyEvent(CtrlEvent.EVT_ATTACKED, player, damage);
					}
			}
		}, skill.getHitTime());
	}

	private static void doDebuff(final Player player, final CubicTemplate.SkillInfo info, final int delay)
	{
		if(!Rnd.chance(info.getChance()))
			return;

		final Skill skill = info.getSkill();
		Creature target = null;
		if(player.isInCombat())
		{
			GameObject object = player.getTarget();
			target = object != null && object.isCreature() ? (Creature) object : null;
		}
		if(target == null || target.isDead() || (target.isDoor() && !info.isCanAttackDoor()) || !player.isInRangeZ(target, skill.getCastRange()) || !target.isAutoAttackable(player))
			return;
		if(target.isPlayer() && target.getPvpFlag() == 0 && !target.getPlayer().isInOlympiadMode())
			return;
		final Creature aimTarget = target;
		player.broadcastPacket(new MagicSkillUse(player, target, skill.getDisplayId(), skill.getDisplayLevel(), skill.getHitTime(), 0));
		player.disableSkill(skill, delay * 1000L);
		ThreadPoolManager.getInstance().schedule(new RunnableImpl()
		{
			@Override
			public void runImpl() throws Exception
			{
				final List<Creature> targets = new ArrayList<Creature>(1);
				targets.add(aimTarget);
				player.broadcastPacket(new MagicSkillLaunched(player.getObjectId(), skill.getDisplayId(), skill.getDisplayLevel(), targets));
				final boolean succ = Formulas.calcSkillSuccess(player, aimTarget, skill, info.getChance());
				if(succ)
					player.callSkill(skill, targets, false);

				if(aimTarget.isNpc())
					if(aimTarget.paralizeOnAttack(player))
					{
						if(Config.PARALIZE_ON_RAID_DIFF)
							player.paralizeMe(aimTarget);
					}
					else
					{
						int damage = skill.getEffectPoint() != 0 ? skill.getEffectPoint() : (int) skill.getPower();
						aimTarget.getAI().notifyEvent(CtrlEvent.EVT_ATTACKED, player, damage);
					}
			}
		}, skill.getHitTime());
	}

	private static void doCancel(final Player player, final CubicTemplate.SkillInfo info, final int delay)
	{
		if(!Rnd.chance(info.getChance()))
			return;

		final Skill skill = info.getSkill();
		boolean hasDebuff = false;
		for(Effect e : player.getEffectList().getAllEffects())
			if(e != null && e.isOffensive() && e.isCancelable() && !e.getTemplate()._applyOnCaster)
				hasDebuff = true;

		if(!hasDebuff)
			return;

		player.broadcastPacket(new MagicSkillUse(player, player, skill.getDisplayId(), skill.getDisplayLevel(), skill.getHitTime(), 0));
		player.disableSkill(skill, delay * 1000L);
		ThreadPoolManager.getInstance().schedule(new RunnableImpl()
		{
			@Override
			public void runImpl() throws Exception
			{
				final List<Creature> targets = new ArrayList<Creature>(1);
				targets.add(player);
				player.broadcastPacket(new MagicSkillLaunched(player.getObjectId(), skill.getDisplayId(), skill.getDisplayLevel(), targets));
				player.callSkill(skill, targets, false);
			}
		}, skill.getHitTime());
	}
}
