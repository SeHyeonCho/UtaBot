package handler.music;


import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import java.util.Queue;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import util.Util;
import util.YtdlpResolver;

public class MusicCommandHandler extends ListenerAdapter {

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        switch (event.getName()) {
            case "play" -> handlePlay(event);
            case "skip" -> handleSkip(event);
            case "stop" -> handleStop(event);
            case "queue" -> handleQueue(event);
            case "volume" -> handleVolume(event);
            default -> {}
        }
    }

    private void handlePlay(SlashCommandInteractionEvent event) {
        if (!Util.requireGuild(event)) {
            return;
        }

        Member m = event.getMember();
        if (m == null || m.getVoiceState() == null || !m.getVoiceState().inAudioChannel()) {
            event.reply("먼저 보이스 채널에 들어가 주세요!").setEphemeral(true).queue();
            return;
        }
        AudioChannel ch = m.getVoiceState().getChannel();
        Guild guild = event.getGuild();
        ServerMusicManager music = MusicManager.get().of(guild);

        var audioManager = guild.getAudioManager();
        if (!audioManager.isConnected()) {
            audioManager.setSelfDeafened(true);
            audioManager.setSendingHandler(music.sendHandler);
            audioManager.openAudioConnection(ch);
        }

        String q = event.getOption("query").getAsString();
        event.deferReply().queue();

        YtdlpResolver.resolveAudioUrl(q).whenComplete((streamUrl, err) -> {
            if (err != null) {
                event.getHook().sendMessage("yt-dlp 로드 실패: " + err.getMessage()).queue();
                return;
            }

            MusicManager.get().playerManager().loadItemOrdered(music, streamUrl, new AudioLoadResultHandler() {
                @Override
                public void trackLoaded(AudioTrack track) {
                    music.scheduler.queue(track);
                    event.getHook().sendMessage("▶️ 재생/추가: **" + track.getInfo().title + "**").queue();
                }

                @Override
                public void playlistLoaded(AudioPlaylist playlist) {
                    AudioTrack first = playlist.getSelectedTrack();
                    if (first == null && !playlist.getTracks().isEmpty()) {
                        first = playlist.getTracks().get(0);
                    }
                    if (first != null) {
                        music.scheduler.queue(first);
                        event.getHook().sendMessage("▶️ 재생/추가: **" + first.getInfo().title + "** (플레이리스트)").queue();
                    } else {
                        event.getHook().sendMessage("플레이리스트를 불러왔지만 트랙이 없어요.").queue();
                    }
                }

                @Override
                public void noMatches() {
                    event.getHook().sendMessage("스트림 URL을 찾지 못했어요.").queue();
                }

                @Override
                public void loadFailed(FriendlyException e) {
                    e.printStackTrace();
                    event.getHook().sendMessage("로드 실패: " + e.getMessage()).queue();
                }
            });
        });
    }

    private void handleSkip(SlashCommandInteractionEvent event) {
        if (!Util.requireGuild(event)) {
            return;
        }

        ServerMusicManager music = MusicManager.get().of(event.getGuild());
        music.scheduler.nextTrack();
        event.reply("⏭ 다음 트랙으로 넘어갑니다.").queue();
    }

    private void handleStop(SlashCommandInteractionEvent event) {
        if (!Util.requireGuild(event)) {
            return;
        }

        Guild guild = event.getGuild();
        ServerMusicManager music = MusicManager.get().of(guild);
        music.player.stopTrack();
        music.scheduler.getQueue().clear();
        var audioManager = guild.getAudioManager();
        if (audioManager.isConnected()) audioManager.closeAudioConnection();
        event.reply("⏹ 정지하고 연결을 종료했어요.").queue();
    }

    private void handleQueue(SlashCommandInteractionEvent event) {
        if (!Util.requireGuild(event)) return;
        Queue<AudioTrack> queue = MusicManager.get().of(event.getGuild()).scheduler.getQueue();
        if (queue.isEmpty()) {
            event.reply("대기열이 비었어요.").setEphemeral(true).queue();
            return;
        }
        StringBuilder sb = new StringBuilder("**대기열**\n");
        int i = 1;
        for (AudioTrack t : queue) {
            sb.append(i++).append(". ").append(t.getInfo().title).append("\n");
            if (i > 10) {
                sb.append("..."); break; } // 너무 길면 생략
        }
        event.reply(sb.toString()).queue();
    }

    private void handleVolume(SlashCommandInteractionEvent event) {
        if (!Util.requireGuild(event)) {
            return;
        }
        ServerMusicManager music = MusicManager.get().of(event.getGuild());
        AudioPlayer player = music.player;

        var opt = event.getOption("level");

        if (opt == null) {
            event.reply("🔊 현재 볼륨: **" + player.getVolume() + "%**").queue();
            return;
        }

        int vol = opt.getAsInt();
        if (vol < 0 || vol > 150) {
            event.reply("⚠️ 0 ~ 150 사이로 입력하세요.").setEphemeral(true).queue();
            return;
        }

        player.setVolume(vol);
        event.reply("✅ 볼륨을 **" + vol + "%** 로 설정했습니다.").queue();
    }
}
