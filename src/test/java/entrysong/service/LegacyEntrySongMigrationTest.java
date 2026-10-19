package entrysong.service;

import entrysong.model.EntrySongConfig;
import entrysong.repository.EntrySongRepository;
import entrysong.service.LegacyEntrySongMigration.GuildMember;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * LegacyEntrySongMigration 테스트 클래스
 *
 * 테스트 대상:
 * - 예전 data/entry-songs.json (username#discriminator 기준)을 서버별·userId 기준으로 옮기기
 * - 예전 uploads/username#discriminator.mp3 파일을 uploads/{guildId}/{userId}.mp3 로 복사
 * - 옮긴 뒤 예전 파일 백업
 */
class LegacyEntrySongMigrationTest {

    private static final long GUILD_A = 111L;
    private static final long GUILD_B = 222L;
    private static final long SEHYEON = 1001L;
    private static final long UTA = 1002L;

    @TempDir
    Path tempDir;

    private Path legacyStorePath;
    private Path uploadDir;
    private EntrySongRepository repository;
    private LegacyEntrySongMigration migration;

    @BeforeEach
    void setUp() throws Exception {
        legacyStorePath = tempDir.resolve("data").resolve("entry-songs.json");
        uploadDir = tempDir.resolve("uploads");
        Files.createDirectories(legacyStorePath.getParent());
        Files.createDirectories(uploadDir);

        repository = new EntrySongRepository(tempDir.resolve("data").resolve("server-entry-songs.json"));
        EntrySongService service = new EntrySongService(repository, uploadDir);
        migration = new LegacyEntrySongMigration(service, repository, legacyStorePath, uploadDir);
    }

    // 예전 EntrySongRegistry가 저장하던 형식 그대로
    private void writeLegacyStore(String json) throws Exception {
        Files.writeString(legacyStorePath, json);
    }

    @Test
    @DisplayName("예전 데이터가 없으면 옮길 것이 없다")
    void shouldDoNothingWithoutLegacyData() throws Exception {
        // when & then
        assertFalse(migration.hasLegacyData());
        assertEquals(0, migration.migrate(List.of(new GuildMember(GUILD_A, SEHYEON, "sehyeon", "0000"))).migratedCount);
    }

    @Test
    @DisplayName("URL 입장곡은 유저가 속한 모든 서버로 복사된다")
    void shouldCopyUrlEntryToEveryServerOfUser() throws Exception {
        // given
        writeLegacyStore("""
                {
                  "sehyeon#0000": {
                    "fileName": "https://www.youtube.com/watch?v\\u003dabc",
                    "startSec": 5,
                    "durationSec": 3
                  }
                }""");

        // when
        LegacyEntrySongMigration.Result result = migration.migrate(List.of(
                new GuildMember(GUILD_A, SEHYEON, "sehyeon", "0000"),
                new GuildMember(GUILD_B, SEHYEON, "sehyeon", "0000")
        ));

        // then
        assertEquals(2, result.migratedCount);
        for (long guildId : new long[]{GUILD_A, GUILD_B}) {
            EntrySongConfig config = repository.get(guildId, SEHYEON);
            assertEquals("https://www.youtube.com/watch?v=abc", config.source);
            assertEquals(5, config.startSec);
            assertEquals(3, config.durationSec);
        }
    }

    @Test
    @DisplayName("업로드한 mp3는 서버 폴더 아래 userId 파일로 복사되고 구간도 유지된다")
    void shouldCopyUploadedFileWithTime() throws Exception {
        // given
        writeLegacyStore("""
                {
                  "uta#0000": {
                    "fileName": "uta#0000.mp3",
                    "startSec": 2,
                    "durationSec": 8
                  }
                }""");
        Files.writeString(uploadDir.resolve("uta#0000.mp3"), "mp3-data");

        // when
        migration.migrate(List.of(new GuildMember(GUILD_A, UTA, "uta", "0000")));

        // then
        Path migratedFile = uploadDir.resolve("111").resolve("1002.mp3");
        assertEquals("mp3-data", Files.readString(migratedFile));
        EntrySongConfig config = repository.get(GUILD_A, UTA);
        assertEquals("1002.mp3", config.source);
        assertEquals(2, config.startSec);
        assertEquals(8, config.durationSec);
    }

    @Test
    @DisplayName("레지스트리가 가리키는 파일은 이름이 키와 달라도 따로 주인을 찾지 않는다")
    void shouldNotReportFileReferencedByRegistry() throws Exception {
        // given
        writeLegacyStore("""
                { "uta#0000": { "fileName": "my-song.mp3", "startSec": 0, "durationSec": 10 } }""");
        Files.writeString(uploadDir.resolve("my-song.mp3"), "mp3-data");

        // when
        LegacyEntrySongMigration.Result result =
                migration.migrate(List.of(new GuildMember(GUILD_A, UTA, "uta", "0000")));

        // then
        assertEquals(1, result.migratedCount);
        assertTrue(result.unmatchedKeys.isEmpty());
        assertEquals("mp3-data", Files.readString(uploadDir.resolve("111").resolve("1002.mp3")));
    }

    @Test
    @DisplayName("레지스트리 없이 파일만 있던 입장곡은 예전 기본 구간(0초 ~ 7초)으로 옮겨진다")
    void shouldMigrateFallbackFileWithoutRegistryEntry() throws Exception {
        // given
        Files.writeString(uploadDir.resolve("uta#0000.mp3"), "mp3-data");

        // when
        migration.migrate(List.of(new GuildMember(GUILD_A, UTA, "uta", "0000")));

        // then
        EntrySongConfig config = repository.get(GUILD_A, UTA);
        assertEquals("1002.mp3", config.source);
        assertEquals(0, config.startSec);
        assertEquals(7, config.durationSec);
    }

    @Test
    @DisplayName("새 구조에 이미 설정이 있으면 덮어쓰지 않는다")
    void shouldNotOverwriteExistingNewEntry() throws Exception {
        // given
        repository.save(GUILD_A, SEHYEON, new EntrySongConfig("https://youtu.be/new", 0, 7));
        writeLegacyStore("""
                { "sehyeon#0000": { "fileName": "https://youtu.be/old", "startSec": 0, "durationSec": 7 } }""");

        // when
        LegacyEntrySongMigration.Result result =
                migration.migrate(List.of(new GuildMember(GUILD_A, SEHYEON, "sehyeon", "0000")));

        // then
        assertEquals(0, result.migratedCount);
        assertEquals("https://youtu.be/new", repository.get(GUILD_A, SEHYEON).source);
        assertTrue(result.unmatchedKeys.isEmpty());
    }

    @Test
    @DisplayName("예전 업로드 핸들러 정규식으로 저장된 한글 이름도 찾아서 옮긴다")
    void shouldMatchKeySanitizedByOldUploadHandler() throws Exception {
        // given: 예전 업로드 핸들러는 '세현'을 '__'로 바꿔 저장했음
        writeLegacyStore("""
                { "__#2221": { "fileName": "__#2221.mp3", "startSec": 0, "durationSec": 10 } }""");
        Files.writeString(uploadDir.resolve("__#2221.mp3"), "mp3-data");

        // when
        migration.migrate(List.of(new GuildMember(GUILD_A, SEHYEON, "세현", "2221")));

        // then
        assertEquals("1001.mp3", repository.get(GUILD_A, SEHYEON).source);
    }

    @Test
    @DisplayName("여러 유저와 겹치는 키는 주인을 알 수 없어 옮기지 않는다")
    void shouldSkipAmbiguousKey() throws Exception {
        // given: 두 한글 이름이 예전 업로드 정규식으로는 같은 키 '__#0000'이 됨
        writeLegacyStore("""
                { "__#0000": { "fileName": "https://youtu.be/abc", "startSec": 0, "durationSec": 7 } }""");

        // when
        LegacyEntrySongMigration.Result result = migration.migrate(List.of(
                new GuildMember(GUILD_A, SEHYEON, "세현", "0000"),
                new GuildMember(GUILD_A, UTA, "우타", "0000")
        ));

        // then
        assertNull(repository.get(GUILD_A, SEHYEON));
        assertNull(repository.get(GUILD_A, UTA));
        assertEquals(Set.of("__#0000"), result.unmatchedKeys);
    }

    @Test
    @DisplayName("옮긴 뒤 예전 파일은 legacy 폴더로 백업하고, 주인을 못 찾은 키를 알려준다")
    void shouldBackupLegacyDataAndReportUnmatched() throws Exception {
        // given
        writeLegacyStore("""
                {
                  "sehyeon#0000": { "fileName": "https://youtu.be/abc", "startSec": 0, "durationSec": 7 },
                  "left_user#0000": { "fileName": "left_user#0000.mp3", "startSec": 0, "durationSec": 10 }
                }""");
        Files.writeString(uploadDir.resolve("left_user#0000.mp3"), "mp3-data");

        // when
        LegacyEntrySongMigration.Result result =
                migration.migrate(List.of(new GuildMember(GUILD_A, SEHYEON, "sehyeon", "0000")));

        // then
        assertEquals(1, result.migratedCount);
        assertEquals(Set.of("left_user#0000"), result.unmatchedKeys);
        assertFalse(Files.exists(legacyStorePath));
        assertTrue(Files.exists(legacyStorePath.getParent().resolve("legacy").resolve("entry-songs.json")));
        assertFalse(Files.exists(uploadDir.resolve("left_user#0000.mp3")));
        assertTrue(Files.exists(uploadDir.resolve("legacy").resolve("left_user#0000.mp3")));
        assertFalse(migration.hasLegacyData()); // 다음 실행 때 다시 옮기지 않음
    }

    @Test
    @DisplayName("레지스트리에 있지만 파일이 없으면 옮기지 않고 알려준다")
    void shouldReportEntryWhoseFileIsMissing() throws Exception {
        // given
        writeLegacyStore("""
                { "uta#0000": { "fileName": "uta#0000.mp3", "startSec": 0, "durationSec": 10 } }""");

        // when
        LegacyEntrySongMigration.Result result =
                migration.migrate(List.of(new GuildMember(GUILD_A, UTA, "uta", "0000")));

        // then
        assertNull(repository.get(GUILD_A, UTA));
        assertEquals(Set.of("uta#0000"), result.unmatchedKeys);
    }
}
