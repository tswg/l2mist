package core.gameserver.network.l2.c2s;

import java.util.Calendar;

import core.gameserver.Announcements;
import core.gameserver.Config;
import core.gameserver.dao.MailDAO;
import core.gameserver.data.StringHolder;
import core.gameserver.data.xml.holder.ResidenceHolder;
import core.gameserver.instancemanager.CoupleManager;
import core.gameserver.instancemanager.CursedWeaponsManager;
import core.gameserver.instancemanager.PetitionManager;
import core.gameserver.instancemanager.PlayerMessageStack;
import core.gameserver.instancemanager.QuestManager;
import core.gameserver.listener.actor.player.OnAnswerListener;
import core.gameserver.listener.actor.player.impl.ReviveAnswerListener;
import core.gameserver.model.Creature;
import core.gameserver.model.Effect;
import core.gameserver.model.GameObjectsStorage;
import core.gameserver.model.Player;
import core.gameserver.model.Skill;
import core.gameserver.model.Summon;
import core.gameserver.model.World;
import core.gameserver.model.base.InvisibleType;
import core.gameserver.model.entity.Hero;
import core.gameserver.model.entity.SevenSigns;
import core.gameserver.model.entity.events.impl.ClanHallAuctionEvent;
import core.gameserver.model.entity.residence.ClanHall;
import core.gameserver.model.items.ItemInstance;
import core.gameserver.model.mail.Mail;
import core.gameserver.model.pledge.Clan;
import core.gameserver.model.pledge.SubUnit;
import core.gameserver.model.pledge.UnitMember;
import core.gameserver.model.quest.Quest;
import core.gameserver.network.l2.GameClient;
import core.gameserver.network.l2.components.SystemMsg;
import core.gameserver.network.l2.s2c.ChangeWaitType;
import core.gameserver.network.l2.s2c.ClientSetTime;
import core.gameserver.network.l2.s2c.ConfirmDlg;
import core.gameserver.network.l2.s2c.Die;
import core.gameserver.network.l2.s2c.EtcStatusUpdate;
import core.gameserver.network.l2.s2c.ExAutoSoulShot;
import core.gameserver.network.l2.s2c.ExBR_PremiumState;
import core.gameserver.network.l2.s2c.ExBasicActionList;
import core.gameserver.network.l2.s2c.ExGoodsInventoryChangedNotify;
import core.gameserver.network.l2.s2c.ExMPCCOpen;
import core.gameserver.network.l2.s2c.ExNoticePostArrived;
import core.gameserver.network.l2.s2c.ExNotifyPremiumItem;
import core.gameserver.network.l2.s2c.ExPCCafePointInfo;
import core.gameserver.network.l2.s2c.ExReceiveShowPostFriend;
import core.gameserver.network.l2.s2c.ExSetCompassZoneCode;
import core.gameserver.network.l2.s2c.ExStorageMaxCount;
import core.gameserver.network.l2.s2c.HennaInfo;
import core.gameserver.network.l2.s2c.L2FriendList;
import core.gameserver.network.l2.s2c.L2GameServerPacket;
import core.gameserver.network.l2.s2c.MagicSkillLaunched;
import core.gameserver.network.l2.s2c.MagicSkillUse;
import core.gameserver.network.l2.s2c.PartySmallWindowAll;
import core.gameserver.network.l2.s2c.PartySpelled;
import core.gameserver.network.l2.s2c.PetInfo;
import core.gameserver.network.l2.s2c.PledgeShowInfoUpdate;
import core.gameserver.network.l2.s2c.PledgeShowMemberListUpdate;
import core.gameserver.network.l2.s2c.PledgeSkillList;
import core.gameserver.network.l2.s2c.PrivateStoreMsgBuy;
import core.gameserver.network.l2.s2c.PrivateStoreMsgSell;
import core.gameserver.network.l2.s2c.QuestList;
import core.gameserver.network.l2.s2c.RecipeShopMsg;
import core.gameserver.network.l2.s2c.RelationChanged;
import core.gameserver.network.l2.s2c.Ride;
import core.gameserver.network.l2.s2c.SSQInfo;
import core.gameserver.network.l2.s2c.ShortCutInit;
import core.gameserver.network.l2.s2c.SkillCoolTime;
import core.gameserver.network.l2.s2c.SkillList;
import core.gameserver.network.l2.s2c.SystemMessage2;
import core.gameserver.scripts.Functions;
import core.gameserver.skills.AbnormalEffect;
import core.gameserver.tables.SkillTable;
import core.gameserver.templates.item.ItemTemplate;
import core.gameserver.utils.GameStats;
import core.gameserver.utils.ItemFunctions;
import core.gameserver.utils.TradeHelper;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import GameGuard.GameGuard;
import GameGuard.network.l2.GuardManager;

