package core.gameserver.handler.admincommands;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import core.commons.data.xml.AbstractHolder;
import core.gameserver.handler.admincommands.impl.AdminAdmin;
import core.gameserver.handler.admincommands.impl.AdminAnnouncements;
import core.gameserver.handler.admincommands.impl.AdminAttribute;
import core.gameserver.handler.admincommands.impl.AdminBan;
import core.gameserver.handler.admincommands.impl.AdminCamera;
import core.gameserver.handler.admincommands.impl.AdminCancel;
import core.gameserver.handler.admincommands.impl.AdminChangeAccessLevel;
import core.gameserver.handler.admincommands.impl.AdminClanHall;
import core.gameserver.handler.admincommands.impl.AdminClientSupport;
import core.gameserver.handler.admincommands.impl.AdminCreateItem;
import core.gameserver.handler.admincommands.impl.AdminCursedWeapons;
import core.gameserver.handler.admincommands.impl.AdminDelete;
import core.gameserver.handler.admincommands.impl.AdminDisconnect;
import core.gameserver.handler.admincommands.impl.AdminDoorControl;
import core.gameserver.handler.admincommands.impl.AdminEditChar;
import core.gameserver.handler.admincommands.impl.AdminEffects;
import core.gameserver.handler.admincommands.impl.AdminEnchant;
import core.gameserver.handler.admincommands.impl.AdminEvents;
import core.gameserver.handler.admincommands.impl.AdminGeodata;
import core.gameserver.handler.admincommands.impl.AdminGlobalEvent;
import core.gameserver.handler.admincommands.impl.AdminGm;
import core.gameserver.handler.admincommands.impl.AdminGmChat;
import core.gameserver.handler.admincommands.impl.AdminHeal;
import core.gameserver.handler.admincommands.impl.AdminHellbound;
import core.gameserver.handler.admincommands.impl.AdminHelpPage;
import core.gameserver.handler.admincommands.impl.AdminIP;
import core.gameserver.handler.admincommands.impl.AdminInstance;
import core.gameserver.handler.admincommands.impl.AdminKill;
import core.gameserver.handler.admincommands.impl.AdminLevel;
import core.gameserver.handler.admincommands.impl.AdminMammon;
import core.gameserver.handler.admincommands.impl.AdminManor;
import core.gameserver.handler.admincommands.impl.AdminMenu;
import core.gameserver.handler.admincommands.impl.AdminMonsterRace;
import core.gameserver.handler.admincommands.impl.AdminNochannel;
import core.gameserver.handler.admincommands.impl.AdminOlympiad;
import core.gameserver.handler.admincommands.impl.AdminPetition;
import core.gameserver.handler.admincommands.impl.AdminPledge;
import core.gameserver.handler.admincommands.impl.AdminPolymorph;
import core.gameserver.handler.admincommands.impl.AdminQuests;
import core.gameserver.handler.admincommands.impl.AdminReload;
import core.gameserver.handler.admincommands.impl.AdminRepairChar;
import core.gameserver.handler.admincommands.impl.AdminRes;
import core.gameserver.handler.admincommands.impl.AdminRide;
import core.gameserver.handler.admincommands.impl.AdminSS;
import core.gameserver.handler.admincommands.impl.AdminScripts;
import core.gameserver.handler.admincommands.impl.AdminServer;
import core.gameserver.handler.admincommands.impl.AdminShop;
import core.gameserver.handler.admincommands.impl.AdminShutdown;
import core.gameserver.handler.admincommands.impl.AdminSkill;
import core.gameserver.handler.admincommands.impl.AdminSpawn;
import core.gameserver.handler.admincommands.impl.AdminTarget;
import core.gameserver.handler.admincommands.impl.AdminTeam;
import core.gameserver.handler.admincommands.impl.AdminTeleport;
import core.gameserver.handler.admincommands.impl.AdminZone;
import core.gameserver.model.Player;
import core.gameserver.network.l2.components.CustomMessage;
import core.gameserver.utils.Log;

public class AdminCommandHandler extends AbstractHolder
{
	private static final AdminCommandHandler _instance = new AdminCommandHandler();

	public static AdminCommandHandler getInstance()
	{
		return _instance;
	}

	private Map<String, IAdminCommandHandler> _datatable = new HashMap<String, IAdminCommandHandler>();

