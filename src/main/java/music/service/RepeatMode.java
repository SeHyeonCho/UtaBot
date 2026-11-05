package music.service;

/**
 * 곡이 끝났을 때의 반복 방식입니다.
 */
public enum RepeatMode {
    OFF("반복 끔"),
    TRACK("한 곡 반복"),
    QUEUE("전체 반복");

    private final String label;

    RepeatMode(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    /**
     * 버튼을 누를 때마다 끔 → 한 곡 → 전체 → 끔 순서로 바뀝니다.
     */
    public RepeatMode next() {
        return values()[(ordinal() + 1) % values().length];
    }
}
