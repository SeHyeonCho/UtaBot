package entrysong.command;

import common.command.SlashCommand;
import common.util.CommandUtils;
import entrysong.model.EntrySongConfig;
import entrysong.service.EntrySongService;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;

/**
 * /setentrytime start/duration → 이 서버에 설정된 내 입장곡의 재생 구간 수정
 */
public class SetEntryTimeCommand implements SlashCommand {

    private static final String OPTION_START = "start";
    private static final String OPTION_DURATION = "duration";

    private final EntrySongService entrySongService;

    public SetEntryTimeCommand(EntrySongService entrySongService) {
        this.entrySongService = entrySongService;
    }

    @Override
    public SlashCommandData data() {
        return Commands.slash("setentrytime", "입장곡 재생 시간을 설정합니다.")
                .addOption(OptionType.INTEGER, OPTION_START, "시작 시간(초)", true)
                .addOption(OptionType.INTEGER, OPTION_DURATION, "재생 길이(초)", true);
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        if (!CommandUtils.requireGuild(event)) {
            return;
        }

        int start = event.getOption(OPTION_START).getAsInt();
        int duration = event.getOption(OPTION_DURATION).getAsInt();

        if (start < 0 || duration <= 0) {
            event.reply("⚠️ start ≥ 0, duration ≥ 1 이어야 합니다.")
                    .setEphemeral(true).queue();
            return;
        }

        EntrySongConfig config = entrySongService.setTime(
                event.getGuild().getIdLong(), event.getUser().getIdLong(), start, duration);

        if (config == null) {
            event.reply("❌ 이 서버에 설정된 입장곡이 없습니다.\n" +
                            "먼저 mp3 파일을 업로드하거나 `/setentrysong` 으로 URL을 설정해주세요.")
                    .setEphemeral(true).queue();
            return;
        }

        String sourceText = config.isUrl() ? "`" + config.source + "`" : "업로드한 mp3";
        event.reply("⏱️ 입장곡 재생 구간을 " +
                        start + "초 ~ " + (start + duration) + "초로 설정했습니다.\n" +
                        "(소스: " + sourceText + ")")
                .queue();
    }
}
