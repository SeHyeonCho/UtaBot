package entrysong.repository;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 서버별 mp3 업로드 전용 채널을 저장하는 저장소.
 * guildId -> channelId (메모리에만 저장, 재시작 시 초기화)
 */
public class UploadChannelRepository {

    private final Map<Long, Long> uploadChannelByGuild = new ConcurrentHashMap<>();

    public void setUploadChannel(long guildId, long channelId) {
        uploadChannelByGuild.put(guildId, channelId);
    }

    public Long getUploadChannel(long guildId) {
        return uploadChannelByGuild.get(guildId);
    }

    public void removeUploadChannel(long guildId) {
        uploadChannelByGuild.remove(guildId);
    }
}

