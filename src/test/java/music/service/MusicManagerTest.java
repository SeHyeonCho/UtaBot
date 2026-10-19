package music.service;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel;
import net.dv8tion.jda.api.managers.AudioManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * MusicManager 테스트 클래스
 *
 * 테스트 대상:
 * - of(): 서버별 ServerMusicManager 인스턴스 관리
 *
 * 주의: 실제 LavaPlayer를 사용하므로 통합 테스트 성격을 가집니다.
 */
@ExtendWith(MockitoExtension.class)
class MusicManagerTest {

    @Mock
    private Guild guild1;

    @Mock
    private Guild guild2;

    private AudioPlayerManager playerManager;
    private MusicManager musicManager;

    @BeforeEach
    void setUp() {
        playerManager = new DefaultAudioPlayerManager();
        musicManager = new MusicManager(playerManager);
    }

    @AfterEach
    void tearDown() {
        playerManager.shutdown();
    }

    @Test
    @DisplayName("서버에 대한 ServerMusicManager를 반환한다")
    void shouldReturnServerMusicManagerForGuild() {
        // given
        when(guild1.getIdLong()).thenReturn(123456789L);

        // when
        ServerMusicManager serverMusic1 = musicManager.of(guild1);

        // then
        assertNotNull(serverMusic1);
        assertTrue(serverMusic1.getQueue().isEmpty());
        assertFalse(serverMusic1.isPlaying());
    }

    @Test
    @DisplayName("같은 서버에 대해 동일한 ServerMusicManager를 반환한다")
    void shouldReturnSameServerMusicManagerForSameGuild() {
        // given
        when(guild1.getIdLong()).thenReturn(123456789L);

        // when
        ServerMusicManager serverMusic1 = musicManager.of(guild1);
        ServerMusicManager serverMusic2 = musicManager.of(guild1);

        // then
        assertSame(serverMusic1, serverMusic2);
    }

    @Test
    @DisplayName("다른 서버에 대해 다른 ServerMusicManager를 반환한다")
    void shouldReturnDifferentServerMusicManagerForDifferentGuilds() {
        // given
        when(guild1.getIdLong()).thenReturn(123456789L);
        when(guild2.getIdLong()).thenReturn(987654321L);

        // when
        ServerMusicManager serverMusic1 = musicManager.of(guild1);
        ServerMusicManager serverMusic2 = musicManager.of(guild2);
        ServerMusicManager serverMusic1Again = musicManager.of(guild1);

        // then
        assertNotSame(serverMusic1, serverMusic2);
        assertSame(serverMusic1, serverMusic1Again);
    }

    @Test
    @DisplayName("서버의 AudioManager로 음성 채널에 연결한다")
    void shouldConnectWithAudioManagerOfGuild() {
        // given
        AudioManager audioManager = mock(AudioManager.class);
        AudioChannel channel = mock(AudioChannel.class);
        when(guild1.getIdLong()).thenReturn(123456789L);
        when(guild1.getAudioManager()).thenReturn(audioManager);

        // when
        musicManager.of(guild1).connect(channel);

        // then
        verify(audioManager).openAudioConnection(channel);
    }

    @Test
    @DisplayName("MusicManager마다 서버별 상태를 따로 가진다")
    void shouldKeepStateSeparatePerMusicManager() {
        // given
        when(guild1.getIdLong()).thenReturn(123456789L);
        MusicManager another = new MusicManager(playerManager);

        // when & then
        assertNotSame(musicManager.of(guild1), another.of(guild1));
    }
}
