package core.gameserver.network.l2.c2s;

import core.gameserver.Config;
import core.gameserver.instancemanager.PetitionManager;
import core.gameserver.model.Player;
import core.gameserver.network.l2.components.ChatType;
import core.gameserver.network.l2.components.SystemMsg;
import core.gameserver.network.l2.s2c.Say2;
import core.gameserver.network.l2.s2c.SystemMessage2;
import core.gameserver.tables.GmListTable;

/**
 * <p>Format: (c) d
 * <ul>
 * <li>d: Unknown</li>
 * </ul></p>
 *
 * @author n0nam3
 */
public final class RequestPetitionCancel extends L2GameClientPacket
{
	//private int _unknown;

	@Override
	protected void readImpl()
	{
		//_unknown = readD(); This is pretty much a trigger packet.
	}

	@Override
	protected void runImpl()
	{
		Player activeChar = getClient().getActiveChar();
		if(activeChar == null)
			return;

		if(PetitionManager.getInstance().isPlayerInConsultation(activeChar))
		{
			if(activeChar.isGM())
				PetitionManager.getInstance().endActivePetition(activeChar);
			else
				activeChar.sendPacket(new SystemMessage2(SystemMsg.YOUR_PETITION_IS_BEING_PROCESSED));
		}
		else if(PetitionManager.getInstance().isPlayerPetitionPending(activeChar))
		{
			if(PetitionManager.getInstance().cancelActivePetition(activeChar))
			{
				int numRemaining = Config.MAX_PETITIONS_PER_PLAYER - PetitionManager.getInstance().getPlayerTotalPetitionCount(activeChar);

				activeChar.sendPacket(new SystemMessage2(SystemMsg.THE_PETITION_WAS_CANCELED).addString(String.valueOf(numRemaining)));

				// Notify all GMs that the player's pending petition has been cancelled.
				String msgContent = activeChar.getName() + " has canceled a pending petition.";
				GmListTable.broadcastToGMs(new Say2(activeChar.getObjectId(), ChatType.HERO_VOICE, "Petition System", msgContent));
			}
			else
				activeChar.sendPacket(new SystemMessage2(SystemMsg.FAILED_TO_CANCEL_PETITION));
		}
		else
			activeChar.sendPacket(new SystemMessage2(SystemMsg.YOU_HAVE_NOT_SUBMITTED_A_PETITION));
	}
}
