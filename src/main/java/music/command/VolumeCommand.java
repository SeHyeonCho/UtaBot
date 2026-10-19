package music.command;

import common.command.SlashCommand;
import common.util.CommandUtils;
import music.service.MusicManager;
import music.service.ServerMusicManager;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;

public class VolumeCommand implements SlashCommand {

    private static final String OPTION_LEVEL = "level";
    static final int MIN_VOLUME = 0;
    static final int MAX_VOLUME = 150;

    private final MusicManager musicManager;

    public VolumeCommand(MusicManager musicManager) {
        this.musicManager = musicManager;
    }

    @Override
    public SlashCommandData data() {
        return Commands.slash("volume", "현재 음악 볼륨을 조절합니다.")
                .addOption(OptionType.INTEGER, OPTION_LEVEL,
                        MIN_VOLUME + "~" + MAX_VOLUME + " 사이의 볼륨 (비우면 현재 볼륨 표시)", false);
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        if (!CommandUtils.requireGuild(event)) {
            return;
        }
        ServerMusicManager music = musicManager.of(event.getGuild());

        OptionMapping opt = event.getOption(OPTION_LEVEL);

        if (opt == null) {
            event.reply("🔊 현재 볼륨: **" + music.getVolume() + "%**").queue();
            return;
        }

        int vol = opt.getAsInt();
        if (vol < MIN_VOLUME || vol > MAX_VOLUME) {
            event.reply("⚠️ " + MIN_VOLUME + " ~ " + MAX_VOLUME + " 사이로 입력하세요.").setEphemeral(true).queue();
            return;
        }

        music.setVolume(vol);
        event.reply("✅ 볼륨을 **" + vol + "%** 로 설정했습니다.").queue();
    }
}
