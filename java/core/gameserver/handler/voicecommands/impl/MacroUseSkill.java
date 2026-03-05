package core.gameserver.handler.voicecommands.impl;

import core.gameserver.handler.voicecommands.IVoicedCommandHandler;
import core.gameserver.model.Creature;
import core.gameserver.model.Player;
import core.gameserver.model.Skill;
import core.gameserver.scripts.Functions;

/**
 * @author Kekess
 */
public class MacroUseSkill extends Functions implements IVoicedCommandHandler{

	private String[] _commandList = new String[] { "useskill", "useskillforce", "useskillstand" };

	@Override
	public boolean useVoicedCommand(String command, Player activeChar, String args)
	{
		command = command.intern();
		if(command.equalsIgnoreCase("useskill"))
		{
			for(Skill _skill: activeChar.getAllSkills())
				if(_skill.getName().equalsIgnoreCase(args))
				{
					activeChar.setUseMacro(true);
					_skill.setIsUseMacro(true);
					activeChar.getAI().Cast(_skill, (Creature)activeChar.getTarget(), false, false);
				}
		}
		else if(command.equalsIgnoreCase("useskillforce"))
		{
			for(Skill _skill: activeChar.getAllSkills())
				if(_skill.getName().equalsIgnoreCase(args))
				{
					activeChar.setUseMacro(true);
					_skill.setIsUseMacro(true);
					activeChar.getAI().Cast(_skill, (Creature)activeChar.getTarget(), true, false);
				}
		}
		else if(command.equalsIgnoreCase("useskillstand"))
		{
			for(Skill _skill: activeChar.getAllSkills())
				if(_skill.getName().equalsIgnoreCase(args))
				{
					activeChar.setUseMacro(true);
					_skill.setIsUseMacro(true);
					activeChar.getAI().Cast(_skill, (Creature)activeChar.getTarget(), true, true);
				}
		}
		return true;
	}

	@Override
	public String[] getVoicedCommandList()
	{
		return _commandList;
	}
}
