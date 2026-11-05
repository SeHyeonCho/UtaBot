package fun.command;

import common.command.SlashCommand;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * /ladder participants:철수, 영희, 민수 [results:커피, 꽝, 꽝] → 사다리 타기
 *
 * 결과를 비우면 한 명만 당첨, 나머지는 꽝입니다.
 */
public class LadderCommand implements SlashCommand {

    private static final String OPTION_PARTICIPANTS = "participants";
    private static final String OPTION_RESULTS = "results";
    static final int MAX_PARTICIPANTS = 20;
    static final String WIN = "당첨 🎉";
    static final String LOSE = "꽝";

    private final Random random;

    public LadderCommand() {
        this(new Random());
    }

    LadderCommand(Random random) {
        this.random = random;
    }

    @Override
    public SlashCommandData data() {
        return Commands.slash("ladder", "사다리 타기로 결과를 정합니다.")
                .addOption(OptionType.STRING, OPTION_PARTICIPANTS, "쉼표(,)로 구분한 참가자 (예: 철수, 영희, 민수)", true)
                .addOption(OptionType.STRING, OPTION_RESULTS, "쉼표(,)로 구분한 결과 (비우면 한 명만 당첨)", false);
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        List<String> participants = PickCommand.splitItems(event.getOption(OPTION_PARTICIPANTS).getAsString());
        OptionMapping resultsOption = event.getOption(OPTION_RESULTS);
        List<String> results = resultsOption == null
                ? defaultResults(participants.size())
                : PickCommand.splitItems(resultsOption.getAsString());

        String error = validate(participants, results);
        if (error != null) {
            event.reply("⚠️ " + error).setEphemeral(true).queue();
            return;
        }

        StringBuilder sb = new StringBuilder("🪜 **사다리 타기 결과**\n");
        match(participants, results).forEach((participant, result) ->
                sb.append(participant).append(" ➜ **").append(result).append("**\n"));
        event.reply(sb.toString()).queue();
    }

    static String validate(List<String> participants, List<String> results) {
        if (participants.size() < 2) {
            return "참가자를 쉼표(,)로 구분해서 2명 이상 적어주세요.";
        }
        if (participants.size() > MAX_PARTICIPANTS) {
            return "참가자는 " + MAX_PARTICIPANTS + "명까지만 가능해요.";
        }
        if (results.size() != participants.size()) {
            return "결과 개수(" + results.size() + "개)가 참가자 수(" + participants.size() + "명)와 같아야 해요.";
        }
        return null;
    }

    static List<String> defaultResults(int size) {
        List<String> results = new ArrayList<>(Collections.nCopies(Math.max(0, size - 1), LOSE));
        results.add(0, WIN);
        return results;
    }

    /**
     * 결과를 섞어서 참가자 순서대로 하나씩 짝지어줍니다.
     */
    Map<String, String> match(List<String> participants, List<String> results) {
        List<String> shuffled = new ArrayList<>(results);
        Collections.shuffle(shuffled, random);

        Map<String, String> matched = new LinkedHashMap<>();
        for (int i = 0; i < participants.size(); i++) {
            matched.put(participants.get(i), shuffled.get(i));
        }
        return matched;
    }
}
