package common.module;

import common.command.ButtonHandler;
import common.command.InteractionDispatcher;
import common.command.SlashCommand;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.hooks.EventListener;
import net.dv8tion.jda.api.requests.GatewayIntent;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * 봇에 들어가는 모든 BotModule을 모아서 JDA에 등록할 형태로 만들어주는 클래스입니다.
 */
public final class BotModules {

    private final List<BotModule> modules;
    private final InteractionDispatcher dispatcher;

    /**
     * @throws IllegalArgumentException 모듈끼리 명령어 이름이나 버튼 ID 앞부분이 겹치는 경우
     */
    public BotModules(List<BotModule> modules) {
        this.modules = List.copyOf(modules);

        List<SlashCommand> commands = new ArrayList<>();
        List<ButtonHandler> buttonHandlers = new ArrayList<>();
        for (BotModule module : this.modules) {
            commands.addAll(module.commands());
            buttonHandlers.addAll(module.buttonHandlers());
        }
        this.dispatcher = new InteractionDispatcher(commands, buttonHandlers);
    }

    public InteractionDispatcher dispatcher() {
        return dispatcher;
    }

    /**
     * @return JDABuilder에 등록할 리스너 (명령어/버튼 디스패처 + 각 모듈의 리스너)
     */
    public List<EventListener> eventListeners() {
        List<EventListener> listeners = new ArrayList<>();
        listeners.add(dispatcher);
        for (BotModule module : modules) {
            listeners.addAll(module.listeners());
        }
        return listeners;
    }

    /**
     * @return 모든 모듈이 필요로 하는 Gateway Intent
     */
    public Set<GatewayIntent> intents() {
        Set<GatewayIntent> intents = EnumSet.noneOf(GatewayIntent.class);
        for (BotModule module : modules) {
            intents.addAll(module.intents());
        }
        return intents;
    }

    /**
     * 각 모듈의 onReady()를 실행합니다. 한 모듈이 실패해도 나머지 모듈은 계속 실행합니다.
     */
    public void onReady(JDA jda) {
        for (BotModule module : modules) {
            try {
                module.onReady(jda);
            } catch (RuntimeException e) {
                System.out.println("[BotModules] " + module.name() + " onReady failed:");
                e.printStackTrace();
            }
        }
    }
}
