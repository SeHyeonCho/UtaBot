package music.command;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import common.command.SlashCommand;
import common.util.CommandUtils;
import music.service.MusicManager;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;

import java.util.List;

public class QueueCommand implements SlashCommand {

    private static final int DISPLAY_LIMIT = 10;

    private final MusicManager musicManager;

    public QueueCommand(MusicManager musicManager) {
        this.musicManager = musicManager;
    }

    @Override
    public SlashCommandData data() {
        return Commands.slash("queue", "대기열 보기");
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        if (!CommandUtils.requireGuild(event)) {
            return;
        }

        List<AudioTrack> queue = musicManager.of(event.getGuild()).getQueue();
        if (queue.isEmpty()) {
            event.reply("대기열이 비었어요.").setEphemeral(true).queue();
            return;
        }
        event.reply(format(queue)).queue();
    }

    static String format(List<AudioTrack> queue) {
        StringBuilder sb = new StringBuilder("**대기열**\n");
        int shown = Math.min(queue.size(), DISPLAY_LIMIT);
        for (int i = 0; i < shown; i++) {
            sb.append(i + 1).append(". ").append(queue.get(i).getInfo().title).append("\n");
        }
        if (queue.size() > shown) {
            sb.append("..."); // 너무 길면 생략
        }
        return sb.toString();
    }
}
