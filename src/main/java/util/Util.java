package util;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

public class Util {

    private Util() {
    }

    public static boolean requireGuild(SlashCommandInteractionEvent event) {
        if (!event.isFromGuild()) {
            event.reply("서버에서만 사용할 수 있어요.")
                    .setEphemeral(true)
                    .queue();
            return false;
        }
        return true;
    }
}
