package music.command;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import common.command.SlashCommand;
import common.util.CommandUtils;
import common.util.UrlUtils;
import music.service.MusicManager;
import music.service.ServerMusicManager;
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;

import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.List;

public class PlayCommand implements SlashCommand {

    private static final String OPTION_QUERY = "query";
    private static final String YOUTUBE_SEARCH_PREFIX = "ytsearch:";

    private final MusicManager musicManager;
    private final Path musicDir;

    /**
     * @param musicDir 서버에 직접 넣은 음악 파일 폴더. 이 폴더 안의 파일은 /play 파일이름 으로 재생할 수 있음
     */
    public PlayCommand(MusicManager musicManager, Path musicDir) {
        this.musicManager = musicManager;
        this.musicDir = musicDir.toAbsolutePath().normalize();
    }

    @Override
    public SlashCommandData data() {
        return Commands.slash("play", "노래 재생/큐에 추가")
                .addOption(OptionType.STRING, OPTION_QUERY, "유튜브 URL, 검색어 또는 music 폴더의 파일 이름", true);
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

        String query = event.getOption(OPTION_QUERY).getAsString().trim();
        event.deferReply().queue();

        music.load(toIdentifier(query), createAudioLoadResultHandler(music, event, query));
    }

    /**
     * 입력을 Lavaplayer에 넘길 값으로 바꿉니다.
     *
     * URL은 그대로, music 폴더 안의 파일 이름은 파일 경로로, 나머지는 유튜브 검색어로 처리합니다.
     */
    String toIdentifier(String query) {
        if (UrlUtils.isHttpUrl(query)) {
            return query;
        }

        Path file = findMusicFile(query);
        if (file != null) {
            return file.toString();
        }

        return YOUTUBE_SEARCH_PREFIX + query;
    }

    // "노래.mp3" 와 "music/노래.mp3" 둘 다 허용하고, music 폴더 밖의 파일(../ 등)은 재생하지 않음
    private Path findMusicFile(String name) {
        try {
            for (Path candidate : List.of(musicDir.resolve(name), Path.of(name).toAbsolutePath())) {
                Path file = candidate.normalize();
                if (file.startsWith(musicDir) && Files.isRegularFile(file)) {
                    return file;
                }
            }
        } catch (InvalidPathException e) {
            // 파일 경로에 쓸 수 없는 문자가 있으면 검색어로 처리
        }
        return null;
    }

    private AudioLoadResultHandler createAudioLoadResultHandler(ServerMusicManager music,
                                                               SlashCommandInteractionEvent event,
                                                               String query) {
        return new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack track) {
                queueTrack(track, music, event);
            }

            @Override
            public void playlistLoaded(AudioPlaylist playlist) {
                handlePlaylistLoaded(playlist, music, event);
            }

            @Override
            public void noMatches() {
                String message = UrlUtils.isHttpUrl(query)
                        ? "스트림 URL을 찾지 못했어요."
                        : "🔍 `" + query + "` 검색 결과가 없어요.";
                event.getHook().sendMessage(message).queue();
            }

            @Override
            public void loadFailed(FriendlyException e) {
                e.printStackTrace();
                event.getHook().sendMessage("로드 실패: " + e.getMessage()).queue();
            }
        };
    }

    private void queueTrack(AudioTrack track, ServerMusicManager music, SlashCommandInteractionEvent event) {
        track.setUserData(event.getChannel()); // 재생 중 오류를 알릴 채널
        music.queue(track);
        event.getHook().sendMessage("▶️ 재생/추가: **" + track.getInfo().title + "**").queue();
    }

    private void handlePlaylistLoaded(AudioPlaylist playlist, ServerMusicManager music,
                                      SlashCommandInteractionEvent event) {
        if (playlist.getTracks().isEmpty()) {
            event.getHook().sendMessage("플레이리스트를 불러왔지만 트랙이 없어요.").queue();
            return;
        }

        // 검색 결과는 재생목록 형태로 오므로 첫 번째 곡만 재생
        if (playlist.isSearchResult()) {
            queueTrack(playlist.getTracks().get(0), music, event);
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
