package fun.command;

import common.command.SlashCommand;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * /dice [sides] [count] → 주사위 굴리기
 */
public class DiceCommand implements SlashCommand {

    private static final String OPTION_SIDES = "sides";
    private static final String OPTION_COUNT = "count";
    static final int DEFAULT_SIDES = 6;
    static final int MAX_SIDES = 1000;
    static final int MAX_COUNT = 10;

    private final Random random;

    public DiceCommand() {
        this(new Random());
    }

    DiceCommand(Random random) {
        this.random = random;
    }

    @Override
    public SlashCommandData data() {
        return Commands.slash("dice", "주사위를 굴립니다.")
                .addOptions(
                        new OptionData(OptionType.INTEGER, OPTION_SIDES, "주사위 면 수 (기본 " + DEFAULT_SIDES + ")", false)
                                .setRequiredRange(2, MAX_SIDES),
                        new OptionData(OptionType.INTEGER, OPTION_COUNT, "주사위 개수 (기본 1)", false)
                                .setRequiredRange(1, MAX_COUNT));
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        int sides = event.getOption(OPTION_SIDES, DEFAULT_SIDES, option -> option.getAsInt());
        int count = event.getOption(OPTION_COUNT, 1, option -> option.getAsInt());
        event.reply(format(roll(sides, count), sides)).queue();
    }

    List<Integer> roll(int sides, int count) {
        List<Integer> rolls = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            rolls.add(random.nextInt(sides) + 1);
        }
        return rolls;
    }

    static String format(List<Integer> rolls, int sides) {
        if (rolls.size() == 1) {
            return "🎲 **" + rolls.get(0) + "** (1~" + sides + ")";
        }

        String values = rolls.stream().map(String::valueOf).collect(Collectors.joining(", "));
        int sum = rolls.stream().mapToInt(Integer::intValue).sum();
        return "🎲 " + values + " → 합계 **" + sum + "** (1~" + sides + " × " + rolls.size() + "개)";
    }
}
