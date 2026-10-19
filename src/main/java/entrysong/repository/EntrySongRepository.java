package entrysong.repository;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import entrysong.model.EntrySongConfig;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 서버별, 유저별 입장곡 설정을 JSON 파일에 저장하는 저장소입니다.
 *
 * 구조: guildId -> (userId -> EntrySongConfig)
 */
public class EntrySongRepository {

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .create();
    private static final Type STORE_TYPE =
            new TypeToken<Map<Long, Map<Long, EntrySongConfig>>>(){}.getType();

    private final Path storePath;
    private final Map<Long, Map<Long, EntrySongConfig>> songsByGuild = new ConcurrentHashMap<>();

    public EntrySongRepository(Path storePath) {
        this.storePath = storePath;
        loadFromDisk();
    }

    /**
     * @return 입장곡 설정, 없으면 null
     */
    public EntrySongConfig get(long guildId, long userId) {
        Map<Long, EntrySongConfig> songs = songsByGuild.get(guildId);
        return songs == null ? null : songs.get(userId);
    }

    public synchronized void save(long guildId, long userId, EntrySongConfig config) {
        songsByGuild.computeIfAbsent(guildId, id -> new ConcurrentHashMap<>()).put(userId, config);
        saveToDisk();
    }

    public synchronized void remove(long guildId, long userId) {
        Map<Long, EntrySongConfig> songs = songsByGuild.get(guildId);
        if (songs != null && songs.remove(userId) != null) {
            saveToDisk();
        }
    }

    private void loadFromDisk() {
        if (!Files.exists(storePath)) {
            System.out.println("[EntrySongRepository] no store file, start empty.");
            return;
        }

        try (BufferedReader reader = Files.newBufferedReader(storePath, StandardCharsets.UTF_8)) {
            Map<Long, Map<Long, EntrySongConfig>> loaded = GSON.fromJson(reader, STORE_TYPE);
            if (loaded != null) {
                loaded.forEach((guildId, songs) -> songsByGuild.put(guildId, new ConcurrentHashMap<>(songs)));
            }
            System.out.println("[EntrySongRepository] loaded " + songsByGuild.size() + " servers.");
        } catch (IOException e) {
            System.out.println("[EntrySongRepository] load failed:");
            e.printStackTrace();
        }
    }

    // 임시 파일에 쓴 뒤 교체해서, 저장 도중 종료돼도 기존 파일이 깨지지 않게 함
    private void saveToDisk() {
        try {
            Path dir = storePath.toAbsolutePath().getParent();
            Files.createDirectories(dir);

            Path tempPath = Files.createTempFile(dir, storePath.getFileName().toString(), ".tmp");
            try (BufferedWriter writer = Files.newBufferedWriter(tempPath, StandardCharsets.UTF_8)) {
                GSON.toJson(songsByGuild, STORE_TYPE, writer);
            }
            Files.move(tempPath, storePath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            System.out.println("[EntrySongRepository] save failed:");
            e.printStackTrace();
        }
    }
}

