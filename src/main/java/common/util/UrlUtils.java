package common.util;

import java.net.URI;
import java.util.Locale;
import java.util.Set;

public final class UrlUtils {

    private static final Set<String> YOUTUBE_HOSTS = Set.of(
            "youtube.com", "www.youtube.com", "m.youtube.com", "music.youtube.com", "youtu.be");

    private UrlUtils() {
    }

    public static boolean isHttpUrl(String value) {
        return value.startsWith("http://") || value.startsWith("https://");
    }

    public static boolean isYoutubeUrl(String value) {
        if (!isHttpUrl(value)) {
            return false;
        }
        try {
            String host = URI.create(value).getHost();
            return host != null && YOUTUBE_HOSTS.contains(host.toLowerCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return false; // 공백 등 URL에 쓸 수 없는 문자가 있는 경우
        }
    }
}
