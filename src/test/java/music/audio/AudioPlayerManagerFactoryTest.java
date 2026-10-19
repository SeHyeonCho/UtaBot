package music.audio;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.local.LocalAudioSourceManager;
import dev.lavalink.youtube.YoutubeAudioSourceManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AudioPlayerManagerFactory 테스트 클래스
 *
 * 테스트 대상:
 * - create(): 음원 소스 등록
 */
class AudioPlayerManagerFactoryTest {

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
    @DisplayName("youtube-source가 AudioPlayerManager에 등록되어 있다")
    void shouldHaveYoutubeSourceRegistered() {
        assertTrue(playerManager.getSourceManagers().stream()
                        .anyMatch(manager -> manager instanceof YoutubeAudioSourceManager),
                "youtube-source가 AudioPlayerManager에 등록되어 있지 않습니다.");
    }

    @Test
    @DisplayName("업로드한 mp3 재생을 위해 로컬 파일 소스가 등록되어 있다")
    void shouldHaveLocalSourceRegistered() {
        assertTrue(playerManager.getSourceManagers().stream()
                .anyMatch(manager -> manager instanceof LocalAudioSourceManager));
    }
}
