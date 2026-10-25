package entrysong.command;

import common.command.SlashCommand;
import common.util.CommandUtils;
import common.util.UrlUtils;
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

        // 유튜브 외의 링크는 입장할 때 재생에 실패하므로 설정 단계에서 막음
        if (!UrlUtils.isYoutubeUrl(url)) {
            event.reply("❌ 유튜브 링크만 입장곡으로 설정할 수 있어요. (youtube.com, youtu.be)\n" +
                            "mp3 파일은 업로드 채널에 올려주세요.")
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
