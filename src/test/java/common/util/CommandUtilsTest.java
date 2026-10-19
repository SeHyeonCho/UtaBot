package common.util;

import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.unions.AudioChannelUnion;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * CommandUtils 테스트 클래스
 *
 * 테스트 대상:
 * - requireGuild(): 서버에서만 사용 가능한지 검증
 * - requireVoiceChannel(): 명령어를 쓴 유저의 음성 채널 확인
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CommandUtilsTest {

    @Mock
    private SlashCommandInteractionEvent event;

    @Mock
    private ReplyCallbackAction replyAction;

    @Mock
    private Member member;

    @Mock
    private GuildVoiceState voiceState;

    @Mock
    private AudioChannelUnion channel;

    @BeforeEach
    void setUp() {
        when(event.reply(anyString())).thenReturn(replyAction);
        when(replyAction.setEphemeral(anyBoolean())).thenReturn(replyAction);
    }

    @Test
    @DisplayName("서버에서 쓴 명령어는 통과한다")
    void shouldPassWhenFromGuild() {
        // given
        when(event.isFromGuild()).thenReturn(true);

        // when & then
        assertTrue(CommandUtils.requireGuild(event));
        verify(event, never()).reply(anyString());
    }

    @Test
    @DisplayName("DM에서 쓴 명령어는 안내 메시지를 보내고 막는다")
    void shouldRejectWhenNotFromGuild() {
        // given
        when(event.isFromGuild()).thenReturn(false);

        // when & then
        assertFalse(CommandUtils.requireGuild(event));
        verify(event).reply("서버에서만 사용할 수 있어요.");
        verify(replyAction).setEphemeral(true);
    }

    @Test
    @DisplayName("음성 채널에 있으면 그 채널을 돌려준다")
    void shouldReturnVoiceChannelOfMember() {
        // given
        when(event.getMember()).thenReturn(member);
        when(member.getVoiceState()).thenReturn(voiceState);
        when(voiceState.inAudioChannel()).thenReturn(true);
        when(voiceState.getChannel()).thenReturn(channel);

        // when & then
        assertSame(channel, CommandUtils.requireVoiceChannel(event));
        verify(event, never()).reply(anyString());
    }

    @Test
    @DisplayName("음성 채널에 없으면 안내 메시지를 보내고 null을 돌려준다")
    void shouldReturnNullWhenNotInVoiceChannel() {
        // given
        when(event.getMember()).thenReturn(member);
        when(member.getVoiceState()).thenReturn(voiceState);
        when(voiceState.inAudioChannel()).thenReturn(false);

        // when & then
        assertNull(CommandUtils.requireVoiceChannel(event));
        verify(event).reply("먼저 보이스 채널에 들어가 주세요!");
    }

    @Test
    @DisplayName("멤버 정보가 없으면 null을 돌려준다")
    void shouldReturnNullWithoutMember() {
        // given
        when(event.getMember()).thenReturn(null);

        // when & then
        assertNull(CommandUtils.requireVoiceChannel(event));
    }
}
