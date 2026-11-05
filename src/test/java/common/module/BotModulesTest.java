package common.module;

import common.command.ButtonHandler;
import common.command.SlashCommand;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.hooks.EventListener;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * BotModules 테스트 클래스
 *
 * 테스트 대상:
 * - 여러 모듈의 명령어, 버튼, 리스너, Intent 모으기
 * - 모듈끼리 겹치는 명령어 막기
 * - onReady 실행
 */
class BotModulesTest {

    private static SlashCommand command(String name) {
        SlashCommand command = mock(SlashCommand.class);
        when(command.data()).thenReturn(Commands.slash(name, name + " 명령어"));
        return command;
    }

    private static ButtonHandler buttons(String prefix) {
        ButtonHandler handler = mock(ButtonHandler.class);
        when(handler.idPrefix()).thenReturn(prefix);
        return handler;
    }

    // 테스트용 모듈: 필요한 항목만 넘겨받아 그대로 돌려줌
    private record TestModule(List<SlashCommand> commands,
                              List<ButtonHandler> buttonHandlers,
                              List<EventListener> listeners,
                              Set<GatewayIntent> intents,
                              Runnable ready) implements BotModule {

        TestModule(List<SlashCommand> commands) {
            this(commands, List.of(), List.of(), Set.of(), () -> { });
        }

        @Override
        public void onReady(JDA jda) {
            ready.run();
        }
    }

    @Test
    @DisplayName("모든 모듈의 명령어와 버튼을 하나의 디스패처에 모은다")
    void shouldCollectCommandsFromAllModules() {
        // given
        BotModules modules = new BotModules(List.of(
                new TestModule(List.of(command("ping"))),
                new TestModule(List.of(command("play"), command("skip")), List.of(buttons("music")),
                        List.of(), Set.of(), () -> { })));

        // when
        List<String> names = modules.dispatcher().getCommandData().stream().map(data -> data.getName()).toList();

        // then
        assertEquals(List.of("ping", "play", "skip"), names);
    }

    @Test
    @DisplayName("다른 모듈끼리 명령어 이름이 겹치면 시작할 때 막는다")
    void shouldRejectDuplicateCommandsAcrossModules() {
        assertThrows(IllegalArgumentException.class, () -> new BotModules(List.of(
                new TestModule(List.of(command("play"))),
                new TestModule(List.of(command("play"))))));
    }

    @Test
    @DisplayName("JDA에 등록할 리스너는 디스패처와 각 모듈의 리스너다")
    void shouldCollectEventListeners() {
        // given
        EventListener voiceListener = mock(EventListener.class);
        BotModules modules = new BotModules(List.of(
                new TestModule(List.of(), List.of(), List.of(voiceListener), Set.of(), () -> { })));

        // when & then
        assertEquals(List.of(modules.dispatcher(), voiceListener), modules.eventListeners());
    }

    @Test
    @DisplayName("모든 모듈이 필요로 하는 Intent를 합친다")
    void shouldMergeIntents() {
        // given
        BotModules modules = new BotModules(List.of(
                new TestModule(List.of(), List.of(), List.of(), Set.of(GatewayIntent.GUILD_VOICE_STATES), () -> { }),
                new TestModule(List.of(), List.of(), List.of(),
                        Set.of(GatewayIntent.GUILD_VOICE_STATES, GatewayIntent.GUILD_MESSAGES), () -> { })));

        // when & then
        assertEquals(Set.of(GatewayIntent.GUILD_VOICE_STATES, GatewayIntent.GUILD_MESSAGES), modules.intents());
    }

    @Test
    @DisplayName("한 모듈의 onReady가 실패해도 나머지 모듈의 onReady는 실행된다")
    void shouldRunEveryOnReadyEvenIfOneFails() {
        // given
        List<String> ran = new ArrayList<>();
        BotModules modules = new BotModules(List.of(
                new TestModule(List.of(), List.of(), List.of(), Set.of(), () -> {
                    throw new IllegalStateException("이전 실패");
                }),
                new TestModule(List.of(), List.of(), List.of(), Set.of(), () -> ran.add("second"))));

        // when
        modules.onReady(mock(JDA.class));

        // then
        assertEquals(List.of("second"), ran);
    }
}
