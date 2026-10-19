package entrysong.service;

/**
 * 입장할 때 실제로 재생할 입장곡 정보입니다.
 */
public class EntrySongSource {
    public final String identifier;  // lavaplayer에 넘길 URL 또는 파일 경로
    public final int startSec;
    public final int durationSec;

    public EntrySongSource(String identifier, int startSec, int durationSec) {
        this.identifier = identifier;
        this.startSec = startSec;
        this.durationSec = durationSec;
    }
}
