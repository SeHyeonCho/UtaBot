package common.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * UrlUtils 테스트 클래스
 */
class UrlUtilsTest {

    @Test
    @DisplayName("http/https로 시작하면 URL로 판단한다")
    void shouldDetectHttpUrl() {
        assertTrue(UrlUtils.isHttpUrl("https://youtu.be/abc"));
        assertTrue(UrlUtils.isHttpUrl("http://example.com/a.mp3"));
        assertFalse(UrlUtils.isHttpUrl("아이유 좋은날"));
        assertFalse(UrlUtils.isHttpUrl("노래.mp3"));
    }

    @Test
    @DisplayName("유튜브 주소만 유튜브 URL로 판단한다")
    void shouldDetectYoutubeUrl() {
        assertTrue(UrlUtils.isYoutubeUrl("https://www.youtube.com/watch?v=dQw4w9WgXcQ"));
        assertTrue(UrlUtils.isYoutubeUrl("https://youtube.com/watch?v=dQw4w9WgXcQ"));
        assertTrue(UrlUtils.isYoutubeUrl("https://m.youtube.com/watch?v=dQw4w9WgXcQ"));
        assertTrue(UrlUtils.isYoutubeUrl("https://music.youtube.com/watch?v=dQw4w9WgXcQ"));
        assertTrue(UrlUtils.isYoutubeUrl("https://youtu.be/dQw4w9WgXcQ"));
        assertTrue(UrlUtils.isYoutubeUrl("https://WWW.YOUTUBE.COM/watch?v=dQw4w9WgXcQ"));

        assertFalse(UrlUtils.isYoutubeUrl("https://suno.com/s/zgDa46mDEBtaAZHa"));
        assertFalse(UrlUtils.isYoutubeUrl("https://youtube.com.evil.example/watch"));
        assertFalse(UrlUtils.isYoutubeUrl("youtube.com/watch?v=abc"));
        assertFalse(UrlUtils.isYoutubeUrl("https://youtu.be/a b"));
    }
}
