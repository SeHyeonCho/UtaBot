package music.command;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import common.command.SlashCommand;
import common.util.CommandUtils;
import music.service.MusicManager;
import music.service.ServerMusicManager;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;

/**
 * /nowplaying → 지금 재생 중인 곡, 진행 상황, 조작 버튼(⏯ ⏭ 🔁 🔀 ⏹)을 보여줍니다.
 */
public class NowPlayingCommand implements SlashCommand {

    private static final int BAR_LENGTH = 15;

    private final MusicManager musicManager;

    public NowPlayingCommand(MusicManager musicManager) {
        this.musicManager = musicManager;
    }

    @Override
    public SlashCommandData data() {
        return Commands.slash("nowplaying", "지금 재생 중인 곡과 조작 버튼을 보여줍니다.");
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        if (!CommandUtils.requireGuild(event)) {
            return;
        }

        String text = format(musicManager.of(event.getGuild()));
        if (text == null) {
            event.reply("지금 재생 중인 곡이 없어요.").setEphemeral(true).queue();
            return;
        }
        event.reply(text).addComponents(MusicControls.buttons()).queue();
    }

    /**
     * @return 지금 재생 중인 곡 정보, 재생 중인 곡이 없으면 null
     */
    static String format(ServerMusicManager music) {
        AudioTrack track = music.getPlayingTrack();
        if (track == null) {
            return null;
        }

        AudioTrackInfo info = track.getInfo();
        StringBuilder sb = new StringBuilder();
        sb.append(music.isPaused() ? "⏸ **일시정지 중**" : "🎶 **지금 재생 중**").append("\n");
        sb.append("**").append(info.title).append("**");
        if (info.author != null && !info.author.isBlank()) {
            sb.append(" - ").append(info.author);
        }
        sb.append("\n");

        if (info.isStream) {
            sb.append("🔴 LIVE");
        } else {
            long position = track.getPosition();
            sb.append(progressBar(position, track.getDuration()))
                    .append(" `").append(formatTime(position)).append(" / ")
                    .append(formatTime(track.getDuration())).append("`");
        }

        sb.append("\n🔁 ").append(music.getRepeatMode().label())
                .append(" · 대기열 ").append(music.getQueue().size()).append("곡");
        return sb.toString();
    }

    static String progressBar(long position, long duration) {
        int filled = duration <= 0 ? 0 : (int) Math.min(BAR_LENGTH - 1, position * BAR_LENGTH / duration);
        return "▬".repeat(filled) + "🔘" + "▬".repeat(BAR_LENGTH - 1 - filled);
    }

    static String formatTime(long millis) {
        long totalSeconds = Math.max(0, millis) / 1000;
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        return hours > 0
                ? String.format("%d:%02d:%02d", hours, minutes, seconds)
                : String.format("%d:%02d", minutes, seconds);
    }
}
