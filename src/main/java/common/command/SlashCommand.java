package common.command;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;

/**
 * 슬래시 명령어 하나의 정의와 처리를 함께 담는 인터페이스입니다.
 *
 * 새 명령어는 이 인터페이스를 구현하고 Main에서 CommandDispatcher에 넘기면 등록됩니다.
 */
public interface SlashCommand {

    /**
     * @return Discord에 등록할 명령어 정의 (이름, 설명, 옵션)
     */
    SlashCommandData data();

    void execute(SlashCommandInteractionEvent event);
}
