package integration;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import music.audio.AudioPlayerManagerFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 통합 테스트: youtube-source로 실제 YouTube URL 불러오기
 *
 * 주의: YouTube에 접속할 수 있어야 합니다. 실행: ./gradlew integrationTest
 */
@Tag("integration")
class YoutubeLoadIntegrationTest {

    private static final String TEST_URL = "https://www.youtube.com/watch?v=dQw4w9WgXcQ";

    private AudioPlayerManager playerManager;

    @BeforeEach
    void setUp() {
        playerManager = AudioPlayerManagerFactory.create();
    }

    @AfterEach
    void tearDown() {
        playerManager.shutdown();
    }

    @Test
    @DisplayName("youtube-source를 사용하여 YouTube URL을 로드할 수 있다")
    void shouldLoadYouTubeUrlWithYoutubeSource() throws InterruptedException {
        // given
        CountDownLatch loadLatch = new CountDownLatch(1);
        final boolean[] loadSuccess = {false};

        // when
        playerManager.loadItem(TEST_URL, new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack track) {
                loadSuccess[0] = true;
                loadLatch.countDown();
            }

            @Override
            public void playlistLoaded(AudioPlaylist playlist) {
                loadSuccess[0] = true;
                loadLatch.countDown();
            }

            @Override
            public void noMatches() {
                loadLatch.countDown();
            }

            @Override
            public void loadFailed(FriendlyException exception) {
                loadLatch.countDown();
            }
        });

        // then
        assertTrue(loadLatch.await(30, TimeUnit.SECONDS), "YouTube URL 로드가 30초 내에 완료되지 않았습니다.");
        assertTrue(loadSuccess[0], "YouTube URL 로드에 실패했습니다.");
    }
}
