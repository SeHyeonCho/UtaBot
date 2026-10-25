package music.command;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import common.command.SlashCommand;
import common.util.CommandUtils;
import music.service.MusicManager;
import music.service.ServerMusicManager;
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;

public class PlayCommand implements SlashCommand {

    private static final String OPTION_QUERY = "query";

    private final MusicManager musicManager;

    public PlayCommand(MusicManager musicManager) {
        this.musicManager = musicManager;
    }

    @Override
    public SlashCommandData data() {
        return Commands.slash("play", "노래 재생/큐에 추가")
                .addOption(OptionType.STRING, OPTION_QUERY, "URL 또는 검색어", true);
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        if (!CommandUtils.requireGuild(event)) {
            return;
        }

        AudioChannel channel = CommandUtils.requireVoiceChannel(event);
        if (channel == null) {
            return;
        }

        ServerMusicManager music = musicManager.of(event.getGuild());
        if (!music.isConnected()) {
            music.connect(channel);
        }

        String query = event.getOption(OPTION_QUERY).getAsString();
        event.deferReply().queue();

        // 모든 URL/검색어를 Lavaplayer에 직접 전달 (youtube-source가 처리)
        music.load(query, createAudioLoadResultHandler(music, event));
    }

    private AudioLoadResultHandler createAudioLoadResultHandler(ServerMusicManager music,
                                                               SlashCommandInteractionEvent event) {
        return new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack track) {
                track.setUserData(event.getChannel()); // 재생 중 오류를 알릴 채널
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
        for (AudioTrack track : playlist.getTracks()) {
            track.setUserData(event.getChannel());
            music.queue(track);
        }

        String playlistName = playlist.getName() != null ? playlist.getName() : "재생목록";
        event.getHook().sendMessage("▶️ 재생목록 추가: **" + playlistName + "** (" + playlist.getTracks().size() + "곡)").queue();
    }
}
