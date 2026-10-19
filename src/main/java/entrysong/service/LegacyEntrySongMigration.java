package entrysong.service;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import entrysong.model.EntrySongConfig;
import entrysong.repository.EntrySongRepository;

import java.io.BufferedReader;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.stream.Stream;

/**
 * 예전 입장곡 데이터(username#discriminator 기준, 모든 서버 공통)를
 * 새 구조(서버별, userId 기준)로 옮기는 클래스입니다.
 *
 * 예전 데이터:
 * - data/entry-songs.json : { "username#0000": { fileName, startSec, durationSec } }
 * - uploads/username#0000.mp3
 *
 * 각 서버에서 이름이 일치하는 멤버를 찾아 그 서버의 입장곡으로 복사합니다.
 * 예전에는 모든 서버에서 같은 입장곡이 나왔으므로, 유저가 속한 모든 서버에 복사합니다.
 * 옮긴 뒤 예전 파일은 각 폴더의 legacy/ 아래로 옮겨 백업합니다.
 */
public class LegacyEntrySongMigration {

    private static final Gson GSON = new Gson();
    private static final Type LEGACY_STORE_TYPE = new TypeToken<Map<String, LegacyEntry>>(){}.getType();
    private static final String LEGACY_FILE_EXTENSION = ".mp3";
    private static final String BACKUP_DIR_NAME = "legacy";
    // 예전 코드에서 레지스트리 없이 username#discriminator.mp3 만 있을 때 쓰던 구간
    private static final int LEGACY_FALLBACK_DURATION_SEC = 7;

    private final EntrySongService service;
    private final EntrySongRepository repository;
    private final Path legacyStorePath;
    private final Path legacyUploadDir;

    public LegacyEntrySongMigration(EntrySongService service,
                                    EntrySongRepository repository,
                                    Path legacyStorePath,
                                    Path legacyUploadDir) {
        this.service = service;
        this.repository = repository;
        this.legacyStorePath = legacyStorePath;
        this.legacyUploadDir = legacyUploadDir;
    }

    /**
     * 서버 멤버 한 명의 정보입니다. JDA 객체 대신 필요한 값만 받습니다.
     */
    public static class GuildMember {
        final long guildId;
        final long userId;
        final String username;
        final String discriminator;

        public GuildMember(long guildId, long userId, String username, String discriminator) {
            this.guildId = guildId;
            this.userId = userId;
            this.username = username;
            this.discriminator = discriminator;
        }
    }

    public static class Result {
        public final int migratedCount;
        public final Set<String> unmatchedKeys;   // 어느 서버에서도 주인을 찾지 못한 예전 키

        Result(int migratedCount, Set<String> unmatchedKeys) {
            this.migratedCount = migratedCount;
            this.unmatchedKeys = unmatchedKeys;
        }
    }

    // 예전 JSON 형식
    private static class LegacyEntry {
        String fileName;
        int startSec;
        int durationSec;
    }

    private enum Outcome { NOT_FOUND, KEPT_EXISTING, MIGRATED }

    public boolean hasLegacyData() throws IOException {
        return Files.exists(legacyStorePath) || !listLegacyFiles().isEmpty();
    }

    /**
     * 예전 데이터를 새 구조로 옮기고, 예전 파일은 백업 폴더로 옮깁니다.
     * 새 구조에 이미 설정이 있는 유저는 덮어쓰지 않습니다.
     *
     * @param members 봇이 들어가 있는 모든 서버의 멤버
     */
    public Result migrate(List<GuildMember> members) throws IOException {
        Map<String, LegacyEntry> legacyEntries = loadLegacyEntries();
        List<Path> legacyFiles = listLegacyFiles();

        // 레지스트리 항목과, 레지스트리 없이 파일만 있던 입장곡의 키
        Set<String> legacyKeys = new TreeSet<>(legacyEntries.keySet());
        Set<String> registeredFiles = new HashSet<>();
        legacyEntries.values().forEach(entry -> registeredFiles.add(entry.fileName));
        for (Path file : legacyFiles) {
            if (!registeredFiles.contains(file.getFileName().toString())) {
                legacyKeys.add(keyOfLegacyFile(file));
            }
        }
        if (legacyKeys.isEmpty()) {
            return new Result(0, Set.of());
        }

        // 이름 정제 과정에서 서로 다른 유저가 같은 키가 될 수 있어, 그런 키는 주인을 알 수 없으므로 건너뜀
        Map<String, Set<Long>> userIdsByKey = new HashMap<>();
        for (GuildMember member : members) {
            for (String key : legacyKeysOf(member)) {
                userIdsByKey.computeIfAbsent(key, k -> new HashSet<>()).add(member.userId);
            }
        }

        int migratedCount = 0;
        Set<String> matchedKeys = new HashSet<>();
        for (GuildMember member : members) {
            for (String key : legacyKeysOf(member)) {
                if (!legacyKeys.contains(key)) {
                    continue;
                }
                if (userIdsByKey.get(key).size() > 1) {
                    System.out.println("[EntrySongMigration] skip ambiguous key: " + key);
                    continue;
                }

                Outcome outcome = migrateMember(member, key, legacyEntries.get(key));
                if (outcome == Outcome.NOT_FOUND) {
                    continue;
                }
                matchedKeys.add(key);
                if (outcome == Outcome.MIGRATED) {
                    migratedCount++;
                }
                break;
            }
        }

        backupLegacyData(legacyFiles);

        Set<String> unmatchedKeys = new TreeSet<>(legacyKeys);
        unmatchedKeys.removeAll(matchedKeys);
        System.out.println("[EntrySongMigration] migrated " + migratedCount + " entries" +
                (unmatchedKeys.isEmpty() ? "." : ", unmatched: " + unmatchedKeys));
        return new Result(migratedCount, Collections.unmodifiableSet(unmatchedKeys));
    }