public class EnterWorld extends L2GameClientPacket
{
	private static final Object _lock = new Object();
	private static final Logger _log = LoggerFactory.getLogger(EnterWorld.class);

	@Override
	protected void readImpl()
	{
		//readS(); - клиент всегда отправляет строку "narcasse"
	}

	@Override
	protected void runImpl()
	{
		GameClient client = getClient();
		Player activeChar = client.getActiveChar();

		if(activeChar == null)
		{
			client.closeNow(false);
			return;
		}
		
		if(activeChar.getScheduleNoCarriering() != null)
			activeChar.resetScheduleNoCarriering();

		int MyObjectId = activeChar.getObjectId();
		Long MyStoreId = activeChar.getStoredId();

		synchronized (_lock)
		{
			for(Player cha : GameObjectsStorage.getAllPlayersForIterate())
			{
				if(MyStoreId == cha.getStoredId())
					continue;
				try
				{
					if(cha.getObjectId() == MyObjectId)
					{
						_log.warn("Double EnterWorld for char: " + activeChar.getName());
						cha.kick();
					}
				}
				catch(Exception e)
				{
					_log.error("", e);
				}
			}
		}

		GameStats.incrementPlayerEnterGame();

		boolean first = activeChar.entering;

		if(first)
		{
			activeChar.setOnlineStatus(true);
			if(activeChar.getPlayerAccess().GodMode && !Config.SHOW_GM_LOGIN)
				activeChar.setInvisibleType(InvisibleType.NORMAL);

			activeChar.setNonAggroTime(Long.MAX_VALUE);
			activeChar.spawnMe();

			if(activeChar.isInStoreMode())
				if(!TradeHelper.checksIfCanOpenStore(activeChar, activeChar.getPrivateStoreType()))
				{
					activeChar.setPrivateStoreType(Player.STORE_PRIVATE_NONE);
					activeChar.standUp();
					activeChar.broadcastCharInfo();
				}

			activeChar.setRunning();
			activeChar.standUp();
			activeChar.startTimers();
		}

		activeChar.sendPacket(new ExBR_PremiumState(activeChar, activeChar.hasBonus()));

		activeChar.getMacroses().sendUpdate();
		activeChar.sendPacket(new SSQInfo(), new HennaInfo(activeChar));
		activeChar.sendItemList(false);
		activeChar.sendPacket(new ShortCutInit(activeChar), new SkillList(activeChar), new SkillCoolTime(activeChar));
		activeChar.sendPacket(SystemMsg.WELCOME_TO_THE_WORLD_OF_LINEAGE_II);

		Announcements.getInstance().showAnnouncements(activeChar);

		if(first)
			activeChar.getListeners().onEnter();

		SevenSigns.getInstance().sendCurrentPeriodMsg(activeChar);

		if(first && activeChar.getCreateTime() > 0)
		{
			Calendar create = Calendar.getInstance();
			create.setTimeInMillis(activeChar.getCreateTime());
			Calendar now = Calendar.getInstance();

			int day = create.get(Calendar.DAY_OF_MONTH);
			if(create.get(Calendar.MONTH) == Calendar.FEBRUARY && day == 29)
				day = 28;

			int myBirthdayReceiveYear = activeChar.getVarInt(Player.MY_BIRTHDAY_RECEIVE_YEAR, 0);
			if(create.get(Calendar.MONTH) == now.get(Calendar.MONTH) && create.get(Calendar.DAY_OF_MONTH) == day)
			{
				if((myBirthdayReceiveYear == 0 && create.get(Calendar.YEAR) != now.get(Calendar.YEAR)) || myBirthdayReceiveYear > 0 && myBirthdayReceiveYear != now.get(Calendar.YEAR))
				{
					Mail mail = new Mail();
					mail.setSenderId(1);
					mail.setSenderName(StringHolder.getInstance().getNotNull(activeChar, "birthday.npc"));
					mail.setReceiverId(activeChar.getObjectId());
					mail.setReceiverName(activeChar.getName());
					mail.setTopic(StringHolder.getInstance().getNotNull(activeChar, "birthday.title"));
					mail.setBody(StringHolder.getInstance().getNotNull(activeChar, "birthday.text"));

					ItemInstance item = ItemFunctions.createItem(21169);
					item.setLocation(ItemInstance.ItemLocation.MAIL);
					item.setCount(1L);
					item.save();

					mail.addAttachment(item);
					mail.setUnread(true);
					mail.setType(Mail.SenderType.BIRTHDAY);
					mail.setExpireTime(720 * 3600 + (int) (System.currentTimeMillis() / 1000L));
					mail.save();

					activeChar.setVar(Player.MY_BIRTHDAY_RECEIVE_YEAR, String.valueOf(now.get(Calendar.YEAR)), -1);
				}
			}
		}

		if(activeChar.getClan() != null)
		{
			notifyClanMembers(activeChar);

			activeChar.sendPacket(activeChar.getClan().listAll());
			activeChar.sendPacket(new PledgeShowInfoUpdate(activeChar.getClan()), new PledgeSkillList(activeChar.getClan()));
		}

		// engage and notify Partner
		if(first && Config.ALLOW_WEDDING)
		{
			CoupleManager.getInstance().engage(activeChar);
			CoupleManager.getInstance().notifyPartner(activeChar);
		}

		if(first)
		{
			activeChar.getFriendList().notifyFriends(true);
			loadTutorial(activeChar);
			activeChar.restoreDisableSkills();
		}

		sendPacket(new L2FriendList(activeChar), new ExStorageMaxCount(activeChar), new QuestList(activeChar), new ExBasicActionList(activeChar), new EtcStatusUpdate(activeChar));

		activeChar.checkHpMessages(activeChar.getMaxHp(), activeChar.getCurrentHp());
		activeChar.checkDayNightMessages();

		if(Config.PETITIONING_ALLOWED)
			PetitionManager.getInstance().checkPetitionMessages(activeChar);

		if(!first)
		{
			if(activeChar.isCastingNow())
			{
				Creature castingTarget = activeChar.getCastingTarget();
				Skill castingSkill = activeChar.getCastingSkill();
				long animationEndTime = activeChar.getAnimationEndTime();
				if(castingSkill != null && castingTarget != null && castingTarget.isCreature() && activeChar.getAnimationEndTime() > 0)
					sendPacket(new MagicSkillUse(activeChar, castingTarget, castingSkill.getId(), castingSkill.getLevel(), (int) (animationEndTime - System.currentTimeMillis()), 0));
			}

			if(activeChar.isInBoat())
				activeChar.sendPacket(activeChar.getBoat().getOnPacket(activeChar, activeChar.getInBoatPosition()));

			if(activeChar.isMoving || activeChar.isFollow)
				sendPacket(activeChar.movePacket());

			if(activeChar.getMountNpcId() != 0)
				sendPacket(new Ride(activeChar));

			if(activeChar.isFishing())
				activeChar.stopFishing();
		}

		activeChar.entering = false;
		activeChar.sendUserInfo(true);

		if(activeChar.isSitting())
			activeChar.sendPacket(new ChangeWaitType(activeChar, ChangeWaitType.WT_SITTING));
		if(activeChar.getPrivateStoreType() != Player.STORE_PRIVATE_NONE)
			if(activeChar.getPrivateStoreType() == Player.STORE_PRIVATE_BUY)
				sendPacket(new PrivateStoreMsgBuy(activeChar));
			else if(activeChar.getPrivateStoreType() == Player.STORE_PRIVATE_SELL || activeChar.getPrivateStoreType() == Player.STORE_PRIVATE_SELL_PACKAGE)
				sendPacket(new PrivateStoreMsgSell(activeChar));
			else if(activeChar.getPrivateStoreType() == Player.STORE_PRIVATE_MANUFACTURE)
				sendPacket(new RecipeShopMsg(activeChar));

		if(activeChar.isDead())
			sendPacket(new Die(activeChar));

		activeChar.unsetVar("offline");

		// на всякий случай
		activeChar.sendActionFailed();

		if(first && activeChar.isGM() && Config.SAVE_GM_EFFECTS && activeChar.getPlayerAccess().CanUseGMCommand)
		{
			//silence
			if(activeChar.getVarB("gm_silence"))
			{
				activeChar.setMessageRefusal(true);
				activeChar.sendPacket(SystemMsg.MESSAGE_REFUSAL_MODE);
			}
			//invul
			if(activeChar.getVarB("gm_invul"))
			{
				activeChar.setIsInvul(true);
				activeChar.startAbnormalEffect(AbnormalEffect.S_INVULNERABLE);
				activeChar.sendMessage(activeChar.getName() + " is now immortal.");
			}
			//gmspeed
			try
			{
				int var_gmspeed = Integer.parseInt(activeChar.getVar("gm_gmspeed"));
				if(var_gmspeed >= 1 && var_gmspeed <= 4)
					activeChar.doCast(SkillTable.getInstance().getInfo(7029, var_gmspeed), activeChar, true);
			}
			catch(Exception E)
			{}
		}

		PlayerMessageStack.getInstance().CheckMessages(activeChar);

		sendPacket(ClientSetTime.STATIC, new ExSetCompassZoneCode(activeChar));

		Pair<Integer, OnAnswerListener> entry = activeChar.getAskListener(false);
		if(entry != null && entry.getValue() instanceof ReviveAnswerListener)
			sendPacket(new ConfirmDlg(SystemMsg.C1_IS_MAKING_AN_ATTEMPT_TO_RESURRECT_YOU_IF_YOU_CHOOSE_THIS_PATH_S2_EXPERIENCE_WILL_BE_RETURNED_FOR_YOU, 0).addString("Other player").addString("some"));

		if(activeChar.isCursedWeaponEquipped())
			CursedWeaponsManager.getInstance().showUsageTime(activeChar, activeChar.getCursedWeaponEquippedId());

		if(!first)
		{
			//Персонаж вылетел во время просмотра
			if(activeChar.isInObserverMode())
			{
				if(activeChar.getObserverMode() == Player.OBSERVER_LEAVING)
					activeChar.returnFromObserverMode();
				else
					if(activeChar.getOlympiadObserveGame() != null)
						activeChar.leaveOlympiadObserverMode(true);
					else
						activeChar.leaveObserverMode();
			}
			else if(activeChar.isVisible())
				World.showObjectsToPlayer(activeChar);

			if(activeChar.getPet() != null)
				sendPacket(new PetInfo(activeChar.getPet()));

			if(activeChar.isInParty())
			{
				Summon member_pet;
				//sends new member party window for all members
				//we do all actions before adding member to a list, this speeds things up a little
				sendPacket(new PartySmallWindowAll(activeChar.getParty(), activeChar));

				for(Player member : activeChar.getParty().getPartyMembers())
					if(member != activeChar)
					{
						sendPacket(new PartySpelled(member, true));
						if((member_pet = member.getPet()) != null)
							sendPacket(new PartySpelled(member_pet, true));

						sendPacket(RelationChanged.update(activeChar, member, activeChar));
					}

				// Если партия уже в СС, то вновь прибывшем посылаем пакет открытия окна СС
				if(activeChar.getParty().isInCommandChannel())
					sendPacket(ExMPCCOpen.STATIC);
			}

			for(int shotId : activeChar.getAutoSoulShot())
				sendPacket(new ExAutoSoulShot(shotId, true));

			for(Effect e : activeChar.getEffectList().getAllFirstEffects())
				if(e.getSkill().isToggle())
					sendPacket(new MagicSkillLaunched(activeChar.getObjectId(), e.getSkill().getId(), e.getSkill().getLevel(), activeChar));

			activeChar.broadcastCharInfo();
		}
		else
			activeChar.sendUserInfo(); // Отобразит права в клане

		activeChar.updateEffectIcons();
		activeChar.updateStats();

		if(Config.ALT_PCBANG_POINTS_ENABLED)
			activeChar.sendPacket(new ExPCCafePointInfo(activeChar, 0, 1, 2, 12));

		if(!activeChar.getPremiumItemList().isEmpty())
			activeChar.sendPacket(Config.GOODS_INVENTORY_ENABLED ? ExGoodsInventoryChangedNotify.STATIC : ExNotifyPremiumItem.STATIC);

		if(activeChar.getVarB("HeroPeriod") && Config.SERVICES_HERO_SELL_ENABLED)
		{
			activeChar.setHero(activeChar);
		}
		if(activeChar.getVarB("HeroPeriod") && activeChar.getVarLong("HeroPeriod") <= System.currentTimeMillis())
		{
			activeChar.setHero(false);
			for(ItemInstance item : activeChar.getInventory().getItems())
			{
				if(item.isHeroWeapon())
					activeChar.getInventory().destroyItem(item, 1);
			}
			activeChar.updatePledgeClass();
			activeChar.broadcastUserInfo(true);
			Hero.deleteHero(activeChar);
			Hero.removeSkills(activeChar);
			activeChar.unsetVar("HeroPeriod");
		}
		if(!activeChar.isHero())
		{
			for(ItemInstance item : activeChar.getInventory().getItems())
			{
				if(item.isHeroWeapon())
					activeChar.getInventory().destroyItem(item, 1);
			}
		}
		activeChar.delOlympiadIp();
		activeChar.sendVoteSystemInfo();
		activeChar.sendPacket(new ExReceiveShowPostFriend(activeChar));
		activeChar.getNevitSystem().onEnterWorld();
		if(GameGuard.isProtectionOn())
		{
			GuardManager.SendSpecialSting(client);
		}

		checkNewMail(activeChar);
		if (Config.ENABLE_TOP_PLAYERS_SYSTEM)
		{
			if (activeChar.getVar("TopPlayerPvP") != null && Config.ENABLE_TOP_PVP_PLAYERS)
				{
					activeChar.sendMessage(activeChar.isLangRus() ? "Сегодня Вы один из лучших в PvP!" : "Today you are one of the best in PvP!");
					activeChar.setFakeHero();
				}
			if (activeChar.getVar("TopPlayerPK") != null && Config.ENABLE_TOP_PK_PLAYERS)
				{
					activeChar.sendMessage(activeChar.isLangRus() ? "Сегодня Вы один из лучших в PK!" : "Today you are one of the best in PK!");
					activeChar.setFakeHero();
				}
			if (activeChar.getVar("TopPlayerLevel") != null && Config.ENABLE_TOP_LEVEL_PLAYERS)
				{
					activeChar.sendMessage(activeChar.isLangRus() ? "Сегодня Вы один из лучших по развитию персонажа!" : "Today you are one of the best character!");
					activeChar.setFakeHero();
				}
			if (activeChar.getVar("TopPlayerCoins") != null && Config.ENABLE_TOP_COINS_PLAYERS)
				{
					activeChar.sendMessage(activeChar.isLangRus() ? "Сегодня Вы один из самых богатых!" : "Today you are one of the richest!");
					activeChar.setFakeHero();
				}
		}
		if (Config.GET_PREMIUM_ITEM && activeChar.getNetConnection() != null)
		{
			if (activeChar.getNetConnection().getBonus() > 1 & activeChar.getNetConnection().getBonusExpire() > (System.currentTimeMillis() / 1000L))
				if (Functions.getItemCount(activeChar, Config.GET_PREMIUM_ITEM_ID) == 0)
					Functions.addItem(activeChar, Config.GET_PREMIUM_ITEM_ID, 1);
			if (activeChar.getNetConnection().getBonus() == 1 || activeChar.getNetConnection().getBonusExpire() < (System.currentTimeMillis() / 1000L))
				if (Functions.getItemCount(activeChar, Config.GET_PREMIUM_ITEM_ID) != 0)
					Functions.removeItem(activeChar, Config.GET_PREMIUM_ITEM_ID, 1);
		}
	}

