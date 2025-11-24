package music.service;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import net.dv8tion.jda.api.entities.Guild;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * MusicManager 테스트 클래스
 * 
 * 테스트 대상:
 * - get(): 싱글톤 인스턴스 반환
 * - of(): 서버별 ServerMusicManager 인스턴스 관리
 * - playerManager(): AudioPlayerManager 반환
 * 
 * 주의: 실제 LavaPlayer를 사용하므로 통합 테스트 성격을 가집니다.
 */
@ExtendWith(MockitoExtension.class)
class MusicManagerTest {

    @Mock
    private Guild guild1;

    @Mock
    private Guild guild2;

    @BeforeEach
    void setUp() {
        // 각 테스트에서 필요한 경우에만 stubbing 설정
    }

    @Test
    @DisplayName("싱글톤 패턴으로 동일한 인스턴스를 반환한다")
    void shouldReturnSameInstanceForSingleton() {
        // given
        MusicManager instance1 = MusicManager.get();
        MusicManager instance2 = MusicManager.get();

        // when & then
        assertSame(instance1, instance2);
        assertNotNull(instance1);
        assertNotNull(instance2);
    }

    @Test
    @DisplayName("서버에 대한 ServerMusicManager를 반환한다")
    void shouldReturnServerMusicManagerForGuild() {
        // given
        when(guild1.getIdLong()).thenReturn(123456789L);
        MusicManager musicManager = MusicManager.get();

        // when
        ServerMusicManager serverMusic1 = musicManager.of(guild1);

        // then
        assertNotNull(serverMusic1);
        assertNotNull(serverMusic1.player);
        assertNotNull(serverMusic1.scheduler);
        assertNotNull(serverMusic1.sendHandler);
    }

    @Test
    @DisplayName("같은 서버에 대해 동일한 ServerMusicManager를 반환한다")
    void shouldReturnSameServerMusicManagerForSameGuild() {
        // given
        when(guild1.getIdLong()).thenReturn(123456789L);
        MusicManager musicManager = MusicManager.get();

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
        MusicManager musicManager = MusicManager.get();

        // when
        ServerMusicManager serverMusic1 = musicManager.of(guild1);
        ServerMusicManager serverMusic2 = musicManager.of(guild2);

        // then
        assertNotSame(serverMusic1, serverMusic2);
        assertNotNull(serverMusic1);
        assertNotNull(serverMusic2);
    }

    @Test
    @DisplayName("AudioPlayerManager를 반환한다")
    void shouldReturnAudioPlayerManager() {
        // given
        MusicManager musicManager = MusicManager.get();

        // when
        AudioPlayerManager playerManager = musicManager.playerManager();

        // then
        assertNotNull(playerManager);
    }

    @Test
    @DisplayName("올바른 컴포넌트로 ServerMusicManager를 생성한다")
    void shouldCreateServerMusicManagerWithCorrectComponents() {
        // given
        when(guild1.getIdLong()).thenReturn(123456789L);
        MusicManager musicManager = MusicManager.get();

        // when
        ServerMusicManager serverMusic = musicManager.of(guild1);

        // then
        assertNotNull(serverMusic.player);
        assertNotNull(serverMusic.scheduler);
        assertNotNull(serverMusic.sendHandler);
    }

    @Test
    @DisplayName("여러 서버를 동시에 처리한다")
    void shouldHandleMultipleGuildsConcurrently() {
        // given
        when(guild1.getIdLong()).thenReturn(123456789L);
        when(guild2.getIdLong()).thenReturn(987654321L);
        MusicManager musicManager = MusicManager.get();

        // when
        ServerMusicManager serverMusic1 = musicManager.of(guild1);
        ServerMusicManager serverMusic2 = musicManager.of(guild2);
        ServerMusicManager serverMusic1Again = musicManager.of(guild1);

        // then
        assertSame(serverMusic1, serverMusic1Again);
        assertNotSame(serverMusic1, serverMusic2);
    }

    @Test
    @DisplayName("유효한 AudioPlayerManager 설정을 반환한다")
    void shouldReturnValidAudioPlayerManagerConfiguration() {
        // given
        MusicManager musicManager = MusicManager.get();

        // when
        AudioPlayerManager playerManager = musicManager.playerManager();

        // then
        assertNotNull(playerManager);
        assertNotNull(playerManager.getConfiguration());
    }

    @Test
    @DisplayName("youtube-source가 AudioPlayerManager에 등록되어 있다")
    void shouldHaveYoutubeSourceRegistered() {
        // given
        MusicManager musicManager = MusicManager.get();
        AudioPlayerManager playerManager = musicManager.playerManager();

        // when
        // youtube-source는 dev.lavalink.youtube.YoutubeAudioSourceManager 타입
        var sourceManagers = playerManager.getSourceManagers();

        // then
        assertNotNull(sourceManagers);
        assertFalse(sourceManagers.isEmpty());
        // youtube-source가 등록되어 있는지 확인
        boolean hasYoutubeSource = sourceManagers.stream()
                .anyMatch(manager -> manager.getClass().getName().contains("lavalink.youtube.YoutubeAudioSourceManager"));
        assertTrue(hasYoutubeSource, "youtube-source가 AudioPlayerManager에 등록되어 있지 않습니다.");
    }
}

