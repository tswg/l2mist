package core.gameserver.utils;

import core.gameserver.Config;
import gnu.trove.map.hash.TIntLongHashMap;
import gnu.trove.iterator.TIntLongIterator;

import org.apache.commons.lang3.StringUtils;

public class AntiFlood
{
	private TIntLongHashMap _recentReceivers = new TIntLongHashMap();
	private long _lastSent = 0L;
	private String _lastText = StringUtils.EMPTY;

	private long _lastHeroTime;
	private long _lastTradeTime;
	private long _lastShoutTime;

	private long _lastMailTime;
	private long _lastEnchantTime;
	private long _lastAttributeTime;
	
	private long _lastBeautyShopWear;
	private long _lastCostumeShopWork;

	public boolean canTrade(String text)
	{
		long currentMillis = System.currentTimeMillis();

		if (currentMillis - _lastTradeTime < 5000L)
			return false;

		_lastTradeTime = currentMillis;
		return true;
	}

	public boolean canShout(String text)
	{
		long currentMillis = System.currentTimeMillis();

		if (currentMillis - _lastShoutTime < 5000L)
			return false;

		_lastShoutTime = currentMillis;
		return true;
	}

	public boolean canHero(String text)
	{
		long currentMillis = System.currentTimeMillis();

		if (currentMillis - _lastHeroTime < 10000L)
			return false;

		_lastHeroTime = currentMillis;
		return true;
	}

	public boolean canMail()
	{
		long currentMillis = System.currentTimeMillis();

		if (currentMillis - _lastMailTime < 10000L)
			return false;

		_lastMailTime = currentMillis;
		return true;
	}
	
	public boolean canEnchant()
	{
		long currentMillis = System.currentTimeMillis();

		if (currentMillis - _lastEnchantTime < Config.ENCHANT_FLOOD_DELAY)
			return false;

		_lastEnchantTime = currentMillis;
		return true;
	}
	
	public boolean canAttribute()
	{
		long currentMillis = System.currentTimeMillis();

		if (currentMillis - _lastAttributeTime < Config.ATTRIBUTE_FLOOD_DELAY)
			return false;

		_lastAttributeTime = currentMillis;
		return true;
	}

	public boolean canTell(int charId, String text)
	{
		long currentMillis = System.currentTimeMillis();
		long lastSent;

		TIntLongIterator itr = _recentReceivers.iterator();

		int recent = 0;
		while(itr.hasNext())
		{
			itr.advance();
			lastSent = itr.value();
			if (currentMillis - lastSent < (text.equalsIgnoreCase(_lastText) ? 600000L : 60000L))
				recent++;
			else
				itr.remove();
		}

		lastSent = _recentReceivers.put(charId, currentMillis);

		long delay = 333L;
		if (recent > 3)
		{
			lastSent = _lastSent;
			delay =  (recent - 3) * 3333L;
		}

		_lastText = text;
		_lastSent = currentMillis;

		return currentMillis - lastSent > delay;
	}
	
	public boolean canBeautyShopWear()
	{
		long currentMillis = System.currentTimeMillis();

		if (currentMillis - _lastBeautyShopWear < 1000L)
			return false;

		_lastBeautyShopWear = currentMillis;
		return true;
	}
	
	public boolean canCostumeShopWork()
	{
		long currentMillis = System.currentTimeMillis();

		if (currentMillis - _lastCostumeShopWork < 1000L)
			return false;

		_lastCostumeShopWork = currentMillis;
		return true;
	}
}
