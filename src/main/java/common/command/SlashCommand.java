package common.command;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;

/**
 * 슬래시 명령어 하나의 정의와 처리를 함께 담는 인터페이스입니다.
 *
 * 새 명령어는 이 인터페이스를 구현하고 해당 기능의 BotModule.commands()에 추가하면 등록됩니다.
 */
public interface SlashCommand {

    /**
     * @return Discord에 등록할 명령어 정의 (이름, 설명, 옵션)
     */
    SlashCommandData data();

    /**
     * guildOnly()가 true면 서버에서 쓴 명령어만 전달되므로 event.getGuild()는 null이 아닙니다.
     */
    void execute(SlashCommandInteractionEvent event);

    /**
     * @return true면 서버에서만 실행 (DM에서 쓰면 디스패처가 안내 메시지를 보내고 막음)
     */
    default boolean guildOnly() {
        return true;
    }
}
