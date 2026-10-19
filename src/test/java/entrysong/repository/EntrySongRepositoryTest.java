package entrysong.repository;

import entrysong.model.EntrySongConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * EntrySongRepository 테스트 클래스
 *
 * 테스트 대상:
 * - save(): 서버별·유저별 입장곡 저장 및 JSON 파일 기록
 * - get(): 입장곡 조회
 * - remove(): 입장곡 제거
 * - 다시 생성했을 때 JSON 파일에서 불러오기
 *
 * 임시 폴더의 파일을 사용하므로 실제 data/ 폴더는 건드리지 않습니다.
 */
class EntrySongRepositoryTest {

    private static final long GUILD_A = 111L;
    private static final long GUILD_B = 222L;
    private static final long USER_1 = 1001L;
    private static final long USER_2 = 1002L;

    @TempDir
    Path tempDir;

    private Path storePath;
    private EntrySongRepository repository;

    @BeforeEach
    void setUp() {
        storePath = tempDir.resolve("data").resolve("server-entry-songs.json");
        repository = new EntrySongRepository(storePath);
    }

    @Test
    @DisplayName("입장곡을 설정하고 조회한다")
    void shouldSaveAndGetSong() {
        // when
        repository.save(GUILD_A, USER_1, new EntrySongConfig("1001.mp3", 0, 10));
        EntrySongConfig result = repository.get(GUILD_A, USER_1);

        // then
        assertNotNull(result);
        assertEquals("1001.mp3", result.source);
        assertEquals(0, result.startSec);
        assertEquals(10, result.durationSec);
    }

    @Test
    @DisplayName("같은 유저라도 서버마다 입장곡이 따로 저장된다")
    void shouldKeepSongsSeparatePerServer() {
        // when
        repository.save(GUILD_A, USER_1, new EntrySongConfig("https://youtu.be/a", 0, 7));
        repository.save(GUILD_B, USER_1, new EntrySongConfig("https://youtu.be/b", 3, 5));

        // then
        assertEquals("https://youtu.be/a", repository.get(GUILD_A, USER_1).source);
        assertEquals("https://youtu.be/b", repository.get(GUILD_B, USER_1).source);
    }

    @Test
    @DisplayName("기존 입장곡을 업데이트한다")
    void shouldUpdateExistingSong() {
        // when
        repository.save(GUILD_A, USER_1, new EntrySongConfig("1001.mp3", 0, 10));
        repository.save(GUILD_A, USER_1, new EntrySongConfig("1001.mp3", 5, 15));

        // then
        EntrySongConfig result = repository.get(GUILD_A, USER_1);
        assertEquals(5, result.startSec);
        assertEquals(15, result.durationSec);
    }

    @Test
    @DisplayName("입장곡이 설정되지 않았을 때 null을 반환한다")
    void shouldReturnNullWhenSongNotSet() {
        // given
        repository.save(GUILD_A, USER_1, new EntrySongConfig("1001.mp3", 0, 10));

        // when & then
        assertNull(repository.get(GUILD_A, USER_2));
        assertNull(repository.get(GUILD_B, USER_1));
    }

    @Test
    @DisplayName("입장곡을 제거한다")
    void shouldRemoveSong() {
        // given
        repository.save(GUILD_A, USER_1, new EntrySongConfig("1001.mp3", 0, 10));

        // when
        repository.remove(GUILD_A, USER_1);

        // then
        assertNull(repository.get(GUILD_A, USER_1));
        assertNull(new EntrySongRepository(storePath).get(GUILD_A, USER_1));
    }

    @Test
    @DisplayName("저장한 입장곡을 새 인스턴스에서 다시 불러온다")
    void shouldLoadSavedSongsFromDisk() {
        // given
        repository.save(GUILD_A, USER_1, new EntrySongConfig("1001.mp3", 2, 8));
        repository.save(GUILD_B, USER_2, new EntrySongConfig("https://youtu.be/b", 0, 7));

        // when
        EntrySongRepository reloaded = new EntrySongRepository(storePath);

        // then
        EntrySongConfig song1 = reloaded.get(GUILD_A, USER_1);
        assertEquals("1001.mp3", song1.source);
        assertEquals(2, song1.startSec);
        assertEquals(8, song1.durationSec);
        assertEquals("https://youtu.be/b", reloaded.get(GUILD_B, USER_2).source);
    }

    @Test
    @DisplayName("JSON 파일은 서버 ID 아래에 유저 ID로 저장된다")
    void shouldWriteGuildAndUserIdsToJson() throws Exception {
        // when
        repository.save(GUILD_A, USER_1, new EntrySongConfig("1001.mp3", 0, 10));

        // then
        String json = Files.readString(storePath);
        assertTrue(json.contains("\"" + GUILD_A + "\""));
        assertTrue(json.contains("\"" + USER_1 + "\""));
        assertTrue(json.contains("\"source\": \"1001.mp3\""));
    }

    @Test
    @DisplayName("저장 파일이 없으면 빈 상태로 시작한다")
    void shouldStartEmptyWithoutStoreFile() {
        // when
        EntrySongRepository empty = new EntrySongRepository(tempDir.resolve("none.json"));

        // then
        assertNull(empty.get(GUILD_A, USER_1));
    }
}
