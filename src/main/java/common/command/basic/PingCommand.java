package common.command.basic;

import common.command.SlashCommand;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;

public class PingCommand implements SlashCommand {

    @Override
    public SlashCommandData data() {
        return Commands.slash("ping", "봇 지연시간 체크");
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        event.reply("Pong! " + event.getJDA().getGatewayPing() + "ms").queue();
    }
}
