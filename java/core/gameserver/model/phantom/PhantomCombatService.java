package core.gameserver.model.phantom;

import core.gameserver.model.Player;
import core.gameserver.model.base.Race;
import core.gameserver.model.instances.NpcInstance;
import core.gameserver.model.Skill;
import core.gameserver.tables.SkillTable;

public class PhantomCombatService
{
	private final PhantomRoleResolver roleResolver;
	private final PhantomMovementService movementService;

	public PhantomCombatService(PhantomRoleResolver roleResolver, PhantomMovementService movementService)
	{
		this.roleResolver = roleResolver;
		this.movementService = movementService;
	}

	public void attackPlayer(Player phantom, Player target)
	{
		if(target == null)
			return;
		movementService.pursueOrAttack(phantom, target);
		if(roleResolver.resolve(phantom) == PhantomRoleResolver.Role.MAGE)
			cast(phantom, target, SkillTable.getInstance().getInfo(1177, 2));
		else if(phantom.getRealDistance3D(target) <= phantom.getPhysicalAttackRange() + 40)
			castRaceNuke(phantom, target);
	}

	public void attackMonster(Player phantom, NpcInstance target)
	{
		if(target == null)
			return;
		movementService.pursueOrAttack(phantom, target);
		if(roleResolver.resolve(phantom) == PhantomRoleResolver.Role.MAGE)
			cast(phantom, target, SkillTable.getInstance().getInfo(1177, 2));
	}

	private void castRaceNuke(Player phantom, Player target)
	{
		Race race = phantom.getClassId().getRace();
		if(race == Race.orc)
			cast(phantom, target, SkillTable.getInstance().getInfo(29, 2));
		else if(race == Race.kamael)
			cast(phantom, target, SkillTable.getInstance().getInfo(468, 2));
		else if(race != Race.dwarf)
			cast(phantom, target, SkillTable.getInstance().getInfo(3, 2));
	}

	private void cast(Player phantom, Object target, Skill skill)
	{
		if(skill != null)
			phantom.doCast(skill, target, true);
	}
}
