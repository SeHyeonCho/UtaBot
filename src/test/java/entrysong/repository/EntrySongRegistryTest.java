package entrysong.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * EntrySongRegistry 테스트 클래스
 * 
 * 테스트 대상:
 * - setSong(): 입장곡 설정 및 JSON 저장
 * - getSong(): 입장곡 조회
 * - removeSong(): 입장곡 제거
 * - JSON 파일 저장/로드
 * 
 * 주의: EntrySongRegistry는 static 필드와 static 블록을 사용하므로,
 * 실제 파일 시스템을 사용하는 통합 테스트로 작성합니다.
 */
class EntrySongRegistryTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("입장곡을 설정하고 조회한다")
    void shouldSetAndGetSong() {
        // given
        String username = "testuser";
        String discriminator = "1234";
        String fileName = "test.mp3";
        int startSec = 0;
        int durationSec = 10;

        // when
        EntrySongRegistry.setSong(username, discriminator, fileName, startSec, durationSec);
        EntrySongConfig result = EntrySongRegistry.getSong(username, discriminator);

        // then
        assertNotNull(result);
        assertEquals(fileName, result.fileName);
        assertEquals(startSec, result.startSec);
        assertEquals(durationSec, result.durationSec);
    }

    @Test
    @DisplayName("태그로 입장곡을 조회한다")
    void shouldGetSongByTag() {
        // given
        String username = "testuser";
        String discriminator = "1234";
        String fileName = "test.mp3";
        String tag = username + "#" + discriminator;

        // when
        EntrySongRegistry.setSong(username, discriminator, fileName, 0, 10);
        EntrySongConfig result = EntrySongRegistry.getSong(tag);

        // then
        assertNotNull(result);
        assertEquals(fileName, result.fileName);
    }

    @Test
    @DisplayName("기존 입장곡을 업데이트한다")
    void shouldUpdateExistingSong() {
        // given
        String username = "testuser";
        String discriminator = "1234";
        String fileName1 = "test1.mp3";
        String fileName2 = "test2.mp3";

        // when
        EntrySongRegistry.setSong(username, discriminator, fileName1, 0, 10);
        EntrySongRegistry.setSong(username, discriminator, fileName2, 5, 15);
        EntrySongConfig result = EntrySongRegistry.getSong(username, discriminator);

        // then
        assertNotNull(result);
        assertEquals(fileName2, result.fileName);
        assertEquals(5, result.startSec);
        assertEquals(15, result.durationSec);
    }

    @Test
    @DisplayName("입장곡이 설정되지 않았을 때 null을 반환한다")
    void shouldReturnNullWhenSongNotSet() {
        // given
        String username = "nonexistent";
        String discriminator = "9999";

        // when
        EntrySongConfig result = EntrySongRegistry.getSong(username, discriminator);

        // then
        assertNull(result);
    }

    @Test
    @DisplayName("입장곡을 제거한다")
    void shouldRemoveSong() {
        // given
        String username = "testuser";
        String discriminator = "1234";
        String fileName = "test.mp3";
        EntrySongRegistry.setSong(username, discriminator, fileName, 0, 10);

        // when
        EntrySongRegistry.removeSong(username, discriminator);
        EntrySongConfig result = EntrySongRegistry.getSong(username, discriminator);

        // then
        assertNull(result);
    }

    @Test
    @DisplayName("URL을 파일명으로 처리한다")
    void shouldHandleUrlAsFileName() {
        // given
        String username = "testuser";
        String discriminator = "1234";
        String url = "https://www.youtube.com/watch?v=test";

        // when
        EntrySongRegistry.setSong(username, discriminator, url, 0, 7);
        EntrySongConfig result = EntrySongRegistry.getSong(username, discriminator);

        // then
        assertNotNull(result);
        assertEquals(url, result.fileName);
    }

    @Test
    @DisplayName("여러 사용자를 처리한다")
    void shouldHandleMultipleUsers() {
        // given
        String username1 = "user1";
        String username2 = "user2";
        String discriminator = "1234";
        String fileName1 = "user1.mp3";
        String fileName2 = "user2.mp3";

        // when
        EntrySongRegistry.setSong(username1, discriminator, fileName1, 0, 10);
        EntrySongRegistry.setSong(username2, discriminator, fileName2, 5, 15);

        // then
        EntrySongConfig config1 = EntrySongRegistry.getSong(username1, discriminator);
        EntrySongConfig config2 = EntrySongRegistry.getSong(username2, discriminator);
        
        assertNotNull(config1);
        assertNotNull(config2);
        assertEquals(fileName1, config1.fileName);
        assertEquals(fileName2, config2.fileName);
    }

    @Test
    @DisplayName("사용자명의 특수 문자를 처리한다")
    void shouldHandleSpecialCharactersInUsername() {
        // given
        String username = "user with spaces";
        String discriminator = "1234";
        String fileName = "test.mp3";

        // when
        EntrySongRegistry.setSong(username, discriminator, fileName, 0, 10);
        EntrySongConfig result = EntrySongRegistry.getSong(username, discriminator);

        // then
        assertNotNull(result);
        assertEquals(fileName, result.fileName);
    }
}

