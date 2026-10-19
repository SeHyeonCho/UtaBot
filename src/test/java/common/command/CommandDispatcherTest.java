package common.command;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.requests.restaction.CommandListUpdateAction;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.*;

/**
 * CommandDispatcher 테스트 클래스
 *
 * 테스트 대상:
 * - 명령어 이름으로 알맞은 명령어 실행
 * - 이름이 겹치는 명령어 막기
 * - 서버에 명령어 등록
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CommandDispatcherTest {

    @Mock
    private SlashCommand ping;

    @Mock
    private SlashCommand echo;

    @Mock
    private SlashCommandInteractionEvent event;

    @Mock
    private Guild guild;

    @Mock
    private CommandListUpdateAction guildUpdateAction;

    @BeforeEach
    void setUp() {
        when(ping.data()).thenReturn(Commands.slash("ping", "핑"));
        when(echo.data()).thenReturn(Commands.slash("echo", "에코"));
        when(guild.updateCommands()).thenReturn(guildUpdateAction);
        when(guildUpdateAction.addCommands(anyCollection())).thenReturn(guildUpdateAction);
    }

    @Test
    @DisplayName("명령어 이름에 맞는 명령어만 실행한다")
    void shouldExecuteCommandMatchingName() {
        // given
        CommandDispatcher dispatcher = new CommandDispatcher(List.of(ping, echo));
        when(event.getName()).thenReturn("echo");

        // when
        dispatcher.onSlashCommandInteraction(event);

        // then
        verify(echo).execute(event);
        verify(ping, never()).execute(any());
    }

    @Test
    @DisplayName("모르는 명령어는 무시한다")
    void shouldIgnoreUnknownCommand() {
        // given
        CommandDispatcher dispatcher = new CommandDispatcher(List.of(ping, echo));
        when(event.getName()).thenReturn("unknown");

        // when & then
        assertDoesNotThrow(() -> dispatcher.onSlashCommandInteraction(event));
        verify(ping, never()).execute(any());
        verify(echo, never()).execute(any());
    }

    @Test
    @DisplayName("이름이 같은 명령어가 두 개면 시작할 때 막는다")
    void shouldRejectDuplicateCommandNames() {
        // given
        SlashCommand anotherPing = mock(SlashCommand.class);
        when(anotherPing.data()).thenReturn(Commands.slash("ping", "또 다른 핑"));

        // when & then
        assertThrows(IllegalArgumentException.class,
                () -> new CommandDispatcher(List.of(ping, anotherPing)));
    }

    @Test
    @DisplayName("봇이 준비되면 전역 명령어를 비우고 모든 서버에 명령어를 등록한다")
    void shouldRegisterCommandsToAllGuildsOnReady() {
        // given
        CommandDispatcher dispatcher = new CommandDispatcher(List.of(ping, echo));
        ReadyEvent readyEvent = mock(ReadyEvent.class);
        JDA jda = mock(JDA.class);
        CommandListUpdateAction globalUpdateAction = mock(CommandListUpdateAction.class);
        when(readyEvent.getJDA()).thenReturn(jda);
        when(jda.updateCommands()).thenReturn(globalUpdateAction);
        when(jda.getGuilds()).thenReturn(List.of(guild));

        // when
        dispatcher.onReady(readyEvent);

        // then
        verify(globalUpdateAction).queue();
        verify(globalUpdateAction, never()).addCommands(anyCollection());
        verify(guildUpdateAction).addCommands(dispatcher.getCommandData());
        verify(guildUpdateAction).queue();
        assertEquals(List.of("ping", "echo"),
                dispatcher.getCommandData().stream().map(data -> data.getName()).toList());
    }

    @Test
    @DisplayName("새 서버에 들어가면 그 서버에 명령어를 등록한다")
    void shouldRegisterCommandsWhenJoiningGuild() {
        // given
        CommandDispatcher dispatcher = new CommandDispatcher(List.of(ping, echo));
        GuildJoinEvent joinEvent = mock(GuildJoinEvent.class);
        when(joinEvent.getGuild()).thenReturn(guild);

        // when
        dispatcher.onGuildJoin(joinEvent);

        // then
        verify(guildUpdateAction).addCommands(dispatcher.getCommandData());
        verify(guildUpdateAction).queue();
    }
}
