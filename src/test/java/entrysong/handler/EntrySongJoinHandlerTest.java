package entrysong.handler;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import entrysong.repository.EntrySongRepository;
import entrysong.service.EntrySongService;
import music.service.MusicManager;
import music.service.ServerMusicManager;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.unions.AudioChannelUnion;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.nio.file.Path;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * EntrySongJoinHandler 테스트 클래스
 *
 * 테스트 대상:
 * - 입장한 유저의 입장곡 재생
 * - 봇이 다른 채널에서 재생 중일 때 이동하지 않기
 * - 재생 구간 적용
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EntrySongJoinHandlerTest {

    private static final long GUILD_ID = 111L;
    private static final long USER_ID = 1001L;
    private static final String URL = "https://youtu.be/abc";

    @TempDir
    Path tempDir;

    @Mock
    private MusicManager musicManager;

    @Mock
    private ServerMusicManager music;

    @Mock
    private GuildVoiceUpdateEvent event;

    @Mock
    private Guild guild;

    @Mock
    private Member member;

    @Mock
    private User user;

    @Mock
    private AudioChannelUnion joinedChannel;

    @Mock
    private AudioChannelUnion otherChannel;

    private EntrySongService entrySongService;
    private EntrySongJoinHandler handler;

    @BeforeEach
    void setUp() {
        EntrySongRepository repository = new EntrySongRepository(tempDir.resolve("entry-songs.json"));
        entrySongService = new EntrySongService(repository, tempDir.resolve("uploads"));

        when(event.getGuild()).thenReturn(guild);
        when(event.getMember()).thenReturn(member);
        when(event.getChannelJoined()).thenReturn(joinedChannel);
        when(guild.getIdLong()).thenReturn(GUILD_ID);
        when(member.getIdLong()).thenReturn(USER_ID);
        when(member.getUser()).thenReturn(user);
        when(joinedChannel.getIdLong()).thenReturn(10L);
        when(otherChannel.getIdLong()).thenReturn(20L);
        when(musicManager.of(guild)).thenReturn(music);

        handler = new EntrySongJoinHandler(entrySongService, musicManager);
    }

    @Test
    @DisplayName("봇이 연결되어 있지 않으면 입장한 채널에 접속해서 입장곡을 불러온다")
    void shouldConnectAndLoadWhenNotConnected() {
        // given
        entrySongService.setUrl(GUILD_ID, USER_ID, URL);

        // when
        handler.onGuildVoiceUpdate(event);

        // then
        verify(music).connect(joinedChannel);
        verify(music).load(eq(URL), any(AudioLoadResultHandler.class));
    }

    @Test
    @DisplayName("봇이 이미 같은 채널에 있으면 다시 접속하지 않는다")
    void shouldNotReconnectWhenInSameChannel() {
        // given
        entrySongService.setUrl(GUILD_ID, USER_ID, URL);
        when(music.getConnectedChannel()).thenReturn(joinedChannel);

        // when
        handler.onGuildVoiceUpdate(event);

        // then
        verify(music, never()).connect(any());
        verify(music).load(eq(URL), any(AudioLoadResultHandler.class));
    }

    @Test
    @DisplayName("봇이 다른 채널에서 재생 중이면 이동하지 않고 입장곡을 건너뛴다")
    void shouldSkipWhenPlayingInAnotherChannel() {
        // given
        entrySongService.setUrl(GUILD_ID, USER_ID, URL);
        when(music.getConnectedChannel()).thenReturn(otherChannel);
        when(music.isPlaying()).thenReturn(true);

        // when
        handler.onGuildVoiceUpdate(event);

        // then
        verify(music, never()).connect(any());
        verify(music, never()).load(anyString(), any());
    }

    @Test
    @DisplayName("봇이 다른 채널에 있어도 재생 중이 아니면 입장한 채널로 이동한다")
    void shouldMoveWhenIdleInAnotherChannel() {
        // given
        entrySongService.setUrl(GUILD_ID, USER_ID, URL);
        when(music.getConnectedChannel()).thenReturn(otherChannel);
        when(music.isPlaying()).thenReturn(false);

        // when
        handler.onGuildVoiceUpdate(event);

        // then
        verify(music).connect(joinedChannel);
        verify(music).load(eq(URL), any(AudioLoadResultHandler.class));
    }

    @Test
    @DisplayName("입장곡이 없는 유저는 아무것도 하지 않는다")
    void shouldDoNothingWithoutEntrySong() {
        // when
        handler.onGuildVoiceUpdate(event);

        // then
        verify(musicManager, never()).of(any());
    }

    @Test
    @DisplayName("다른 서버에서 설정한 입장곡은 재생하지 않는다")
    void shouldIgnoreEntrySongOfAnotherServer() {
        // given
        entrySongService.setUrl(999L, USER_ID, URL);

        // when
        handler.onGuildVoiceUpdate(event);

        // then
        verify(musicManager, never()).of(any());
    }

    @Test
    @DisplayName("봇이 입장하면 무시한다")
    void shouldIgnoreBots() {
        // given
        entrySongService.setUrl(GUILD_ID, USER_ID, URL);
        when(user.isBot()).thenReturn(true);

        // when
        handler.onGuildVoiceUpdate(event);

        // then
        verify(musicManager, never()).of(any());
    }

    @Test
    @DisplayName("채널에서 나간 경우는 무시한다")
    void shouldIgnoreLeavingChannel() {
        // given
        entrySongService.setUrl(GUILD_ID, USER_ID, URL);
        when(event.getChannelJoined()).thenReturn(null);

        // when
        handler.onGuildVoiceUpdate(event);

        // then
        verify(musicManager, never()).of(any());
    }

    @Test
    @DisplayName("불러온 입장곡을 설정한 시작 위치부터 설정한 길이만큼 재생한다")
    void shouldPlayEntrySongWithConfiguredTime() {
        // given
        entrySongService.setUrl(GUILD_ID, USER_ID, URL);
        entrySongService.setTime(GUILD_ID, USER_ID, 5, 3);
        handler.onGuildVoiceUpdate(event);
        ArgumentCaptor<AudioLoadResultHandler> loadHandler = ArgumentCaptor.forClass(AudioLoadResultHandler.class);
        verify(music).load(eq(URL), loadHandler.capture());
        AudioTrack track = mock(AudioTrack.class);
        when(track.getDuration()).thenReturn(60_000L);

        // when
        loadHandler.getValue().trackLoaded(track);

        // then
        verify(track).setPosition(5_000L);
        verify(music).playEntrySong(track, 3_000L);
    }

    @Test
    @DisplayName("시작 위치가 곡 길이보다 길면 입장곡을 재생하지 않는다")
    void shouldSkipWhenStartIsBeyondTrackLength() {
        // given
        entrySongService.setUrl(GUILD_ID, USER_ID, URL);
        entrySongService.setTime(GUILD_ID, USER_ID, 120, 3);
        handler.onGuildVoiceUpdate(event);
        ArgumentCaptor<AudioLoadResultHandler> loadHandler = ArgumentCaptor.forClass(AudioLoadResultHandler.class);
        verify(music).load(eq(URL), loadHandler.capture());
        AudioTrack track = mock(AudioTrack.class);
        when(track.getDuration()).thenReturn(60_000L);

        // when
        loadHandler.getValue().trackLoaded(track);

        // then
        verify(music, never()).playEntrySong(any(), anyLong());
    }
}
