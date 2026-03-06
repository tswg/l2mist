package core.gameserver.model.phantom;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

public class PhantomDataService
{
	public void loadSets(String fileName, List<PhantomSet> result)
	{
		result.clear();
		LineNumberReader lnr = null;
		BufferedReader br = null;
		FileReader fr = null;
		try
		{
			File data = new File(fileName);
			if(!data.exists())
				return;
			fr = new FileReader(data);
			br = new BufferedReader(fr);
			lnr = new LineNumberReader(br);
			String line;
			while((line = lnr.readLine()) != null)
			{
				if(line.trim().length() == 0 || line.startsWith("#"))
					continue;
				String[] items = line.split(",");
				int custom = 0;
				try { custom = Integer.parseInt(items[5]); } catch(Exception e) { custom = 0; }
				result.add(new PhantomSet(Integer.parseInt(items[0]), Integer.parseInt(items[1]), Integer.parseInt(items[2]), Integer.parseInt(items[3]), Integer.parseInt(items[4]), Integer.parseInt(items[5]), custom));
			}
		}
		catch(Exception e)
		{
		}
		finally
		{
			try
			{
				if(fr != null)
					fr.close();
				if(br != null)
					br.close();
				if(lnr != null)
					lnr.close();
			}
			catch(Exception e1)
			{
			}
		}
	}

	public List<String> loadPhrases(String fileName)
	{
		List<String> phrases = new ArrayList<String>();
		LineNumberReader lnr = null;
		BufferedReader br = null;
		FileReader fr = null;
		try
		{
			File data = new File(fileName);
			if(!data.exists())
				return phrases;
			fr = new FileReader(data);
			br = new BufferedReader(fr);
			lnr = new LineNumberReader(br);
			String line;
			while((line = lnr.readLine()) != null)
			{
				if(line.trim().length() == 0 || line.startsWith("#"))
					continue;
				phrases.add(line);
			}
		}
		catch(Exception e)
		{
		}
		finally
		{
			try
			{
				if(fr != null)
					fr.close();
				if(br != null)
					br.close();
				if(lnr != null)
					lnr.close();
			}
			catch(Exception e1)
			{
			}
		}
		return phrases;
	}

	public void loadTownClans(String fileName, Map<Integer, ConcurrentLinkedQueue<Integer>> clanLists)
	{
		clanLists.clear();
		LineNumberReader lnr = null;
		BufferedReader br = null;
		FileReader fr = null;
		try
		{
			File data = new File(fileName);
			if(!data.exists())
				return;
			fr = new FileReader(data);
			br = new BufferedReader(fr);
			lnr = new LineNumberReader(br);
			String line;
			while((line = lnr.readLine()) != null)
			{
				if(line.trim().length() == 0 || line.startsWith("#"))
					continue;
				String[] items = line.split(":");
				int clanId = Integer.parseInt(items[0]);
				String[] pls = items[1].split(",");
				ConcurrentLinkedQueue<Integer> players = new ConcurrentLinkedQueue<Integer>();
				for(String plid : pls)
					players.add(Integer.valueOf(Integer.parseInt(plid)));
				clanLists.put(Integer.valueOf(clanId), players);
			}
		}
		catch(Exception e)
		{
		}
		finally
		{
			try
			{
				if(fr != null)
					fr.close();
				if(br != null)
					br.close();
				if(lnr != null)
					lnr.close();
			}
			catch(Exception e1)
			{
			}
		}
	}
}
