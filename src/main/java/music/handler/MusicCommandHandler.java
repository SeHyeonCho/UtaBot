package music.handler;


import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import java.util.List;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import music.service.MusicManager;
import music.service.ServerMusicManager;
import common.util.CommandUtils;

public class MusicCommandHandler extends ListenerAdapter {

    private static final int QUEUE_DISPLAY_LIMIT = 10;

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
        if (!CommandUtils.requireGuild(event)) {
            return;
        }

        Member member = event.getMember();
        if (!isMemberInVoiceChannel(member, event)) {
            return;
        }

        AudioChannel channel = member.getVoiceState().getChannel();
        Guild guild = event.getGuild();
        ServerMusicManager music = MusicManager.get().of(guild);

        if (!music.isConnected()) {
            music.connect(channel);
        }

        String query = event.getOption("query").getAsString();
        event.deferReply().queue();

        // 모든 URL/검색어를 Lavaplayer에 직접 전달 (youtube-source가 처리)
        music.load(query, createAudioLoadResultHandler(music, event));
    }

    private boolean isMemberInVoiceChannel(Member member, SlashCommandInteractionEvent event) {
        if (member == null || member.getVoiceState() == null || !member.getVoiceState().inAudioChannel()) {
            event.reply("먼저 보이스 채널에 들어가 주세요!").setEphemeral(true).queue();
            return false;
        }
        return true;
    }

    private AudioLoadResultHandler createAudioLoadResultHandler(ServerMusicManager music,
                                                               SlashCommandInteractionEvent event) {
        return new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack track) {
                music.queue(track);
                event.getHook().sendMessage("▶️ 재생/추가: **" + track.getInfo().title + "**").queue();
            }

            @Override
            public void playlistLoaded(AudioPlaylist playlist) {
                handlePlaylistLoaded(playlist, music, event);
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
        };
    }

    private void handlePlaylistLoaded(AudioPlaylist playlist, ServerMusicManager music,
                                     SlashCommandInteractionEvent event) {
        if (playlist.getTracks().isEmpty()) {
            event.getHook().sendMessage("플레이리스트를 불러왔지만 트랙이 없어요.").queue();
            return;
        }

        // 모든 트랙을 큐에 추가
        int addedCount = 0;
        for (AudioTrack track : playlist.getTracks()) {
            music.queue(track);
            addedCount++;
        }

        String playlistName = playlist.getName() != null ? playlist.getName() : "재생목록";
        event.getHook().sendMessage("▶️ 재생목록 추가: **" + playlistName + "** (" + addedCount + "곡)").queue();
    }

    private void handleSkip(SlashCommandInteractionEvent event) {
        if (!CommandUtils.requireGuild(event)) {
            return;
        }

        MusicManager.get().of(event.getGuild()).skip();
        event.reply("⏭ 다음 트랙으로 넘어갑니다.").queue();
    }

    private void handleStop(SlashCommandInteractionEvent event) {
        if (!CommandUtils.requireGuild(event)) {
            return;
        }

        ServerMusicManager music = MusicManager.get().of(event.getGuild());
        music.stop();
        if (music.isConnected()) {
            music.disconnect();
        }
        event.reply("⏹ 정지하고 연결을 종료했어요.").queue();
    }

    private void handleQueue(SlashCommandInteractionEvent event) {
        if (!CommandUtils.requireGuild(event)) return;
        List<AudioTrack> queue = MusicManager.get().of(event.getGuild()).getQueue();
        if (queue.isEmpty()) {
            event.reply("대기열이 비었어요.").setEphemeral(true).queue();
            return;
        }
        StringBuilder sb = new StringBuilder("**대기열**\n");
        int shown = Math.min(queue.size(), QUEUE_DISPLAY_LIMIT);
        for (int i = 0; i < shown; i++) {
            sb.append(i + 1).append(". ").append(queue.get(i).getInfo().title).append("\n");
        }
        if (queue.size() > shown) {
            sb.append("..."); // 너무 길면 생략
        }
        event.reply(sb.toString()).queue();
    }

    private void handleVolume(SlashCommandInteractionEvent event) {
        if (!CommandUtils.requireGuild(event)) {
            return;
        }
        ServerMusicManager music = MusicManager.get().of(event.getGuild());

        OptionMapping opt = event.getOption("level");

        if (opt == null) {
            event.reply("🔊 현재 볼륨: **" + music.getVolume() + "%**").queue();
            return;
        }

        int vol = opt.getAsInt();
        if (vol < 0 || vol > 150) {
            event.reply("⚠️ 0 ~ 150 사이로 입력하세요.").setEphemeral(true).queue();
            return;
        }

        music.setVolume(vol);
        event.reply("✅ 볼륨을 **" + vol + "%** 로 설정했습니다.").queue();
    }
}

