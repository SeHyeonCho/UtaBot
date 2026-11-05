package music.handler;

import music.command.MusicControls;
import music.service.MusicManager;
import music.service.ServerMusicManager;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * MusicButtonHandler 테스트 클래스
 *
 * 테스트 대상:
 * - /nowplaying 버튼 ID에 맞는 조작 실행
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MusicButtonHandlerTest {

    @Mock
    private MusicManager musicManager;

    @Mock
    private ServerMusicManager music;

    @Mock
    private ButtonInteractionEvent event;

    @Mock
    private Guild guild;

    @Mock
    private Member member;

    @Mock
    private ReplyCallbackAction replyAction;

    private MusicButtonHandler handler;

    @BeforeEach
    void setUp() {
        when(event.getGuild()).thenReturn(guild);
        when(event.getMember()).thenReturn(member);
        when(member.getEffectiveName()).thenReturn("세현");
        when(event.reply(anyString())).thenReturn(replyAction);
        when(replyAction.setEphemeral(anyBoolean())).thenReturn(replyAction);
        when(musicManager.of(guild)).thenReturn(music);

        handler = new MusicButtonHandler(musicManager);
    }

    @Test
    @DisplayName("music으로 시작하는 버튼을 맡는다")
    void shouldHandleMusicPrefix() {
        assertEquals("music", handler.idPrefix());
    }

    @Test
    @DisplayName("다음 곡 버튼을 누르면 넘기고 누가 눌렀는지 알린다")
    void shouldSkipOnSkipButton() {
        // given
        when(event.getComponentId()).thenReturn(MusicControls.SKIP);

        // when
        handler.handle(event);

        // then
        verify(music).skip();
        verify(event).reply("⏭ 다음 트랙으로 넘어갑니다. (세현)");
        verify(replyAction).setEphemeral(false);
    }

    @Test
    @DisplayName("실패한 조작은 누른 사람에게만 보여준다")
    void shouldReplyEphemerallyWhenRejected() {
        // given: 재생 중인 곡이 없는데 일시정지 버튼을 누름
        when(event.getComponentId()).thenReturn(MusicControls.PAUSE);

        // when
        handler.handle(event);

        // then
        verify(event).reply("지금 재생 중인 곡이 없어요.");
        verify(replyAction).setEphemeral(true);
    }

    @Test
    @DisplayName("모르는 동작의 버튼은 무시한다")
    void shouldIgnoreUnknownAction() {
        // given
        when(event.getComponentId()).thenReturn("music:unknown");

        // when
        handler.handle(event);

        // then
        verify(event, never()).reply(anyString());
    }
}
