package music.command;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import music.service.MusicManager;
import music.service.ServerMusicManager;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.unions.AudioChannelUnion;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.requests.restaction.WebhookMessageCreateAction;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

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
 * - 불러온 트랙을 대기열에 추가
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PlayCommandTest {

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
    private OptionMapping queryOption;

    @Mock
    private ReplyCallbackAction replyAction;

    @Mock
    private InteractionHook hook;

    @Mock
    private WebhookMessageCreateAction<?> messageAction;

    private PlayCommand command;

    @BeforeEach
    void setUp() {
        when(event.isFromGuild()).thenReturn(true);
        when(event.getGuild()).thenReturn(guild);
        when(event.getMember()).thenReturn(member);
        when(member.getVoiceState()).thenReturn(voiceState);
        when(voiceState.inAudioChannel()).thenReturn(true);
        when(voiceState.getChannel()).thenReturn(userChannel);
        when(event.getOption("query")).thenReturn(queryOption);
        when(queryOption.getAsString()).thenReturn("https://youtu.be/abc");
        when(event.deferReply()).thenReturn(replyAction);
        when(event.reply(anyString())).thenReturn(replyAction);
        when(replyAction.setEphemeral(anyBoolean())).thenReturn(replyAction);
        when(event.getHook()).thenReturn(hook);
        doReturn(messageAction).when(hook).sendMessage(anyString());
        when(musicManager.of(guild)).thenReturn(music);

        command = new PlayCommand(musicManager);
    }

    @Test
    @DisplayName("봇이 연결되어 있지 않으면 유저의 음성 채널에 접속한다")
    void shouldConnectWhenNotConnected() {
        // given
        when(music.isConnected()).thenReturn(false);

        // when
        command.execute(event);

        // then
        verify(music).connect(userChannel);
        verify(music).load(eq("https://youtu.be/abc"), any(AudioLoadResultHandler.class));
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
        verify(music).load(eq("https://youtu.be/abc"), any(AudioLoadResultHandler.class));
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

    @Test
    @DisplayName("불러온 트랙을 대기열에 추가하고 제목을 알려준다")
    void shouldQueueLoadedTrack() {
        // given
        command.execute(event);
        ArgumentCaptor<AudioLoadResultHandler> handler = ArgumentCaptor.forClass(AudioLoadResultHandler.class);
        verify(music).load(anyString(), handler.capture());
        AudioTrack track = mock(AudioTrack.class);
        when(track.getInfo()).thenReturn(new AudioTrackInfo("노래 제목", "가수", 1000L, "id", false, "uri"));

        // when
        handler.getValue().trackLoaded(track);

        // then
        verify(music).queue(track);
        verify(hook).sendMessage("▶️ 재생/추가: **노래 제목**");
    }
}
