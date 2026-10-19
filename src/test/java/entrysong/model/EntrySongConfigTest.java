package entrysong.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * EntrySongConfig 테스트 클래스
 *
 * 테스트 대상:
 * - 생성자: 데이터 보관
 * - isUrl(): URL 여부 판단
 * - withTime(): 재생 구간 변경
 * - toString(): 문자열 표현
 */
class EntrySongConfigTest {

    @Test
    @DisplayName("유효한 데이터로 설정을 생성한다")
    void shouldCreateConfigWithValidData() {
        // given
        String fileName = "123456789.mp3";
        int startSec = 5;
        int durationSec = 10;

        // when
        EntrySongConfig config = new EntrySongConfig(fileName, startSec, durationSec);

        // then
        assertEquals(fileName, config.source);
        assertEquals(startSec, config.startSec);
        assertEquals(durationSec, config.durationSec);
        assertFalse(config.isUrl());
    }

    @Test
    @DisplayName("URL로 설정을 생성한다")
    void shouldCreateConfigWithUrl() {
        // given
        String url = "https://www.youtube.com/watch?v=test";

        // when
        EntrySongConfig config = new EntrySongConfig(url, 0, 7);

        // then
        assertEquals(url, config.source);
        assertTrue(config.isUrl());
    }

    @Test
    @DisplayName("http/https로 시작하는 값만 URL로 판단한다")
    void shouldDetectUrl() {
        assertTrue(EntrySongConfig.isUrl("http://example.com/a.mp3"));
        assertTrue(EntrySongConfig.isUrl("https://youtu.be/abc"));
        assertFalse(EntrySongConfig.isUrl("123.mp3"));
        assertFalse(EntrySongConfig.isUrl("ftp://example.com/a.mp3"));
    }

    @Test
    @DisplayName("재생 구간만 바꾼 새 설정을 만든다")
    void shouldChangeOnlyTimeWithTime() {
        // given
        EntrySongConfig config = new EntrySongConfig("https://youtu.be/abc", 0, 7);

        // when
        EntrySongConfig updated = config.withTime(5, 3);

        // then
        assertEquals(config.source, updated.source);
        assertEquals(5, updated.startSec);
        assertEquals(3, updated.durationSec);
        assertEquals(0, config.startSec); // 원본은 그대로
    }

    @Test
    @DisplayName("toString()을 올바르게 생성한다")
    void shouldGenerateToStringCorrectly() {
        // given
        String fileName = "123456789.mp3";
        EntrySongConfig config = new EntrySongConfig(fileName, 5, 10);

        // when
        String result = config.toString();

        // then
        assertTrue(result.contains("EntrySongConfig"));
        assertTrue(result.contains(fileName));
        assertTrue(result.contains("5"));
        assertTrue(result.contains("10"));
    }
}
