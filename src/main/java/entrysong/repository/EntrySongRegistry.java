package entrysong.repository;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * username#discriminator 기반으로 입장곡 설정 저장
 */
public final class EntrySongRegistry {

    private static final Path STORE_PATH = Paths.get("data", "entry-songs.json");
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .create();
    private static final Type MAP_TYPE =
            new TypeToken<Map<String, EntrySongConfig>>(){}.getType();

    // key = "username#0000"
    private static final Map<String, EntrySongConfig> REGISTRY =
            new ConcurrentHashMap<>();

    static {
        loadFromDisk();
    }

    private EntrySongRegistry() {}

    // username#0000 생성 함수
    private static String keyOf(String username, String discriminator) {
        return username + "#" + discriminator;
    }

    public static EntrySongConfig getSong(String username, String discriminator) {
        return REGISTRY.get(keyOf(username, discriminator));
    }

    public static EntrySongConfig getSong(String tag) { // "name#0000"
        return REGISTRY.get(tag);
    }

    public static synchronized void setSong(String username, String discriminator,
                                            String fileName, int start, int duration) {

        String key = keyOf(username, discriminator);
        EntrySongConfig cfg = new EntrySongConfig(fileName, start, duration);

        REGISTRY.put(key, cfg);
        saveToDisk();
    }

    public static synchronized void removeSong(String username, String discriminator) {
        REGISTRY.remove(keyOf(username, discriminator));
        saveToDisk();
    }

    private static void loadFromDisk() {
        try {
            if (!Files.exists(STORE_PATH)) {
                System.out.println("[EntrySongRegistry] no store file, start empty.");
                return;
            }

            try (BufferedReader reader = Files.newBufferedReader(STORE_PATH, StandardCharsets.UTF_8)) {
                Map<String, EntrySongConfig> loaded = GSON.fromJson(reader, MAP_TYPE);
                if (loaded != null) {
                    REGISTRY.clear();
                    REGISTRY.putAll(loaded);
                    System.out.println("[EntrySongRegistry] loaded " + REGISTRY.size() + " entries.");
                }
            }
        } catch (IOException e) {
            System.out.println("[EntrySongRegistry] load failed:");
            e.printStackTrace();
        }
    }

    private static void saveToDisk() {
        try {
            Path dir = STORE_PATH.getParent();
            if (dir != null && !Files.exists(dir)) {
                Files.createDirectories(dir);
            }

            try (BufferedWriter writer = Files.newBufferedWriter(
                    STORE_PATH,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING
            )) {
                GSON.toJson(REGISTRY, MAP_TYPE, writer);
            }

            System.out.println("[EntrySongRegistry] saved.");
        } catch (IOException e) {
            System.out.println("[EntrySongRegistry] save failed:");
            e.printStackTrace();
        }
    }
}


