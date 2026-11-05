package fun.command;

import common.command.SlashCommand;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;

import java.util.List;
import java.util.Random;

/**
 * /conch question → 마법의 소라고둥님께 물어보기
 */
public class ConchCommand implements SlashCommand {

    private static final String OPTION_QUESTION = "question";
    static final List<String> ANSWERS = List.of(
            "그래.",
            "안 돼.",
            "다시 한 번 물어봐.",
            "언젠가는.",
            "가만히 있어.",
            "아무것도 하지 마.",
            "둘 다 먹지 마.",
            "당연하지.",
            "절대 안 돼.",
            "지금은 때가 아니야."
    );

    private final Random random;

    public ConchCommand() {
        this(new Random());
    }

    ConchCommand(Random random) {
        this.random = random;
    }

    @Override
    public SlashCommandData data() {
        return Commands.slash("conch", "마법의 소라고둥님께 물어봅니다.")
                .addOption(OptionType.STRING, OPTION_QUESTION, "질문 (예: 오늘 야식 먹어도 될까?)", true);
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        String question = event.getOption(OPTION_QUESTION).getAsString().trim();
        event.reply("🐚 마법의 소라고둥님, **" + question + "**\n> " + answer()).queue();
    }

    String answer() {
        return ANSWERS.get(random.nextInt(ANSWERS.size()));
    }
}
