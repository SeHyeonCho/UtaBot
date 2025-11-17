package util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 특정 유저의 입장곡 URL을 저장하는 임시 레지스트리.
 *
 */
public class EntrySongRegistry {

    public static class EntrySongConfig {
        public final String fileName;   // "세현#2221.mp3"
        public final int startSec;      // 시작 위치 (초)
        public final int durationSec;   // 재생 길이 (초)

        public EntrySongConfig(String fileName, int startSec, int durationSec) {
            this.fileName = fileName;
            this.startSec = startSec;
            this.durationSec = durationSec;
        }
    }

    private static final Map<Long, EntrySongConfig> songs = new ConcurrentHashMap<>();

    public static void setSong(long userId, String fileName, int start, int duration) {
        songs.put(userId, new EntrySongConfig(fileName, start, duration));
    }

    public static EntrySongConfig getSong(long userId) {
        return songs.get(userId);
    }

    public static void removeSong(long userId) {
        songs.remove(userId);
    }
}