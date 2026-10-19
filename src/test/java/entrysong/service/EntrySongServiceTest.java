package entrysong.service;

import entrysong.model.EntrySongConfig;
import entrysong.repository.EntrySongRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * EntrySongService 테스트 클래스
 *
 * 테스트 대상:
 * - setUrl()/setTime(): 입장곡 설정과 재생 구간 변경
 * - prepareUploadPath()/registerUpload(): 업로드 파일 규칙
 * - findPlayableSource(): 입장 시 재생할 소스 찾기
 */
class EntrySongServiceTest {

    private static final long GUILD_A = 111L;
    private static final long GUILD_B = 222L;
    private static final long USER = 1001L;

    @TempDir
    Path tempDir;

    private Path uploadDir;
    private EntrySongService service;

    @BeforeEach
    void setUp() {
        uploadDir = tempDir.resolve("uploads");
        EntrySongRepository repository = new EntrySongRepository(tempDir.resolve("entry-songs.json"));
        service = new EntrySongService(repository, uploadDir);
    }

    @Test
    @DisplayName("URL을 설정하면 기본 구간으로 저장된다")
    void shouldSetUrlWithDefaultTime() {
        // when
        EntrySongConfig config = service.setUrl(GUILD_A, USER, "https://youtu.be/abc");

        // then
        assertEquals("https://youtu.be/abc", config.source);
        assertEquals(0, config.startSec);
        assertEquals(EntrySongService.DEFAULT_URL_DURATION_SEC, config.durationSec);
    }

    @Test
    @DisplayName("재생 구간을 바꿔도 소스는 유지된다")
    void shouldKeepSourceWhenChangingTime() {
        // given
        service.setUrl(GUILD_A, USER, "https://youtu.be/abc");

        // when
        EntrySongConfig updated = service.setTime(GUILD_A, USER, 5, 3);

        // then
        assertEquals("https://youtu.be/abc", updated.source);
        assertEquals(5, updated.startSec);
        assertEquals(3, updated.durationSec);
    }

    @Test
    @DisplayName("입장곡이 없으면 재생 구간을 바꿀 수 없다")
    void shouldReturnNullWhenChangingTimeWithoutSong() {
        // when & then
        assertNull(service.setTime(GUILD_A, USER, 5, 3));
    }

    @Test
    @DisplayName("업로드 파일은 서버 폴더 아래에 userId로 저장된다")
    void shouldPrepareUploadPathPerServerAndUser() throws Exception {
        // when
        Path path = service.prepareUploadPath(GUILD_A, USER);

        // then
        assertEquals(uploadDir.resolve("111").resolve("1001.mp3"), path);
        assertTrue(Files.isDirectory(path.getParent()));
    }

    @Test
    @DisplayName("업로드를 등록하면 기본 구간으로 저장된다")
    void shouldRegisterUploadWithDefaultTime() {
        // when
        EntrySongConfig config = service.registerUpload(GUILD_A, USER);

        // then
        assertEquals("1001.mp3", config.source);
        assertEquals(0, config.startSec);
        assertEquals(EntrySongService.DEFAULT_UPLOAD_DURATION_SEC, config.durationSec);
    }

    @Test
    @DisplayName("URL 입장곡은 URL을 그대로 재생한다")
    void shouldFindUrlSource() {
        // given
        service.setUrl(GUILD_A, USER, "https://youtu.be/abc");
        service.setTime(GUILD_A, USER, 4, 6);

        // when
        EntrySongSource source = service.findPlayableSource(GUILD_A, USER);

        // then
        assertEquals("https://youtu.be/abc", source.identifier);
        assertEquals(4, source.startSec);
        assertEquals(6, source.durationSec);
    }

    @Test
    @DisplayName("업로드 입장곡은 저장된 파일 경로를 재생한다")
    void shouldFindUploadedFileSource() throws Exception {
        // given
        Path path = service.prepareUploadPath(GUILD_A, USER);
        Files.writeString(path, "mp3");
        service.registerUpload(GUILD_A, USER);

        // when
        EntrySongSource source = service.findPlayableSource(GUILD_A, USER);

        // then
        assertEquals(path.toString(), source.identifier);
    }

    @Test
    @DisplayName("업로드 파일이 사라졌으면 재생하지 않는다")
    void shouldReturnNullWhenUploadedFileMissing() {
        // given
        service.registerUpload(GUILD_A, USER);

        // when & then
        assertNull(service.findPlayableSource(GUILD_A, USER));
    }

    @Test
    @DisplayName("다른 서버에서 설정한 입장곡은 재생하지 않는다")
    void shouldNotFindSongOfAnotherServer() {
        // given
        service.setUrl(GUILD_A, USER, "https://youtu.be/abc");

        // when & then
        assertNull(service.findPlayableSource(GUILD_B, USER));
    }
}
