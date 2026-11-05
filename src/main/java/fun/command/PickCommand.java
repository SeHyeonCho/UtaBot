package fun.command;

import common.command.SlashCommand;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * /pick choices:짜장면, 짬뽕, 탕수육 → 하나 골라주기
 */
public class PickCommand implements SlashCommand {

    private static final String OPTION_CHOICES = "choices";

    private final Random random;

    public PickCommand() {
        this(new Random());
    }

    PickCommand(Random random) {
        this.random = random;
    }

    @Override
    public SlashCommandData data() {
        return Commands.slash("pick", "여러 개 중에 하나를 골라줍니다.")
                .addOption(OptionType.STRING, OPTION_CHOICES, "쉼표(,)로 구분한 선택지 (예: 짜장면, 짬뽕, 탕수육)", true);
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        List<String> choices = splitItems(event.getOption(OPTION_CHOICES).getAsString());
        if (choices.size() < 2) {
            event.reply("⚠️ 선택지를 쉼표(,)로 구분해서 2개 이상 적어주세요. (예: 짜장면, 짬뽕)")
                    .setEphemeral(true).queue();
            return;
        }

        String picked = choices.get(random.nextInt(choices.size()));
        event.reply("🤔 고민 끝! **" + picked + "**\n(후보: " + String.join(", ", choices) + ")").queue();
    }

    /**
     * 쉼표(, 또는 ，)로 나누고 빈 항목은 뺍니다. 사다리 타기에서도 사용합니다.
     */
    static List<String> splitItems(String text) {
        return Arrays.stream(text.split("[,，]"))
                .map(String::trim)
                .filter(item -> !item.isEmpty())
                .toList();
    }
}
