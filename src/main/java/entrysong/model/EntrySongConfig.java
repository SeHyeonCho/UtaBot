package entrysong.model;

/**
 * 한 서버에서 한 유저의 입장곡 설정입니다.
 */
public class EntrySongConfig {
    public final String source;     // URL 또는 업로드한 mp3 파일명 ("123456789.mp3")
    public final int startSec;      // 시작 위치 (초)
    public final int durationSec;   // 재생 길이 (초)

    public EntrySongConfig(String source, int startSec, int durationSec) {
        this.source = source;
        this.startSec = startSec;
        this.durationSec = durationSec;
    }

    public static boolean isUrl(String value) {
        return value.startsWith("http://") || value.startsWith("https://");
    }

    public boolean isUrl() {
        return isUrl(source);
    }

    /**
     * 소스는 그대로 두고 재생 구간만 바꾼 새 설정을 만듭니다.
     */
    public EntrySongConfig withTime(int startSec, int durationSec) {
        return new EntrySongConfig(source, startSec, durationSec);
    }

    @Override
    public String toString() {
        return "EntrySongConfig{" +
                "source='" + source + '\'' +
                ", startSec=" + startSec +
                ", durationSec=" + durationSec +
                '}';
    }
}

