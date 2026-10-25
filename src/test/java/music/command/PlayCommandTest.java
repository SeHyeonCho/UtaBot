package music.command;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import com.sedmelluq.discord.lavaplayer.track.BasicAudioPlaylist;
import music.service.MusicManager;
import music.service.ServerMusicManager;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.unions.AudioChannelUnion;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.requests.restaction.WebhookMessageCreateAction;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
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

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * PlayCommand 테스트 클래스
 *
 * 테스트 대상:
 * - 음성 채널 연결 여부에 따른 접속
 * - 입력 처리: URL, 검색어, music 폴더의 파일
 * - 불러온 트랙/검색 결과/재생목록을 대기열에 추가
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PlayCommandTest {

    private static final String URL = "https://youtu.be/abc";

    @TempDir
    Path tempDir;

    @Mock
    private MusicManager musicManager;

    @Mock
    private ServerMusicManager music;

    @Mock
    private SlashCommandInteractionEvent event;

    @Mock
    private Guild guild;

    @Mock
    private Member member;

    @Mock
    private GuildVoiceState voiceState;

    @Mock
    private AudioChannelUnion userChannel;

    @Mock
    private MessageChannelUnion textChannel;

    @Mock
    private OptionMapping queryOption;

    @Mock
    private ReplyCallbackAction replyAction;

    @Mock
    private InteractionHook hook;

    @Mock
    private WebhookMessageCreateAction<?> messageAction;

    private Path musicDir;
    private PlayCommand command;

    @BeforeEach
    void setUp() throws Exception {
        when(event.isFromGuild()).thenReturn(true);
        when(event.getGuild()).thenReturn(guild);
        when(event.getMember()).thenReturn(member);
        when(event.getChannel()).thenReturn(textChannel);
        when(member.getVoiceState()).thenReturn(voiceState);
        when(voiceState.inAudioChannel()).thenReturn(true);
        when(voiceState.getChannel()).thenReturn(userChannel);
        when(event.getOption("query")).thenReturn(queryOption);
        when(queryOption.getAsString()).thenReturn(URL);
        when(event.deferReply()).thenReturn(replyAction);
        when(event.reply(anyString())).thenReturn(replyAction);
        when(replyAction.setEphemeral(anyBoolean())).thenReturn(replyAction);
        when(event.getHook()).thenReturn(hook);
        doReturn(messageAction).when(hook).sendMessage(anyString());
        when(musicManager.of(guild)).thenReturn(music);

        musicDir = Files.createDirectories(tempDir.resolve("music"));
        command = new PlayCommand(musicManager, musicDir);
    }

    private static AudioTrack track(String title) {
        AudioTrack track = mock(AudioTrack.class);
        when(track.getInfo()).thenReturn(new AudioTrackInfo(title, "가수", 1000L, "id", false, "uri"));
        return track;
    }

    private AudioLoadResultHandler executeAndCaptureHandler() {
        command.execute(event);
        ArgumentCaptor<AudioLoadResultHandler> handler = ArgumentCaptor.forClass(AudioLoadResultHandler.class);
        verify(music).load(anyString(), handler.capture());
        return handler.getValue();
    }

    // ─────────────────────────────
    // 음성 채널
    // ─────────────────────────────

    @Test
    @DisplayName("봇이 연결되어 있지 않으면 유저의 음성 채널에 접속한다")
    void shouldConnectWhenNotConnected() {
        // given
        when(music.isConnected()).thenReturn(false);

        // when
        command.execute(event);

        // then
        verify(music).connect(userChannel);
        verify(music).load(eq(URL), any(AudioLoadResultHandler.class));
    }

    @Test
    @DisplayName("봇이 이미 연결되어 있으면 다시 접속하지 않는다")
    void shouldNotReconnectWhenAlreadyConnected() {
        // given
        when(music.isConnected()).thenReturn(true);

        // when
        command.execute(event);

        // then
        verify(music, never()).connect(any());
        verify(music).load(eq(URL), any(AudioLoadResultHandler.class));
    }

    @Test
    @DisplayName("음성 채널에 없으면 불러오지 않는다")
    void shouldNotLoadWhenUserNotInVoiceChannel() {
        // given
        when(voiceState.inAudioChannel()).thenReturn(false);

        // when
        command.execute(event);

        // then
        verify(music, never()).connect(any());
        verify(music, never()).load(anyString(), any());
    }

    // ─────────────────────────────
    // 입력 처리
    // ─────────────────────────────

    @Test
    @DisplayName("URL은 그대로 불러온다")
    void shouldLoadUrlAsIs() {
        assertEquals(URL, command.toIdentifier(URL));
    }

    @Test
    @DisplayName("URL이 아닌 입력은 유튜브 검색어로 불러온다")
    void shouldSearchYoutubeForPlainText() {
        // given
        when(queryOption.getAsString()).thenReturn("  아이유 좋은날 ");

        // when
        command.execute(event);

        // then
        verify(music).load(eq("ytsearch:아이유 좋은날"), any(AudioLoadResultHandler.class));
    }

    @Test
    @DisplayName("music 폴더에 있는 파일 이름은 그 파일을 재생한다")
    void shouldPlayFileInMusicFolder() throws Exception {
        // given
        Path file = Files.writeString(musicDir.resolve("song.mp3"), "mp3");

        // when & then
        assertEquals(file.toAbsolutePath().normalize().toString(), command.toIdentifier("song.mp3"));
        assertEquals(file.toAbsolutePath().normalize().toString(), command.toIdentifier(file.toString()));
    }

    @Test
    @DisplayName("music 폴더 밖의 파일은 재생하지 않고 검색어로 처리한다")
    void shouldNotPlayFileOutsideMusicFolder() throws Exception {
        // given
        Path outside = Files.writeString(tempDir.resolve("secret.mp3"), "mp3");

        // when & then
        assertEquals("ytsearch:../secret.mp3", command.toIdentifier("../secret.mp3"));
        assertEquals("ytsearch:" + outside, command.toIdentifier(outside.toString()));
    }

    @Test
    @DisplayName("music 폴더에 없는 파일 이름은 검색어로 처리한다")
    void shouldSearchWhenFileDoesNotExist() {
        assertEquals("ytsearch:없는노래.mp3", command.toIdentifier("없는노래.mp3"));
    }

    // ─────────────────────────────
    // 불러온 결과
    // ─────────────────────────────

    @Test
    @DisplayName("불러온 트랙을 대기열에 추가하고, 오류를 알릴 채널을 붙인다")
    void shouldQueueLoadedTrack() {
        // given
        AudioLoadResultHandler handler = executeAndCaptureHandler();
        AudioTrack track = track("노래 제목");

        // when
        handler.trackLoaded(track);

        // then
        verify(track).setUserData(textChannel);
        verify(music).queue(track);
        verify(hook).sendMessage("▶️ 재생/추가: **노래 제목**");
    }

    @Test
    @DisplayName("검색 결과는 첫 번째 곡만 대기열에 추가한다")
    void shouldQueueOnlyFirstSearchResult() {
        // given
        AudioLoadResultHandler handler = executeAndCaptureHandler();
        AudioTrack first = track("첫 번째");
        AudioTrack second = track("두 번째");
        AudioPlaylist results = new BasicAudioPlaylist("Search results for: 노래", List.of(first, second), null, true);

        // when
        handler.playlistLoaded(results);

        // then
        verify(music).queue(first);
        verify(music, never()).queue(second);
        verify(hook).sendMessage("▶️ 재생/추가: **첫 번째**");
    }

    @Test
    @DisplayName("재생목록 링크는 모든 곡을 대기열에 추가한다")
    void shouldQueueWholePlaylist() {
        // given
        AudioLoadResultHandler handler = executeAndCaptureHandler();
        AudioTrack first = track("첫 번째");
        AudioTrack second = track("두 번째");
        AudioPlaylist playlist = new BasicAudioPlaylist("내 재생목록", List.of(first, second), null, false);

        // when
        handler.playlistLoaded(playlist);

        // then
        verify(music).queue(first);
        verify(music).queue(second);
        verify(hook).sendMessage("▶️ 재생목록 추가: **내 재생목록** (2곡)");
    }

    @Test
    @DisplayName("검색 결과가 없으면 검색어와 함께 알려준다")
    void shouldTellWhenSearchHasNoResults() {
        // given
        when(queryOption.getAsString()).thenReturn("없는 노래");
        AudioLoadResultHandler handler = executeAndCaptureHandler();

        // when
        handler.noMatches();

        // then
        verify(hook).sendMessage("🔍 `없는 노래` 검색 결과가 없어요.");
    }
}
