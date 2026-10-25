package common.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * 설정을 JSON 파일로 읽고 쓰는 유틸리티입니다.
 */
public final class JsonFiles {

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .create();

    private JsonFiles() {
    }

    /**
     * @return 파일 내용, 파일이 없으면 null
     * @throws IllegalStateException 파일을 읽을 수 없거나 JSON 형식이 잘못된 경우.
     *         빈 상태로 시작하면 다음 저장 때 기존 파일을 덮어써서 설정이 사라지므로 시작을 막음
     */
    public static <T> T read(Path path, Type type) {
        if (!Files.exists(path)) {
            return null;
        }

        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            return GSON.fromJson(reader, type);
        } catch (IOException | JsonParseException e) {
            throw new IllegalStateException(path.toAbsolutePath() + " 파일을 읽을 수 없습니다. "
                    + "JSON 형식을 확인한 뒤 다시 실행해 주세요: " + e.getMessage(), e);
        }
    }

    /**
     * 임시 파일에 쓴 뒤 교체해서, 저장 도중 종료돼도 기존 파일이 깨지지 않게 합니다.
     */
    public static void writeAtomically(Path path, Object value, Type type) throws IOException {
        Path dir = path.toAbsolutePath().getParent();
        Files.createDirectories(dir);

        Path tempPath = Files.createTempFile(dir, path.getFileName().toString(), ".tmp");
        try (BufferedWriter writer = Files.newBufferedWriter(tempPath, StandardCharsets.UTF_8)) {
            GSON.toJson(value, type, writer);
        }
        Files.move(tempPath, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }
}
