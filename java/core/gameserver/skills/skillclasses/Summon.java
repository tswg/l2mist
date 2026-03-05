package core.gameserver.skills.skillclasses;

import java.util.List;

import core.gameserver.ThreadPoolManager;
import core.gameserver.dao.EffectsDAO;
import core.gameserver.data.xml.holder.NpcHolder;
import core.gameserver.idfactory.IdFactory;
import core.gameserver.model.Creature;
import core.gameserver.model.GameObjectTasks;
import core.gameserver.model.Player;
import core.gameserver.model.Skill;
import core.gameserver.model.base.Experience;
import core.gameserver.model.entity.events.impl.SiegeEvent;
import core.gameserver.model.instances.MerchantInstance;
import core.gameserver.model.instances.NpcInstance;
import core.gameserver.model.instances.SummonInstance;
import core.gameserver.model.instances.TrapInstance;
import core.gameserver.network.l2.components.SystemMsg;
import core.gameserver.stats.Stats;
import core.gameserver.stats.funcs.FuncAdd;
import core.gameserver.tables.SkillTable;
import core.gameserver.templates.StatsSet;
import core.gameserver.templates.npc.NpcTemplate;
import core.gameserver.utils.Location;

public class Summon extends Skill
{
	private final SummonType _summonType;

	private final double _expPenalty;
	private final int _itemConsumeIdInTime;
	private final int _itemConsumeCountInTime;
	private final int _itemConsumeDelay;
	private final int _lifeTime;

	private static enum SummonType
	{
		PET,
		SIEGE_SUMMON,
		AGATHION,
		TRAP,
		MERCHANT
	}

	public Summon(StatsSet set)
	{
		super(set);

		_summonType = Enum.valueOf(SummonType.class, set.getString("summonType", "PET").toUpperCase());
		_expPenalty = set.getDouble("expPenalty", 0.f);
		_itemConsumeIdInTime = set.getInteger("itemConsumeIdInTime", 0);
		_itemConsumeCountInTime = set.getInteger("itemConsumeCountInTime", 0);
		_itemConsumeDelay = set.getInteger("itemConsumeDelay", 240) * 1000;
		_lifeTime = set.getInteger("lifeTime", 1200) * 1000;
	}

	@Override
	public boolean checkCondition(Creature activeChar, Creature target, boolean forceUse, boolean dontMove, boolean first)
	{
		Player player = activeChar.getPlayer();
		if(player == null)
			return false;

		if(player.isProcessingRequest())
		{
			player.sendPacket(SystemMsg.PETS_AND_SERVITORS_ARE_NOT_AVAILABLE_AT_THIS_TIME);
			return false;
		}

		switch(_summonType)
		{
			case TRAP:
				if(player.isInZonePeace())
				{
					activeChar.sendPacket(SystemMsg.A_MALICIOUS_SKILL_CANNOT_BE_USED_IN_A_PEACE_ZONE);
					return false;
				}
				break;
			case PET:
			case SIEGE_SUMMON:
				if(player.getPet() != null || player.isMounted())
				{
					player.sendPacket(SystemMsg.YOU_ALREADY_HAVE_A_PET);
					return false;
				}
				break;
			case AGATHION:
				if(player.getAgathionId() > 0 && _npcId != 0)
				{
					player.sendPacket(SystemMsg.AN_AGATHION_HAS_ALREADY_BEEN_SUMMONED);
					return false;
				}
		}

		return super.checkCondition(activeChar, target, forceUse, dontMove, first);
	}

	@Override
	public void useSkill(Creature caster, List<Creature> targets)
	{
		Player activeChar = caster.getPlayer();

		switch(_summonType)
		{
			case AGATHION:
				activeChar.setAgathion(getNpcId());
				break;
			case TRAP:
				Skill trapSkill = getFirstAddedSkill();

				if(activeChar.getTrapsCount() >= 5)
					activeChar.destroyFirstTrap();
				TrapInstance trap = new TrapInstance(IdFactory.getInstance().getNextId(), NpcHolder.getInstance().getTemplate(getNpcId()), activeChar, trapSkill);
				activeChar.addTrap(trap);
				trap.spawnMe();
				break;
			case PET:
			case SIEGE_SUMMON:
				// Удаление трупа, если идет суммон из трупа.
				Location loc = null;
				if(_targetType == SkillTargetType.TARGET_CORPSE)
					for(Creature target : targets)
						if(target != null && target.isDead())
						{
							activeChar.getAI().setAttackTarget(null);
							loc = target.getLoc();
							if (target.isNpc())
								((NpcInstance) target).endDecayTask();
							else if (target.isSummon())
								((SummonInstance) target).endDecayTask();
							else
								return; // кто труп ?
						}

				if(activeChar.getPet() != null || activeChar.isMounted())
					return;

				NpcTemplate summonTemplate = NpcHolder.getInstance().getTemplate(getNpcId());
				SummonInstance summon = new SummonInstance(IdFactory.getInstance().getNextId(), summonTemplate, activeChar, _lifeTime, _itemConsumeIdInTime, _itemConsumeCountInTime, _itemConsumeDelay, this);
				activeChar.setPet(summon);

				summon.setTitle(activeChar.getName());
				summon.setExpPenalty(_expPenalty);
				summon.setExp(Experience.LEVEL[Math.min(summon.getLevel(), Experience.LEVEL.length - 1)]);
				summon.setHeading(activeChar.getHeading());
				summon.setReflection(activeChar.getReflection());
				summon.spawnMe(loc == null ? Location.findAroundPosition(activeChar, 50, 70) : loc);
				summon.setRunning();
				summon.setFollowMode(true);

				if(summon.getSkillLevel(4140) > 0)
					summon.altUseSkill(SkillTable.getInstance().getInfo(4140, summon.getSkillLevel(4140)), activeChar);

				if(summon.getName().equalsIgnoreCase("Shadow"))//FIXME [G1ta0] идиотский хардкод
					summon.addStatFunc(new FuncAdd(Stats.ABSORB_DAMAGE_PERCENT, 0x40, this, 15));

				EffectsDAO.getInstance().restoreEffects(summon);
				if(activeChar.isInOlympiadMode())
					summon.getEffectList().stopAllEffects();

				summon.setCurrentHpMp(summon.getMaxHp(), summon.getMaxMp(), false);

				if(_summonType == SummonType.SIEGE_SUMMON)
				{
					SiegeEvent siegeEvent = activeChar.getEvent(SiegeEvent.class);
					siegeEvent.addSiegeSummon(summon);
				}
				break;
			case MERCHANT:
				if(activeChar.getPet() != null || activeChar.isMounted())
					return;

				NpcTemplate merchantTemplate = NpcHolder.getInstance().getTemplate(getNpcId());
				MerchantInstance merchant = new MerchantInstance(IdFactory.getInstance().getNextId(), merchantTemplate);

				merchant.setCurrentHp(merchant.getMaxHp(), false);
				merchant.setCurrentMp(merchant.getMaxMp());
				merchant.setHeading(activeChar.getHeading());
				merchant.setReflection(activeChar.getReflection());
				merchant.spawnMe(activeChar.getLoc());

				ThreadPoolManager.getInstance().schedule(new GameObjectTasks.DeleteTask(merchant), _lifeTime);
				break;
		}


		if(isSSPossible())
			caster.unChargeShots(isMagic());
	}

	@Override
	public boolean isOffensive()
	{
		return _targetType == SkillTargetType.TARGET_CORPSE;
	}
}