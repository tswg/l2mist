package core.gameserver.network.l2.c2s;

import core.commons.lang.ArrayUtils;
import core.gameserver.Config;
import core.gameserver.data.xml.holder.SkillAcquireHolder;
import core.gameserver.model.Creature;
import core.gameserver.model.Player;
import core.gameserver.model.SkillLearn;
import core.gameserver.model.base.AcquireType;
import core.gameserver.model.instances.NpcInstance;
import core.gameserver.network.l2.s2c.AcquireSkillInfo;
import core.gameserver.tables.SkillTable;

public class RequestAquireSkillInfo extends L2GameClientPacket
{
	private int _id;
	private int _level, _levela;
	private AcquireType _type;

	@Override
	protected void readImpl()
	{
		_id = readD();
		_level = readD();
		_type = ArrayUtils.valid(AcquireType.VALUES, readD());
	}

	@Override
	protected void runImpl()
	{
		Player player = getClient().getActiveChar();
		if(player == null || player.getTransformation() != 0 || SkillTable.getInstance().getInfo(_id, _level) == null || _type == null)
			return;

		NpcInstance trainer = player.getLastNpc();
		if((trainer == null || player.getDistance(trainer.getX(), trainer.getY()) > Creature.INTERACTION_DISTANCE) && !player.isGM())
			return;
		if(Config.ALT_SKILL_LEARN_MAX_LVL)
			_levela = SkillTable.getInstance().getMaxLevel(_id);
		else
			_levela = _level;
		SkillLearn skillLearn;
		//if(Config.ALT_SKILL_LEARN)
		//	skillLearn = SkillAcquireHolder.getInstance().getSkillLearn(player, _id, _levela, player.getSkillLearningClassId());
		//else
		skillLearn = SkillAcquireHolder.getInstance().getSkillLearn(player, _id, _levela, _type);
		
		if(skillLearn == null && !Config.ALT_SKILL_LEARN)
			return;

		sendPacket(new AcquireSkillInfo(_type, skillLearn));
	}
}