package music.service;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import net.dv8tion.jda.api.audio.AudioSendHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ServerMusicManager 테스트 클래스
 * 
 * 테스트 대상:
 * - 생성자: AudioPlayer, TrackScheduler, AudioPlayerHandler 초기화
 * - getSendHandler(): AudioSendHandler 반환
 * 
 * 주의: 실제 LavaPlayer를 사용하므로 통합 테스트 성격을 가집니다.
 */
class ServerMusicManagerTest {

    private AudioPlayerManager playerManager;

    @BeforeEach
    void setUp() {
        playerManager = new DefaultAudioPlayerManager();
    }

    @Test
    @DisplayName("모든 컴포넌트로 ServerMusicManager를 생성한다")
    void shouldCreateServerMusicManagerWithAllComponents() {
        // when
        ServerMusicManager serverMusic = new ServerMusicManager(playerManager);

        // then
        assertNotNull(serverMusic);
        assertNotNull(serverMusic.player);
        assertNotNull(serverMusic.scheduler);
        assertNotNull(serverMusic.sendHandler);
    }

    @Test
    @DisplayName("getSendHandler()로 AudioSendHandler를 반환한다")
    void shouldReturnAudioSendHandlerFromGetSendHandler() {
        // given
        ServerMusicManager serverMusic = new ServerMusicManager(playerManager);

        // when
        AudioSendHandler sendHandler = serverMusic.getSendHandler();

        // then
        assertNotNull(sendHandler);
        assertSame(serverMusic.sendHandler, sendHandler);
    }

    @Test
    @DisplayName("다른 매니저에 대해 독립적인 인스턴스를 생성한다")
    void shouldCreateIndependentInstancesForDifferentManagers() {
        // given
        AudioPlayerManager manager1 = new DefaultAudioPlayerManager();
        AudioPlayerManager manager2 = new DefaultAudioPlayerManager();

        // when
        ServerMusicManager serverMusic1 = new ServerMusicManager(manager1);
        ServerMusicManager serverMusic2 = new ServerMusicManager(manager2);

        // then
        assertNotSame(serverMusic1, serverMusic2);
        assertNotSame(serverMusic1.player, serverMusic2.player);
        assertNotSame(serverMusic1.scheduler, serverMusic2.scheduler);
    }

    @Test
    @DisplayName("같은 매니저로 여러 인스턴스를 생성한다")
    void shouldCreateMultipleInstancesWithSameManager() {
        // when
        ServerMusicManager serverMusic1 = new ServerMusicManager(playerManager);
        ServerMusicManager serverMusic2 = new ServerMusicManager(playerManager);

        // then
        assertNotSame(serverMusic1, serverMusic2);
        assertNotSame(serverMusic1.player, serverMusic2.player);
    }

    @Test
    @DisplayName("플레이어가 기본 볼륨을 가진다")
    void shouldHavePlayerWithDefaultVolume() {
        // when
        ServerMusicManager serverMusic = new ServerMusicManager(playerManager);

        // then
        assertEquals(100, serverMusic.player.getVolume());
    }

    @Test
    @DisplayName("초기에는 빈 큐를 가진다")
    void shouldHaveEmptyQueueInitially() {
        // when
        ServerMusicManager serverMusic = new ServerMusicManager(playerManager);

        // then
        assertTrue(serverMusic.scheduler.getQueue().isEmpty());
    }

    @Test
    @DisplayName("초기에는 재생 중인 트랙이 없다")
    void shouldHaveNoPlayingTrackInitially() {
        // when
        ServerMusicManager serverMusic = new ServerMusicManager(playerManager);

        // then
        assertNull(serverMusic.player.getPlayingTrack());
    }
}

