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
 * /setentrysong url:링크 → 이 서버의 입장곡 소스를 URL로 설정
 */
public class SetEntrySongCommand implements SlashCommand {

    private static final String OPTION_URL = "url";

    private final EntrySongService entrySongService;

    public SetEntrySongCommand(EntrySongService entrySongService) {
        this.entrySongService = entrySongService;
    }

    @Override
    public SlashCommandData data() {
        return Commands.slash("setentrysong", "입장곡을 유튜브 링크로 설정합니다.")
                .addOption(OptionType.STRING, OPTION_URL, "유튜브 URL", true);
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        if (!CommandUtils.requireGuild(event)) {
            return;
        }

        String url = event.getOption(OPTION_URL).getAsString();

        if (!EntrySongConfig.isUrl(url)) {
            event.reply("❌ 유효한 URL을 입력해주세요. (http/https)")
                    .setEphemeral(true).queue();
            return;
        }

        EntrySongConfig config = entrySongService.setUrl(
                event.getGuild().getIdLong(), event.getUser().getIdLong(), url);

        event.reply("🎵 입장곡을 해당 링크로 설정했어요!\n" +
                        "URL: `" + url + "`\n" +
                        "구간: " + config.startSec + "초 ~ " + (config.startSec + config.durationSec) + "초\n" +
                        "원하면 `/setentrytime` 명령어로 다시 구간을 설정할 수 있어요.")
                .queue();
    }
}
