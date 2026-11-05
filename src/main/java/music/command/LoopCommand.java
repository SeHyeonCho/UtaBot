package music.command;

import common.command.SlashCommand;
import common.util.CommandUtils;
import music.service.MusicManager;
import music.service.RepeatMode;
import music.service.ServerMusicManager;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;

import java.util.Locale;

/**
 * /loop mode → 반복 끄기 / 한 곡 반복 / 전체 반복. 모드를 비우면 다음 모드로 바뀝니다.
 */
public class LoopCommand implements SlashCommand {

    private static final String OPTION_MODE = "mode";

    private final MusicManager musicManager;

    public LoopCommand(MusicManager musicManager) {
        this.musicManager = musicManager;
    }

    @Override
    public SlashCommandData data() {
        OptionData mode = new OptionData(OptionType.STRING, OPTION_MODE, "반복 방식 (비우면 끔 → 한 곡 → 전체 순서로 바뀜)", false);
        for (RepeatMode repeatMode : RepeatMode.values()) {
            mode.addChoice(repeatMode.label(), repeatMode.name().toLowerCase(Locale.ROOT));
        }
        return Commands.slash("loop", "반복 재생 방식을 바꿉니다.").addOptions(mode);
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        if (!CommandUtils.requireGuild(event)) {
            return;
        }

        ServerMusicManager music = musicManager.of(event.getGuild());
        OptionMapping option = event.getOption(OPTION_MODE);
        MusicControls.Reply reply = option == null
                ? MusicControls.cycleRepeat(music)
                : MusicControls.setRepeat(music, RepeatMode.valueOf(option.getAsString().toUpperCase(Locale.ROOT)));
        event.reply(reply.message()).setEphemeral(reply.ephemeral()).queue();
    }
}
