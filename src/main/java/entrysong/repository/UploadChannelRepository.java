package entrysong.repository;

import com.google.gson.reflect.TypeToken;
import common.util.JsonFiles;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 서버별 mp3 업로드 전용 채널을 JSON 파일에 저장하는 저장소.
 * guildId -> channelId
 */
public class UploadChannelRepository {

    private static final Type STORE_TYPE = new TypeToken<Map<Long, Long>>(){}.getType();

    private final Path storePath;
    private final Map<Long, Long> uploadChannelByGuild = new ConcurrentHashMap<>();

    /**
     * @throws IllegalStateException 저장 파일을 읽을 수 없거나 형식이 잘못된 경우
     */
    public UploadChannelRepository(Path storePath) {
        this.storePath = storePath;
        Map<Long, Long> loaded = JsonFiles.read(storePath, STORE_TYPE);
        if (loaded != null) {
            uploadChannelByGuild.putAll(loaded);
        }
    }

    public synchronized void setUploadChannel(long guildId, long channelId) {
        uploadChannelByGuild.put(guildId, channelId);
        saveToDisk();
    }

    public Long getUploadChannel(long guildId) {
        return uploadChannelByGuild.get(guildId);
    }

    public synchronized void removeUploadChannel(long guildId) {
        if (uploadChannelByGuild.remove(guildId) != null) {
            saveToDisk();
        }
    }

    private void saveToDisk() {
        try {
            JsonFiles.writeAtomically(storePath, uploadChannelByGuild, STORE_TYPE);
        } catch (IOException e) {
            System.out.println("[UploadChannelRepository] save failed:");
            e.printStackTrace();
        }
    }
}