	private static void notifyClanMembers(Player activeChar)
	{
		Clan clan = activeChar.getClan();
		SubUnit subUnit = activeChar.getSubUnit();
		if(clan == null || subUnit == null)
			return;

		UnitMember member = subUnit.getUnitMember(activeChar.getObjectId());
		if(member == null)
			return;

		member.setPlayerInstance(activeChar, false);

		int sponsor = activeChar.getSponsor();
		int apprentice = activeChar.getApprentice();
		L2GameServerPacket msg = new SystemMessage2(SystemMsg.CLAN_MEMBER_S1_HAS_LOGGED_INTO_GAME).addName(activeChar);
		PledgeShowMemberListUpdate memberUpdate = new PledgeShowMemberListUpdate(activeChar);
		for(Player clanMember : clan.getOnlineMembers(activeChar.getObjectId()))
		{
			clanMember.sendPacket(memberUpdate);
			if(clanMember.getObjectId() == sponsor)
				clanMember.sendPacket(new SystemMessage2(SystemMsg.YOUR_APPRENTICE_C1_HAS_LOGGED_OUT).addName(activeChar));
			else if(clanMember.getObjectId() == apprentice)
				clanMember.sendPacket(new SystemMessage2(SystemMsg.YOUR_SPONSOR_C1_HAS_LOGGED_IN).addName(activeChar));
			else
				clanMember.sendPacket(msg);
		}

		if(!activeChar.isClanLeader())
			return;

		ClanHall clanHall = clan.getHasHideout() > 0 ? ResidenceHolder.getInstance().getResidence(ClanHall.class, clan.getHasHideout()) : null;
		if(clanHall == null || clanHall.getAuctionLength() != 0)
			return;

		if(clanHall.getSiegeEvent().getClass() != ClanHallAuctionEvent.class)
			return;

		if(clan.getWarehouse().getCountOf(ItemTemplate.ITEM_ID_ADENA) < clanHall.getRentalFee())
			activeChar.sendPacket(new SystemMessage2(SystemMsg.PAYMENT_FOR_YOUR_CLAN_HALL_HAS_NOT_BEEN_MADE_PLEASE_ME_PAYMENT_TO_YOUR_CLAN_WAREHOUSE_BY_S1_TOMORROW).addLong(clanHall.getRentalFee()));
	}

	private void loadTutorial(Player player)
	{
		Quest q = QuestManager.getQuest(255);
		if(q != null)
			player.processQuestEvent(q.getName(), "UC", null);
	}

	private void checkNewMail(Player activeChar)
	{
		for(Mail mail : MailDAO.getInstance().getReceivedMailByOwnerId(activeChar.getObjectId()))
			if(mail.isUnread())
			{
				sendPacket(ExNoticePostArrived.STATIC_FALSE);
				break;
			}
	}
}