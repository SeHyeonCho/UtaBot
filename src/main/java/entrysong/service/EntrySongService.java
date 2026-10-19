package entrysong.service;

import entrysong.model.EntrySongConfig;
import entrysong.repository.EntrySongRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 입장곡 설정과 업로드 파일 규칙을 담당하는 클래스입니다.
 *
 * 입장곡은 서버별로 따로 저장하고, 유저는 userId로 구분합니다.
 * 업로드한 mp3는 uploads/{guildId}/{userId}.mp3 에 저장합니다.
 */
public class EntrySongService {

    public static final int DEFAULT_URL_DURATION_SEC = 7;
    public static final int DEFAULT_UPLOAD_DURATION_SEC = 10;

    private final EntrySongRepository repository;
    private final Path uploadDir;

    public EntrySongService(EntrySongRepository repository, Path uploadDir) {
        this.repository = repository;
        this.uploadDir = uploadDir;
    }

    /**
     * URL을 입장곡으로 설정합니다. 재생 구간은 기본값으로 초기화됩니다.
     */
    public EntrySongConfig setUrl(long guildId, long userId, String url) {
        EntrySongConfig config = new EntrySongConfig(url, 0, DEFAULT_URL_DURATION_SEC);
        repository.save(guildId, userId, config);
        return config;
    }

    /**
     * 현재 입장곡의 재생 구간을 바꿉니다.
     *
     * @return 바뀐 설정, 등록된 입장곡이 없으면 null
     */
    public EntrySongConfig setTime(long guildId, long userId, int startSec, int durationSec) {
        EntrySongConfig current = repository.get(guildId, userId);
        if (current == null) {
            return null;
        }

        EntrySongConfig updated = current.withTime(startSec, durationSec);
        repository.save(guildId, userId, updated);
        return updated;
    }

    /**
     * 업로드한 mp3를 저장할 경로를 돌려줍니다. 폴더가 없으면 만듭니다.
     */
    public Path prepareUploadPath(long guildId, long userId) throws IOException {
        Path path = uploadPath(guildId, userId);
        Files.createDirectories(path.getParent());
        return path;
    }

    /**
     * 저장이 끝난 mp3를 입장곡으로 등록합니다. 재생 구간은 기본값으로 초기화됩니다.
     */
    public EntrySongConfig registerUpload(long guildId, long userId) {
        EntrySongConfig config = new EntrySongConfig(uploadFileName(userId), 0, DEFAULT_UPLOAD_DURATION_SEC);
        repository.save(guildId, userId, config);
        return config;
    }

    /**
     * 입장할 때 재생할 입장곡을 찾습니다.
     *
     * @return 재생할 입장곡, 설정이 없거나 업로드 파일이 없으면 null
     */
    public EntrySongSource findPlayableSource(long guildId, long userId) {
        EntrySongConfig config = repository.get(guildId, userId);
        if (config == null) {
            return null;
        }

        if (config.isUrl()) {
            return new EntrySongSource(config.source, config.startSec, config.durationSec);
        }

        Path file = guildUploadDir(guildId).resolve(config.source);
        if (!Files.exists(file)) {
            System.out.println("[EntrySong] uploaded file not found: " + file.toAbsolutePath());
            return null;
        }
        return new EntrySongSource(file.toString(), config.startSec, config.durationSec);
    }

    private Path uploadPath(long guildId, long userId) {
        return guildUploadDir(guildId).resolve(uploadFileName(userId));
    }

    private Path guildUploadDir(long guildId) {
        return uploadDir.resolve(String.valueOf(guildId));
    }

    private static String uploadFileName(long userId) {
        return userId + ".mp3";
    }
}
