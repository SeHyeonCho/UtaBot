package upload.repository;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 길드(서버)별 mp3 업로드 전용 채널을 저장하는 레지스트리.
 * guildId -> channelId
 */
public class UploadChannelRegistry {

    private static final Map<Long, Long> UPLOAD_CHANNEL_BY_GUILD = new ConcurrentHashMap<>();

    public static void setUploadChannel(long guildId, long channelId) {
        UPLOAD_CHANNEL_BY_GUILD.put(guildId, channelId);
    }

    public static Long getUploadChannel(long guildId) {
        return UPLOAD_CHANNEL_BY_GUILD.get(guildId);
    }

    public static void removeUploadChannel(long guildId) {
        UPLOAD_CHANNEL_BY_GUILD.remove(guildId);
    }
}


