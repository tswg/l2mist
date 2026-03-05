package core.gameserver.network.l2.c2s;

import core.gameserver.model.Party;
import core.gameserver.model.Player;
import core.gameserver.model.entity.Reflection;
import core.gameserver.model.entity.DimensionalRift;
import core.gameserver.network.l2.components.CustomMessage;

public class RequestOustPartyMember extends L2GameClientPacket
{
	//Format: cS
	private String _name;

	@Override
	protected void readImpl()
	{
		_name = readS(16);
	}

	@Override
	protected void runImpl()
	{
		Player activeChar = getClient().getActiveChar();
		if(activeChar == null)
			return;
		
		Party party = activeChar.getParty();
		if(party == null || !activeChar.getParty().isLeader(activeChar))
		{
			activeChar.sendActionFailed();
			return;
		}
				
			if(activeChar.isInOlympiadMode())
			{
				activeChar.sendMessage(new CustomMessage("core.gameserver.clientpackets.RequestOustPartyMember.CantOutOfGroup", activeChar));
				return;
			}
						
			Player member = party.getPlayerByName(_name);
			
			if(member == activeChar)
			{
				activeChar.sendActionFailed();
				return;
			}
			
			if(member == null)
			{
				activeChar.sendActionFailed();
				return;
			}
			Reflection r = party.getReflection();
			if(r != null && r instanceof DimensionalRift && member.getReflection().equals(r))
				activeChar.sendMessage(new CustomMessage("core.gameserver.clientpackets.RequestOustPartyMember.CantOustInRift", activeChar));
			else if(r != null && !(r instanceof DimensionalRift))
				activeChar.sendMessage(new CustomMessage("core.gameserver.clientpackets.RequestOustPartyMember.CantOustInDungeon", activeChar));
			else
				party.removePartyMember(member, true);
	}
}