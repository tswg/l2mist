package core.gameserver.phantom.io;

import core.gameserver.phantom.model.PhantomProfile;

import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public final class ProfileParser {
    private ProfileParser() {}

    public static List<PhantomProfile> load(String path) {
        try {
            List<String> lines = Files.readAllLines(Paths.get(path), Charset.forName("UTF-8"));
            List<PhantomProfile> profiles = new ArrayList<>();

            for (String raw : lines) {
                String line = raw.trim();
                if (line.isEmpty() || line.startsWith("#") || line.startsWith("//")) continue;

                // id;name;archetype;minLvl;maxLvl;aggression
                String[] a = line.split(";");
                if (a.length < 6) throw new IllegalArgumentException("Bad profile line: " + line);

                int i = 0;
                int id = Integer.parseInt(a[i++].trim());
                String name = a[i++].trim();
                String archetype = a[i++].trim();
                int minLvl = Integer.parseInt(a[i++].trim());
                int maxLvl = Integer.parseInt(a[i++].trim());
                double aggr = Double.parseDouble(a[i++].trim());

                profiles.add(new PhantomProfile(id, name, archetype, minLvl, maxLvl, aggr));
            }
            return profiles;
        } catch (Exception e) {
            throw new RuntimeException("Cannot load profiles: " + path, e);
        }
    }
}
