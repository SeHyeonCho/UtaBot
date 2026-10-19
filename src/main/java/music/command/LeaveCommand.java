package music.command;

import common.command.SlashCommand;
import common.util.CommandUtils;
import music.service.MusicManager;
import music.service.ServerMusicManager;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;

public class LeaveCommand implements SlashCommand {

    private final MusicManager musicManager;

    public LeaveCommand(MusicManager musicManager) {
        this.musicManager = musicManager;
    }

    @Override
    public SlashCommandData data() {
        return Commands.slash("leave", "보이스 채널에서 나가기");
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        if (!CommandUtils.requireGuild(event)) {
            return;
        }

        ServerMusicManager music = musicManager.of(event.getGuild());
        if (music.isConnected()) {
            music.disconnect();
            event.reply("보이스 채널에서 나왔어요.").queue();
        } else {
            event.reply("지금 어떤 보이스 채널에도 연결되어 있지 않아요.").setEphemeral(true).queue();
        }
    }
}
