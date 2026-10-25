package entrysong.repository;

import com.google.gson.reflect.TypeToken;
import common.util.JsonFiles;
import entrysong.model.EntrySongConfig;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 서버별, 유저별 입장곡 설정을 JSON 파일에 저장하는 저장소입니다.
 *
 * 구조: guildId -> (userId -> EntrySongConfig)
 */
public class EntrySongRepository {

    private static final Type STORE_TYPE =
            new TypeToken<Map<Long, Map<Long, EntrySongConfig>>>(){}.getType();

    private final Path storePath;
    private final Map<Long, Map<Long, EntrySongConfig>> songsByGuild = new ConcurrentHashMap<>();

    /**
     * @throws IllegalStateException 저장 파일을 읽을 수 없거나 형식이 잘못된 경우
     */
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
        Map<Long, Map<Long, EntrySongConfig>> loaded = JsonFiles.read(storePath, STORE_TYPE);
        if (loaded == null) {
            System.out.println("[EntrySongRepository] no store file, start empty.");
            return;
        }

        loaded.forEach((guildId, songs) -> songsByGuild.put(guildId, new ConcurrentHashMap<>(songs)));
        System.out.println("[EntrySongRepository] loaded " + songsByGuild.size() + " servers.");
    }

    private void saveToDisk() {
        try {
            JsonFiles.writeAtomically(storePath, songsByGuild, STORE_TYPE);
        } catch (IOException e) {
            System.out.println("[EntrySongRepository] save failed:");
            e.printStackTrace();
        }
    }
}

