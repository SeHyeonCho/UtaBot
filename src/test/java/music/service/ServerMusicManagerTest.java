package music.service;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import music.audio.AudioPlayerSendHandler;
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel;
import net.dv8tion.jda.api.entities.channel.unions.AudioChannelUnion;
import net.dv8tion.jda.api.managers.AudioManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * ServerMusicManager 테스트 클래스
 *
 * 테스트 대상:
 * - connect()/disconnect(): 음성 연결 시 항상 필요한 설정
 * - 재생 상태 조회와 볼륨
 *
 * 주의: 실제 LavaPlayer를 사용하므로 통합 테스트 성격을 가집니다.
 */
@ExtendWith(MockitoExtension.class)
class ServerMusicManagerTest {

    @Mock
    private AudioManager audioManager;

    @Mock
    private AudioChannel channel;

    private AudioPlayerManager playerManager;
    private ScheduledExecutorService executor;

    @BeforeEach
    void setUp() {
        playerManager = new DefaultAudioPlayerManager();
        executor = Executors.newSingleThreadScheduledExecutor();
    }

    @AfterEach
    void tearDown() {
        executor.shutdownNow();
        playerManager.shutdown();
    }

    @Test
    @DisplayName("접속할 때 송출 핸들러와 자기 음소거를 설정한 뒤 채널에 연결한다")
    void shouldSetSendHandlerAndDeafenBeforeConnecting() {
        // given
        ServerMusicManager serverMusic = new ServerMusicManager(playerManager, audioManager, executor);

        // when
        serverMusic.connect(channel);

        // then
        InOrder inOrder = inOrder(audioManager);
        inOrder.verify(audioManager).setSelfDeafened(true);
        inOrder.verify(audioManager).setSendingHandler(any(AudioPlayerSendHandler.class));
        inOrder.verify(audioManager).openAudioConnection(channel);
    }

    @Test
    @DisplayName("연결 해제는 AudioManager의 연결을 닫는다")
    void shouldCloseConnectionOnDisconnect() {
        // given
        ServerMusicManager serverMusic = new ServerMusicManager(playerManager, audioManager, executor);

        // when
        serverMusic.disconnect();

        // then
        verify(audioManager).closeAudioConnection();
    }

    @Test
    @DisplayName("연결 상태와 접속한 채널을 AudioManager에서 가져온다")
    void shouldReadConnectionStateFromAudioManager() {
        // given
        AudioChannelUnion connected = mock(AudioChannelUnion.class);
        when(audioManager.isConnected()).thenReturn(true);
        when(audioManager.getConnectedChannel()).thenReturn(connected);
        ServerMusicManager serverMusic = new ServerMusicManager(playerManager, audioManager, executor);

        // when & then
        assertTrue(serverMusic.isConnected());
        assertSame(connected, serverMusic.getConnectedChannel());
    }

    @Test
    @DisplayName("같은 매니저로 여러 인스턴스를 생성해도 서로 독립적이다")
    void shouldCreateIndependentInstances() {
        // given
        ServerMusicManager serverMusic1 = new ServerMusicManager(playerManager, audioManager, executor);
        ServerMusicManager serverMusic2 = new ServerMusicManager(playerManager, audioManager, executor);

        // when
        serverMusic1.setVolume(30);

        // then
        assertNotSame(serverMusic1, serverMusic2);
        assertEquals(30, serverMusic1.getVolume());
        assertEquals(100, serverMusic2.getVolume());
    }

    @Test
    @DisplayName("플레이어가 기본 볼륨을 가진다")
    void shouldHavePlayerWithDefaultVolume() {
        // when
        ServerMusicManager serverMusic = new ServerMusicManager(playerManager, audioManager, executor);

        // then
        assertEquals(100, serverMusic.getVolume());
    }

    @Test
    @DisplayName("초기에는 빈 큐를 가진다")
    void shouldHaveEmptyQueueInitially() {
        // when
        ServerMusicManager serverMusic = new ServerMusicManager(playerManager, audioManager, executor);

        // then
        assertTrue(serverMusic.getQueue().isEmpty());
    }

    @Test
    @DisplayName("초기에는 재생 중인 트랙이 없다")
    void shouldHaveNoPlayingTrackInitially() {
        // when
        ServerMusicManager serverMusic = new ServerMusicManager(playerManager, audioManager, executor);

        // then
        assertFalse(serverMusic.isPlaying());
    }
}
