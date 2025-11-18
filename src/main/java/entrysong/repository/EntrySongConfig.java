package entrysong.repository;

public class EntrySongConfig {
    public final String fileName;   // "세현#2221.mp3"
    public final int startSec;      // 시작 위치 (초)
    public final int durationSec;   // 재생 길이 (초)

    public EntrySongConfig(String fileName, int startSec, int durationSec) {
        this.fileName = fileName;
        this.startSec = startSec;
        this.durationSec = durationSec;
    }

    @Override
    public String toString() {
        return "EntrySongConfig{" +
                "fileName='" + fileName + '\'' +
                ", startSec=" + startSec +
                ", durationSec=" + durationSec +
                '}';
    }
}


