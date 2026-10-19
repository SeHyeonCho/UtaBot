package entrysong.command;

import common.command.SlashCommand;
import common.util.CommandUtils;
import entrysong.repository.UploadChannelRepository;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;

/**
 * /setuploadchannel channel → 이 서버의 mp3 업로드 채널 설정
 */
public class SetUploadChannelCommand implements SlashCommand {

    private static final String OPTION_CHANNEL = "channel";

    private final UploadChannelRepository uploadChannelRepository;

    public SetUploadChannelCommand(UploadChannelRepository uploadChannelRepository) {
        this.uploadChannelRepository = uploadChannelRepository;
    }

    @Override
    public SlashCommandData data() {
        return Commands.slash("setuploadchannel", "이 서버의 mp3 업로드 채널을 설정합니다.")
                .addOption(OptionType.CHANNEL, OPTION_CHANNEL, "업로드용 텍스트 채널", true);
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        if (!CommandUtils.requireGuild(event)) {
            return;
        }

        GuildChannel channel = event.getOption(OPTION_CHANNEL).getAsChannel();

        if (!(channel instanceof GuildMessageChannel messageChannel)) {
            event.reply("❌ 텍스트 채널만 업로드 채널로 설정할 수 있어요!")
                    .setEphemeral(true).queue();
            return;
        }

        uploadChannelRepository.setUploadChannel(event.getGuild().getIdLong(), messageChannel.getIdLong());

        event.reply("📥 이 서버의 업로드 채널이 <#" + messageChannel.getIdLong() + "> 로 설정되었습니다!")
                .queue();
    }
}