    private Outcome migrateMember(GuildMember member, String key, LegacyEntry entry) throws IOException {
        String source;
        int startSec;
        int durationSec;

        if (entry != null) {
            source = entry.fileName;
            startSec = entry.startSec;
            durationSec = entry.durationSec;
        } else {
            source = key + LEGACY_FILE_EXTENSION;
            startSec = 0;
            durationSec = LEGACY_FALLBACK_DURATION_SEC;
        }

        if (!EntrySongConfig.isUrl(source) && !Files.exists(legacyUploadDir.resolve(source))) {
            System.out.println("[EntrySongMigration] legacy file not found for " + key + ": " + source);
            return Outcome.NOT_FOUND;
        }

        if (repository.get(member.guildId, member.userId) != null) {
            return Outcome.KEPT_EXISTING;
        }

        if (EntrySongConfig.isUrl(source)) {
            repository.save(member.guildId, member.userId, new EntrySongConfig(source, startSec, durationSec));
        } else {
            Path target = service.prepareUploadPath(member.guildId, member.userId);
            Files.copy(legacyUploadDir.resolve(source), target, StandardCopyOption.REPLACE_EXISTING);
            repository.save(member.guildId, member.userId,
                    new EntrySongConfig(target.getFileName().toString(), startSec, durationSec));
        }
        return Outcome.MIGRATED;
    }

    private Map<String, LegacyEntry> loadLegacyEntries() throws IOException {
        if (!Files.exists(legacyStorePath)) {
            return Map.of();
        }
        try (BufferedReader reader = Files.newBufferedReader(legacyStorePath, StandardCharsets.UTF_8)) {
            Map<String, LegacyEntry> loaded = GSON.fromJson(reader, LEGACY_STORE_TYPE);
            return loaded == null ? Map.of() : loaded;
        }
    }

    // uploads/ 바로 아래의 mp3만 예전 파일 (새 파일은 uploads/{guildId}/ 아래에 있음)
    private List<Path> listLegacyFiles() throws IOException {
        if (!Files.isDirectory(legacyUploadDir)) {
            return List.of();
        }
        try (Stream<Path> files = Files.list(legacyUploadDir)) {
            return files
                    .filter(Files::isRegularFile)
                    .filter(file -> file.getFileName().toString().endsWith(LEGACY_FILE_EXTENSION))
                    .toList();
        }
    }

    private void backupLegacyData(List<Path> legacyFiles) throws IOException {
        if (Files.exists(legacyStorePath)) {
            moveToBackup(legacyStorePath);
        }
        for (Path file : legacyFiles) {
            moveToBackup(file);
        }
    }

    private void moveToBackup(Path file) throws IOException {
        Path backupDir = file.toAbsolutePath().getParent().resolve(BACKUP_DIR_NAME);
        Files.createDirectories(backupDir);
        Files.move(file, backupDir.resolve(file.getFileName()), StandardCopyOption.REPLACE_EXISTING);
    }

    private static String keyOfLegacyFile(Path file) {
        String fileName = file.getFileName().toString();
        return fileName.substring(0, fileName.length() - LEGACY_FILE_EXTENSION.length());
    }

    // 예전 코드는 업로드와 입장곡 명령에서 서로 다른 정규식으로 이름을 정제했기 때문에 두 가지 키를 모두 확인
    private static Set<String> legacyKeysOf(GuildMember member) {
        Set<String> keys = new LinkedHashSet<>();
        keys.add(sanitizeLikeEntrySongHandler(member.username) + "#"
                + sanitizeLikeEntrySongHandler(member.discriminator));
        keys.add(sanitizeLikeUploadHandler(member.username) + "#"
                + sanitizeLikeUploadHandler(member.discriminator));
        return keys;
    }

    private static String sanitizeLikeEntrySongHandler(String s) {
        return s.replaceAll("[^0-9A-Za-z가-힣_.\\-]", "_");
    }

    private static String sanitizeLikeUploadHandler(String s) {
        return s.replaceAll("[^a-zA-Z0-9._-가-힣]", "_");
    }
}
