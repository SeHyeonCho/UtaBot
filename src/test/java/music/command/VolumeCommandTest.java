package music.command;

import music.service.MusicManager;
import music.service.ServerMusicManager;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * VolumeCommand 테스트 클래스
 *
 * 테스트 대상:
 * - 볼륨 범위 검사와 설정
 * - 명령어 설명과 실제 허용 범위 일치
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class VolumeCommandTest {

    @Mock
    private MusicManager musicManager;

    @Mock
    private ServerMusicManager music;

    @Mock
    private SlashCommandInteractionEvent event;

    @Mock
    private Guild guild;

    @Mock
    private OptionMapping levelOption;

    @Mock
    private ReplyCallbackAction replyAction;

    private VolumeCommand command;

    @BeforeEach
    void setUp() {
        when(event.isFromGuild()).thenReturn(true);
        when(event.getGuild()).thenReturn(guild);
        when(event.reply(anyString())).thenReturn(replyAction);
        when(replyAction.setEphemeral(anyBoolean())).thenReturn(replyAction);
        when(musicManager.of(guild)).thenReturn(music);

        command = new VolumeCommand(musicManager);
    }

    @Test
    @DisplayName("범위 안의 볼륨이면 설정한다")
    void shouldSetVolumeInRange() {
        // given
        when(event.getOption("level")).thenReturn(levelOption);
        when(levelOption.getAsInt()).thenReturn(VolumeCommand.MAX_VOLUME);

        // when
        command.execute(event);

        // then
        verify(music).setVolume(VolumeCommand.MAX_VOLUME);
    }

    @Test
    @DisplayName("범위를 벗어난 볼륨은 설정하지 않는다")
    void shouldRejectVolumeOutOfRange() {
        // given
        when(event.getOption("level")).thenReturn(levelOption);
        when(levelOption.getAsInt()).thenReturn(VolumeCommand.MAX_VOLUME + 1);

        // when
        command.execute(event);

        // then
        verify(music, never()).setVolume(anyInt());
        verify(replyAction).setEphemeral(true);
    }

    @Test
    @DisplayName("값을 비우면 현재 볼륨을 보여준다")
    void shouldShowCurrentVolumeWithoutOption() {
        // given
        when(event.getOption("level")).thenReturn(null);
        when(music.getVolume()).thenReturn(80);

        // when
        command.execute(event);

        // then
        verify(event).reply("🔊 현재 볼륨: **80%**");
        verify(music, never()).setVolume(anyInt());
    }

    @Test
    @DisplayName("명령어 설명의 범위가 실제 허용 범위와 같다")
    void shouldDescribeActualVolumeRange() {
        // when
        OptionData option = command.data().getOptions().get(0);

        // then
        assertTrue(option.getDescription().startsWith(VolumeCommand.MIN_VOLUME + "~" + VolumeCommand.MAX_VOLUME));
    }
}
