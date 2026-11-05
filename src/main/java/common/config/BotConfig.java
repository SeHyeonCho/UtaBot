package common.config;

import io.github.cdimascio.dotenv.Dotenv;

import java.nio.file.Path;
import java.util.function.Function;

/**
 * 봇 설정입니다. .env 파일이나 환경 변수에서 읽습니다.
 *
 * - TOKEN (필수): 디스코드 봇 토큰
 * - DATA_DIR (기본 data): 설정 JSON 파일 폴더
 * - UPLOAD_DIR (기본 uploads): 업로드한 입장곡 mp3 폴더
 * - MUSIC_DIR (기본 music): /play 파일이름 으로 재생할 음악 폴더
 */
public record BotConfig(String token, Path dataDir, Path uploadDir, Path musicDir) {

    public static BotConfig load(Dotenv dotenv) {
        return from(dotenv::get);
    }

    /**
     * @param env 설정 이름으로 값을 찾는 함수 (없으면 null)
     * @throws IllegalStateException TOKEN이 없는 경우
     */
    public static BotConfig from(Function<String, String> env) {
        String token = env.apply("TOKEN");
        if (token == null || token.isBlank()) {
            throw new IllegalStateException(".env 파일이나 환경 변수에 TOKEN(디스코드 봇 토큰)을 설정해 주세요.");
        }

        return new BotConfig(
                token,
                path(env, "DATA_DIR", "data"),
                path(env, "UPLOAD_DIR", "uploads"),
                path(env, "MUSIC_DIR", "music"));
    }

    /**
     * @return 설정 폴더 안의 파일 경로 (예: dataFile("upload-channels.json"))
     */
    public Path dataFile(String fileName) {
        return dataDir.resolve(fileName);
    }

    // 로그에 토큰이 찍히지 않도록 토큰은 빼고 표시
    @Override
    public String toString() {
        return "BotConfig[dataDir=" + dataDir + ", uploadDir=" + uploadDir + ", musicDir=" + musicDir + "]";
    }

    private static Path path(Function<String, String> env, String key, String defaultValue) {
        String value = env.apply(key);
        return Path.of(value == null || value.isBlank() ? defaultValue : value);
    }
}
