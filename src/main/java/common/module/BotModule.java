package common.module;

import common.command.ButtonHandler;
import common.command.SlashCommand;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.hooks.EventListener;
import net.dv8tion.jda.api.requests.GatewayIntent;

import java.util.List;
import java.util.Set;

/**
 * 봇 기능 하나(음악, 입장곡, 놀이 등)를 묶는 단위입니다.
 *
 * 새 기능은 BotModule을 구현하고 Main.createModules()에 추가하면
 * 명령어, 버튼, 이벤트 리스너, 필요한 Gateway Intent가 자동으로 등록됩니다.
 * 필요 없는 항목은 구현하지 않아도 됩니다.
 */
public interface BotModule {

    /**
     * @return 로그에 표시할 이름
     */
    default String name() {
        return getClass().getSimpleName();
    }

    /**
     * @return 이 기능의 슬래시 명령어
     */
    default List<SlashCommand> commands() {
        return List.of();
    }

    /**
     * @return 이 기능의 버튼 처리
     */
    default List<ButtonHandler> buttonHandlers() {
        return List.of();
    }

    /**
     * @return 그 밖의 디스코드 이벤트 리스너 (음성 채널 입장, 메시지 수신 등)
     */
    default List<EventListener> listeners() {
        return List.of();
    }

    /**
     * @return 이 기능이 받아야 하는 디스코드 이벤트 종류 (예: 음성 채널 상태가 필요하면 GUILD_VOICE_STATES)
     */
    default Set<GatewayIntent> intents() {
        return Set.of();
    }

    /**
     * 봇 로그인이 끝난 뒤 한 번 실행할 작업 (데이터 이전 등).
     */
    default void onReady(JDA jda) {
    }
}
