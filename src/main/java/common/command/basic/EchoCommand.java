package common.command.basic;

import common.command.SlashCommand;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;

public class EchoCommand implements SlashCommand {

    private static final String OPTION_TEXT = "text";

    @Override
    public SlashCommandData data() {
        return Commands.slash("echo", "입력 텍스트를 그대로 반환")
                .addOption(OptionType.STRING, OPTION_TEXT, "보낼 텍스트", true);
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        String text = event.getOption(OPTION_TEXT).getAsString();
        event.reply(text).queue();
    }
}
