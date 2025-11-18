package common.util;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;

/**
 * yt-dlp를 사용하여 URL 또는 검색어를 오디오 스트림 URL로 변환하는 클래스
 */
public class AudioUrlResolver {
    public static CompletableFuture<String> resolveAudioUrl(String input) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // URL이면 그대로, 검색어면 ytsearch로 유튜브 URL 하나 뽑기(-g는 직접 스트림 URL)
                // 검색어에도 바로 -g를 써도 되지만, 여기선 유연하게 처리
                ProcessBuilder pb;
                if (isUrl(input)) {
                    pb = new ProcessBuilder("yt-dlp", "-f", "bestaudio", "-g", input);
                } else {
                    // 검색어 → ytsearch1: 하나만 → 그 결과의 스트림 URL
                    pb = new ProcessBuilder("yt-dlp", "ytsearch1:" + input, "-f", "bestaudio", "-g");
                }
                pb.redirectErrorStream(true);
                Process proc = pb.start();

                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(proc.getInputStream(), StandardCharsets.UTF_8))) {
                    String line = br.readLine(); // 첫 줄: 실제 오디오 스트림 URL
                    int code = proc.waitFor();
                    if (code != 0 || line == null || line.isBlank()) {
                        throw new RuntimeException("yt-dlp 실패(code=" + code + ")");
                    }
                    return line.trim();
                }
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private static boolean isUrl(String s) {
        return s.startsWith("http://") || s.startsWith("https://");
    }
}


