package core.gameserver.model.phantom;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.List;

import core.commons.util.Rnd;
import core.gameserver.utils.Location;

public class PhantomSpotService
{
	private final List<Location> spots = new ArrayList<Location>();

	public void loadTownSpots(String fileName)
	{
		spots.clear();
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
				spots.add(new Location(Integer.parseInt(items[0]), Integer.parseInt(items[1]), Integer.parseInt(items[2])));
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

	public Location randomSpot()
	{
		if(spots.isEmpty())
			return null;
		return spots.get(Rnd.get(0, spots.size() - 1));
	}

	public int size()
	{
		return spots.size();
	}
}
