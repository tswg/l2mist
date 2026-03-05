package core.gameserver.phantom.io;

import core.gameserver.phantom.model.PhantomSpot;

import java.nio.file.*;
import java.util.*;

public final class SpotParser {
    private SpotParser() {}

    public static List<PhantomSpot> load(String path) {
        try {
            List<String> lines = Files.readAllLines(Path.of(path));
            List<PhantomSpot> spots = new ArrayList<>();

            for (String raw : lines) {
                String line = raw.trim();
                if (line.isEmpty() || line.startsWith("#") || line.startsWith("//")) continue;

                // id;name;type;centerX;centerY;centerZ;radius;minLvl;maxLvl;maxCount
                String[] a = line.split(";");
                if (a.length < 10) {
                    throw new IllegalArgumentException("Bad spot line: " + line);
                }

                int i = 0;
                int id = Integer.parseInt(a[i++].trim());
                String name = a[i++].trim();
                String type = a[i++].trim();
                int x = Integer.parseInt(a[i++].trim());
                int y = Integer.parseInt(a[i++].trim());
                int z = Integer.parseInt(a[i++].trim());
                int radius = Integer.parseInt(a[i++].trim());
                int minLvl = Integer.parseInt(a[i++].trim());
                int maxLvl = Integer.parseInt(a[i++].trim());
                int maxCount = Integer.parseInt(a[i++].trim());

                spots.add(new PhantomSpot(id, name, type, x, y, z, radius, minLvl, maxLvl, maxCount));
            }
            return spots;
        } catch (Exception e) {
            throw new RuntimeException("Cannot load spots: " + path, e);
        }
    }
}