	private AdminCommandHandler()
	{
		registerAdminCommandHandler(new AdminAdmin());
		registerAdminCommandHandler(new AdminAnnouncements());
                registerAdminCommandHandler(new AdminAttribute());
		registerAdminCommandHandler(new AdminBan());
		registerAdminCommandHandler(new AdminCamera());
		registerAdminCommandHandler(new AdminCancel());
		registerAdminCommandHandler(new AdminChangeAccessLevel());
		registerAdminCommandHandler(new AdminClanHall());
		registerAdminCommandHandler(new AdminClientSupport());
		registerAdminCommandHandler(new AdminCreateItem());
		registerAdminCommandHandler(new AdminCursedWeapons());
		registerAdminCommandHandler(new AdminDelete());
		registerAdminCommandHandler(new AdminDisconnect());
		registerAdminCommandHandler(new AdminDoorControl());
		registerAdminCommandHandler(new AdminEditChar());
		registerAdminCommandHandler(new AdminEffects());
		registerAdminCommandHandler(new AdminEnchant());
		registerAdminCommandHandler(new AdminEvents());
		registerAdminCommandHandler(new AdminGeodata());
		registerAdminCommandHandler(new AdminGlobalEvent());
		registerAdminCommandHandler(new AdminGm());
		registerAdminCommandHandler(new AdminGmChat());
		registerAdminCommandHandler(new AdminHeal());
		registerAdminCommandHandler(new AdminHellbound());
		registerAdminCommandHandler(new AdminHelpPage());
		registerAdminCommandHandler(new AdminInstance());
		registerAdminCommandHandler(new AdminIP());
		registerAdminCommandHandler(new AdminLevel());
		registerAdminCommandHandler(new AdminMammon());
		registerAdminCommandHandler(new AdminManor());
		registerAdminCommandHandler(new AdminMenu());
		registerAdminCommandHandler(new AdminMonsterRace());
		registerAdminCommandHandler(new AdminNochannel());
		registerAdminCommandHandler(new AdminOlympiad());
		registerAdminCommandHandler(new AdminPetition());
		registerAdminCommandHandler(new AdminPledge());
		registerAdminCommandHandler(new AdminPolymorph());
		registerAdminCommandHandler(new AdminQuests());
		registerAdminCommandHandler(new AdminReload());
		registerAdminCommandHandler(new AdminRepairChar());
		registerAdminCommandHandler(new AdminRes());
		registerAdminCommandHandler(new AdminRide());
		registerAdminCommandHandler(new AdminServer());
		registerAdminCommandHandler(new AdminShop());
		registerAdminCommandHandler(new AdminShutdown());
		registerAdminCommandHandler(new AdminSkill());
		registerAdminCommandHandler(new AdminScripts());
		registerAdminCommandHandler(new AdminSpawn());
		registerAdminCommandHandler(new AdminSS());
		registerAdminCommandHandler(new AdminTarget());
		registerAdminCommandHandler(new AdminTeam());
		registerAdminCommandHandler(new AdminTeleport());
		registerAdminCommandHandler(new AdminZone());
		registerAdminCommandHandler(new AdminKill());

	}

	public void registerAdminCommandHandler(IAdminCommandHandler handler)
	{
		for(Enum<?> e : handler.getAdminCommandEnum())
			_datatable.put(e.toString().toLowerCase(), handler);
	}

	public IAdminCommandHandler getAdminCommandHandler(String adminCommand)
	{
		String command = adminCommand;
		if(adminCommand.indexOf(" ") != -1)
			command = adminCommand.substring(0, adminCommand.indexOf(" "));
		return _datatable.get(command);
	}

	public void useAdminCommandHandler(Player activeChar, String adminCommand)
	{
		if(!(activeChar.isGM() || activeChar.getPlayerAccess().CanUseGMCommand))
		{
			activeChar.sendMessage(new CustomMessage("core.gameserver.clientpackets.SendBypassBuildCmd.NoCommandOrAccess", activeChar).addString(adminCommand));
			return;
		}

		String[] wordList = adminCommand.split(" ");
		IAdminCommandHandler handler = _datatable.get(wordList[0]);
		if(handler != null)
		{
			boolean success = false;
			try
			{
				for(Enum<?> e : handler.getAdminCommandEnum())
					if(e.toString().equalsIgnoreCase(wordList[0]))
					{
						success = handler.useAdminCommand(e, wordList, adminCommand, activeChar);
						break;
					}
			}
			catch(Exception e)
			{
				error("", e);
			}

			Log.LogCommand(activeChar, activeChar.getTarget(), adminCommand, success);
		}
	}


	public void process()
	{

	}
	

	public int size()
	{
		return _datatable.size();
	}


	public void clear()
	{
		_datatable.clear();
	}

	/**
	 * Получение списка зарегистрированных админ команд
	 * @return список команд
	 */
	public Set<String> getAllCommands()
	{
		return _datatable.keySet();
	}
}