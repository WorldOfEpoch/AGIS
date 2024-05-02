package atavism.agis.server;

import java.io.*;
import java.nio.file.*;

import atavism.server.util.Log;

public class AdvertisementFileMerger {

    public static void merge(String agentName, String... agents) throws IOException {
        String worldConfigDir = System.getenv("AO_WORLD_CONFIG");
        String worldFileName = worldConfigDir + "/" + agentName + "-ads.txt";
        File worldFile = new File(worldFileName);
        try (FileOutputStream out = new FileOutputStream(worldFile)) {
            for (String agent : agents) {
                append(agent, out);
            }
        }
    }

    private static void append(String agentName, FileOutputStream out) throws IOException {
        String home = System.getenv("AO_HOME");
        String worldConfigDir = System.getenv("AO_WORLD_CONFIG");
        String commonFileName = home + "/config/common/" + agentName + "-ads.txt";
        File commonFile = new File(commonFileName);
        String worldFileName = worldConfigDir + "/" + agentName + "-ads.txt";
        File worldFile = new File(worldFileName);
        if (!commonFile.exists() && !worldFile.exists()) {
            Log.warn("Missing advertisements file for agent " + agentName);
            return;
        }
        if (commonFile.exists()) {
            Files.copy(Paths.get(commonFileName), out);
            out.write('\n');
        }
        if (worldFile.exists()) {
            Files.copy(Paths.get(worldFileName), out);
            out.write('\n');
        }
    }
}
