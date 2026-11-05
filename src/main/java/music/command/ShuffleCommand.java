package music.command;

import common.command.SlashCommand;
import music.service.MusicManager;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;

/**
 * /shuffle → 대기열 순서를 무작위로 섞기
 */
public class ShuffleCommand implements SlashCommand {

    private final MusicManager musicManager;

    public ShuffleCommand(MusicManager musicManager) {
        this.musicManager = musicManager;
    }

    @Override
    public SlashCommandData data() {
        return Commands.slash("shuffle", "대기열 순서를 무작위로 섞습니다.");
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        MusicControls.Reply reply = MusicControls.shuffle(musicManager.of(event.getGuild()));
        event.reply(reply.message()).setEphemeral(reply.ephemeral()).queue();
    }
}
