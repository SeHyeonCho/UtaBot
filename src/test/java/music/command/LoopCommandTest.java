package music.command;

import music.service.MusicManager;
import music.service.RepeatMode;
import music.service.ServerMusicManager;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * LoopCommand 테스트 클래스
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class LoopCommandTest {

    @Mock
    private MusicManager musicManager;

    @Mock
    private ServerMusicManager music;

    @Mock
    private SlashCommandInteractionEvent event;

    @Mock
    private Guild guild;

    @Mock
    private OptionMapping modeOption;

    @Mock
    private ReplyCallbackAction replyAction;

    private LoopCommand command;

    @BeforeEach
    void setUp() {
        when(event.isFromGuild()).thenReturn(true);
        when(event.getGuild()).thenReturn(guild);
        when(event.reply(anyString())).thenReturn(replyAction);
        when(replyAction.setEphemeral(anyBoolean())).thenReturn(replyAction);
        when(musicManager.of(guild)).thenReturn(music);
        when(music.getRepeatMode()).thenReturn(RepeatMode.OFF);

        command = new LoopCommand(musicManager);
    }

    @Test
    @DisplayName("선택지로 반복 방식 3가지를 보여준다")
    void shouldOfferAllRepeatModes() {
        // when
        List<String> values = command.data().getOptions().get(0).getChoices().stream()
                .map(Command.Choice::getAsString).toList();

        // then
        assertEquals(List.of("off", "track", "queue"), values);
    }

    @Test
    @DisplayName("고른 반복 방식으로 바꾼다")
    void shouldSetChosenMode() {
        // given
        when(event.getOption("mode")).thenReturn(modeOption);
        when(modeOption.getAsString()).thenReturn("queue");

        // when
        command.execute(event);

        // then
        verify(music).setRepeatMode(RepeatMode.QUEUE);
    }

    @Test
    @DisplayName("방식을 비우면 다음 반복 방식으로 바꾼다")
    void shouldCycleWithoutOption() {
        // when
        command.execute(event);

        // then
        verify(music).setRepeatMode(RepeatMode.TRACK);
    }
}
