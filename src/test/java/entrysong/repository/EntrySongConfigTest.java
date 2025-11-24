package entrysong.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * EntrySongConfig 테스트 클래스
 * 
 * 테스트 대상:
 * - 생성자: 데이터 보관
 * - toString(): 문자열 표현
 */
class EntrySongConfigTest {

    @Test
    @DisplayName("유효한 데이터로 설정을 생성한다")
    void shouldCreateConfigWithValidData() {
        // given
        String fileName = "test.mp3";
        int startSec = 5;
        int durationSec = 10;

        // when
        EntrySongConfig config = new EntrySongConfig(fileName, startSec, durationSec);

        // then
        assertEquals(fileName, config.fileName);
        assertEquals(startSec, config.startSec);
        assertEquals(durationSec, config.durationSec);
    }

    @Test
    @DisplayName("URL로 설정을 생성한다")
    void shouldCreateConfigWithUrl() {
        // given
        String url = "https://www.youtube.com/watch?v=test";
        int startSec = 0;
        int durationSec = 7;

        // when
        EntrySongConfig config = new EntrySongConfig(url, startSec, durationSec);

        // then
        assertEquals(url, config.fileName);
        assertEquals(startSec, config.startSec);
        assertEquals(durationSec, config.durationSec);
    }

    @Test
    @DisplayName("toString()을 올바르게 생성한다")
    void shouldGenerateToStringCorrectly() {
        // given
        String fileName = "test.mp3";
        int startSec = 5;
        int durationSec = 10;
        EntrySongConfig config = new EntrySongConfig(fileName, startSec, durationSec);

        // when
        String result = config.toString();

        // then
        assertNotNull(result);
        assertTrue(result.contains("EntrySongConfig"));
        assertTrue(result.contains(fileName));
        assertTrue(result.contains(String.valueOf(startSec)));
        assertTrue(result.contains(String.valueOf(durationSec)));
    }

    @Test
    @DisplayName("시작 시간이 0초인 경우를 처리한다")
    void shouldHandleZeroStartTime() {
        // given
        String fileName = "test.mp3";
        int startSec = 0;
        int durationSec = 10;

        // when
        EntrySongConfig config = new EntrySongConfig(fileName, startSec, durationSec);

        // then
        assertEquals(0, config.startSec);
        assertEquals(10, config.durationSec);
    }

    @Test
    @DisplayName("긴 재생 시간을 처리한다")
    void shouldHandleLongDuration() {
        // given
        String fileName = "test.mp3";
        int startSec = 0;
        int durationSec = 300; // 5분

        // when
        EntrySongConfig config = new EntrySongConfig(fileName, startSec, durationSec);

        // then
        assertEquals(300, config.durationSec);
    }
}

