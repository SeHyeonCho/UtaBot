package music.command;

import common.command.SlashCommand;
import common.util.CommandUtils;
import music.service.MusicManager;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;

/**
 * /pause → 일시정지, 다시 쓰면 이어서 재생
 */
public class PauseCommand implements SlashCommand {

    private final MusicManager musicManager;

    public PauseCommand(MusicManager musicManager) {
        this.musicManager = musicManager;
    }

    @Override
    public SlashCommandData data() {
        return Commands.slash("pause", "일시정지합니다. 다시 쓰면 이어서 재생합니다.");
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        if (!CommandUtils.requireGuild(event)) {
            return;
        }

        MusicControls.Reply reply = MusicControls.togglePause(musicManager.of(event.getGuild()));
        event.reply(reply.message()).setEphemeral(reply.ephemeral()).queue();
    }
}
