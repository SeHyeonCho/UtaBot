package common.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * BotConfig 테스트 클래스
 */
class BotConfigTest {

    @Test
    @DisplayName("폴더 설정이 없으면 기본 폴더를 쓴다")
    void shouldUseDefaultDirectories() {
        // when
        BotConfig config = BotConfig.from(Map.of("TOKEN", "abc")::get);

        // then
        assertEquals("abc", config.token());
        assertEquals(Path.of("data"), config.dataDir());
        assertEquals(Path.of("uploads"), config.uploadDir());
        assertEquals(Path.of("music"), config.musicDir());
        assertEquals(Path.of("data", "upload-channels.json"), config.dataFile("upload-channels.json"));
    }

    @Test
    @DisplayName("환경 변수로 폴더를 바꿀 수 있다")
    void shouldReadDirectoriesFromEnv() {
        // when
        BotConfig config = BotConfig.from(Map.of(
                "TOKEN", "abc",
                "DATA_DIR", "/srv/uta/data",
                "UPLOAD_DIR", "/srv/uta/uploads",
                "MUSIC_DIR", "/srv/uta/music")::get);

        // then
        assertEquals(Path.of("/srv/uta/data"), config.dataDir());
        assertEquals(Path.of("/srv/uta/uploads"), config.uploadDir());
        assertEquals(Path.of("/srv/uta/music"), config.musicDir());
    }

    @Test
    @DisplayName("TOKEN이 없으면 무엇을 설정해야 하는지 알려준다")
    void shouldRequireToken() {
        IllegalStateException e = assertThrows(IllegalStateException.class, () -> BotConfig.from(Map.<String, String>of()::get));
        assertTrue(e.getMessage().contains("TOKEN"));
    }

    @Test
    @DisplayName("설정을 문자열로 바꿔도 토큰은 나오지 않는다")
    void shouldNotExposeTokenInToString() {
        assertFalse(BotConfig.from(Map.of("TOKEN", "secret-token")::get).toString().contains("secret-token"));
    }
}
