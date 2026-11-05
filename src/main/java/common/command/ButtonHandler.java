package common.command;

import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;

/**
 * 한 기능의 버튼들을 처리하는 인터페이스입니다.
 *
 * 버튼 ID는 "앞부분:동작" 형식으로 만듭니다 (예: "music:pause").
 * InteractionDispatcher가 앞부분으로 핸들러를 찾아 handle()을 부릅니다.
 */
public interface ButtonHandler {

    char ID_SEPARATOR = ':';

    /**
     * @return 이 핸들러가 맡는 버튼 ID 앞부분 (예: "music"). 기능마다 겹치면 안 됨
     */
    String idPrefix();

    /**
     * 서버 안의 메시지에 달린 버튼만 전달됩니다.
     */
    void handle(ButtonInteractionEvent event);
}
