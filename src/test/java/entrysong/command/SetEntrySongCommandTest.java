package entrysong.command;

import entrysong.repository.EntrySongRepository;
import entrysong.service.EntrySongService;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * SetEntrySongCommand 테스트 클래스
 *
 * 테스트 대상:
 * - 유튜브 링크만 입장곡으로 설정
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SetEntrySongCommandTest {

    private static final long GUILD_ID = 111L;
    private static final long USER_ID = 1001L;

    @TempDir
    Path tempDir;

    @Mock
    private SlashCommandInteractionEvent event;

    @Mock
    private Guild guild;

    @Mock
    private User user;

    @Mock
    private OptionMapping urlOption;

    @Mock
    private ReplyCallbackAction replyAction;

    private EntrySongService service;
    private SetEntrySongCommand command;

    @BeforeEach
    void setUp() {
        service = new EntrySongService(new EntrySongRepository(tempDir.resolve("entry-songs.json")), tempDir);
        command = new SetEntrySongCommand(service);

        when(event.isFromGuild()).thenReturn(true);
        when(event.getGuild()).thenReturn(guild);
        when(event.getUser()).thenReturn(user);
        when(guild.getIdLong()).thenReturn(GUILD_ID);
        when(user.getIdLong()).thenReturn(USER_ID);
        when(event.getOption("url")).thenReturn(urlOption);
        when(event.reply(anyString())).thenReturn(replyAction);
        when(replyAction.setEphemeral(anyBoolean())).thenReturn(replyAction);
    }

    @Test
    @DisplayName("유튜브 링크는 입장곡으로 설정한다")
    void shouldSetYoutubeUrl() {
        // given
        when(urlOption.getAsString()).thenReturn("https://youtu.be/dQw4w9WgXcQ");

        // when
        command.execute(event);

        // then
        assertNotNull(service.findPlayableSource(GUILD_ID, USER_ID));
        verify(replyAction, never()).setEphemeral(true);
    }

    @Test
    @DisplayName("유튜브가 아닌 링크는 재생할 수 없으므로 설정하지 않고 안내한다")
    void shouldRejectNonYoutubeUrl() {
        // given
        when(urlOption.getAsString()).thenReturn("https://suno.com/s/zgDa46mDEBtaAZHa");

        // when
        command.execute(event);

        // then
        assertNull(service.findPlayableSource(GUILD_ID, USER_ID));
        verify(replyAction).setEphemeral(true);
    }
}
