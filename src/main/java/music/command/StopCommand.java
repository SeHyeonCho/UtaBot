package music.command;

import common.command.SlashCommand;
import common.util.CommandUtils;
import music.service.MusicManager;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;

public class StopCommand implements SlashCommand {

    private final MusicManager musicManager;

    public StopCommand(MusicManager musicManager) {
        this.musicManager = musicManager;
    }

    @Override
    public SlashCommandData data() {
        return Commands.slash("stop", "정지 및 연결 해제");
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        if (!CommandUtils.requireGuild(event)) {
            return;
        }

        MusicControls.Reply reply = MusicControls.stop(musicManager.of(event.getGuild()));
        event.reply(reply.message()).setEphemeral(reply.ephemeral()).queue();
    }
}
