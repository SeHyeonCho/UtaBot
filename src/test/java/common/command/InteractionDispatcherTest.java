package common.command;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.requests.restaction.CommandListUpdateAction;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * InteractionDispatcher 테스트 클래스
 *
 * 테스트 대상:
 * - 명령어 이름으로 알맞은 명령어 실행, 서버 전용 명령어는 서버에서만
 * - 버튼 ID 앞부분으로 알맞은 버튼 핸들러 실행
 * - 이름/앞부분이 겹치면 시작할 때 막기
 * - 서버에 명령어 등록
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class InteractionDispatcherTest {

    @Mock
    private SlashCommand ping;

    @Mock
    private SlashCommand echo;

    @Mock
    private ButtonHandler musicButtons;

    @Mock
    private SlashCommandInteractionEvent event;

    @Mock
    private ButtonInteractionEvent buttonEvent;

    @Mock
    private Guild guild;

    @Mock
    private CommandListUpdateAction guildUpdateAction;

    @Mock
    private ReplyCallbackAction replyAction;

    @BeforeEach
    void setUp() {
        when(ping.data()).thenReturn(Commands.slash("ping", "핑"));
        when(echo.data()).thenReturn(Commands.slash("echo", "에코"));
        when(ping.guildOnly()).thenReturn(true);
        when(echo.guildOnly()).thenReturn(true);
        when(musicButtons.idPrefix()).thenReturn("music");
        when(guild.updateCommands()).thenReturn(guildUpdateAction);
        when(guildUpdateAction.addCommands(anyCollection())).thenReturn(guildUpdateAction);
        when(event.isFromGuild()).thenReturn(true);
        when(event.reply(anyString())).thenReturn(replyAction);
        when(replyAction.setEphemeral(anyBoolean())).thenReturn(replyAction);
        when(buttonEvent.getGuild()).thenReturn(guild);
    }

    private InteractionDispatcher dispatcher() {
        return new InteractionDispatcher(List.of(ping, echo), List.of(musicButtons));
    }

    // ─────────────────────────────
    // 슬래시 명령어
    // ─────────────────────────────

    @Test
    @DisplayName("명령어 이름에 맞는 명령어만 실행한다")
    void shouldExecuteCommandMatchingName() {
        // given
        when(event.getName()).thenReturn("echo");

        // when
        dispatcher().onSlashCommandInteraction(event);

        // then
        verify(echo).execute(event);
        verify(ping, never()).execute(any());
    }

    @Test
    @DisplayName("모르는 명령어는 무시한다")
    void shouldIgnoreUnknownCommand() {
        // given
        when(event.getName()).thenReturn("unknown");

        // when & then
        assertDoesNotThrow(() -> dispatcher().onSlashCommandInteraction(event));
        verify(ping, never()).execute(any());
        verify(echo, never()).execute(any());
    }

    @Test
    @DisplayName("서버 전용 명령어를 DM에서 쓰면 안내하고 실행하지 않는다")
    void shouldRejectGuildOnlyCommandOutsideGuild() {
        // given
        when(event.getName()).thenReturn("ping");
        when(event.isFromGuild()).thenReturn(false);

        // when
        dispatcher().onSlashCommandInteraction(event);

        // then
        verify(ping, never()).execute(any());
        verify(event).reply("서버에서만 사용할 수 있어요.");
    }

    @Test
    @DisplayName("서버 전용이 아닌 명령어는 DM에서도 실행한다")
    void shouldExecuteNonGuildCommandOutsideGuild() {
        // given
        when(ping.guildOnly()).thenReturn(false);
        when(event.getName()).thenReturn("ping");
        when(event.isFromGuild()).thenReturn(false);

        // when
        dispatcher().onSlashCommandInteraction(event);

        // then
        verify(ping).execute(event);
    }

    @Test
    @DisplayName("이름이 같은 명령어가 두 개면 시작할 때 막는다")
    void shouldRejectDuplicateCommandNames() {
        // given
        SlashCommand anotherPing = mock(SlashCommand.class);
        when(anotherPing.data()).thenReturn(Commands.slash("ping", "또 다른 핑"));

        // when & then
        assertThrows(IllegalArgumentException.class,
                () -> new InteractionDispatcher(List.of(ping, anotherPing), List.of()));
    }

    // ─────────────────────────────
    // 버튼
    // ─────────────────────────────

    @Test
    @DisplayName("버튼 ID 앞부분에 맞는 핸들러로 보낸다")
    void shouldRouteButtonByIdPrefix() {
        // given
        when(buttonEvent.getComponentId()).thenReturn("music:pause");

        // when
        dispatcher().onButtonInteraction(buttonEvent);

        // then
        verify(musicButtons).handle(buttonEvent);
    }

    @Test
    @DisplayName("맡은 핸들러가 없는 버튼과 서버 밖의 버튼은 무시한다")
    void shouldIgnoreUnknownOrNonGuildButtons() {
        // given
        when(buttonEvent.getComponentId()).thenReturn("poll:yes");

        // when
        dispatcher().onButtonInteraction(buttonEvent);
        when(buttonEvent.getComponentId()).thenReturn("music:pause");
        when(buttonEvent.getGuild()).thenReturn(null);
        dispatcher().onButtonInteraction(buttonEvent);

        // then
        verify(musicButtons, never()).handle(any());
    }

    @Test
    @DisplayName("버튼 ID 앞부분은 첫 번째 ':' 앞까지다")
    void shouldParseButtonIdPrefix() {
        assertEquals("music", InteractionDispatcher.prefixOf("music:pause"));
        assertEquals("music", InteractionDispatcher.prefixOf("music:queue:3"));
        assertEquals("music", InteractionDispatcher.prefixOf("music"));
    }

    @Test
    @DisplayName("버튼 ID 앞부분이 같은 핸들러가 두 개면 시작할 때 막는다")
    void shouldRejectDuplicateButtonPrefixes() {
        // given
        ButtonHandler another = mock(ButtonHandler.class);
        when(another.idPrefix()).thenReturn("music");

        // when & then
        assertThrows(IllegalArgumentException.class,
                () -> new InteractionDispatcher(List.of(), List.of(musicButtons, another)));
    }

    // ─────────────────────────────
    // 명령어 등록
    // ─────────────────────────────

    @Test
    @DisplayName("봇이 준비되면 전역 명령어를 비우고 모든 서버에 명령어를 등록한다")
    void shouldRegisterCommandsToAllGuildsOnReady() {
        // given
        InteractionDispatcher dispatcher = dispatcher();
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
        InteractionDispatcher dispatcher = dispatcher();
        GuildJoinEvent joinEvent = mock(GuildJoinEvent.class);
        when(joinEvent.getGuild()).thenReturn(guild);

        // when
        dispatcher.onGuildJoin(joinEvent);

        // then
        verify(guildUpdateAction).addCommands(dispatcher.getCommandData());
        verify(guildUpdateAction).queue();
    }
}
