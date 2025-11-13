package handler.music;

import com.sedmelluq.discord.lavaplayer.player.*;
import com.sedmelluq.discord.lavaplayer.source.*;
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager;
import net.dv8tion.jda.api.entities.Guild;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MusicManager {
    private static final MusicManager INSTANCE = new MusicManager();
    private final AudioPlayerManager playerManager;
    private final Map<Long, ServerMusicManager> musicByGuild = new ConcurrentHashMap<>();

    private MusicManager() {
        this.playerManager = new DefaultAudioPlayerManager();

        AudioConfiguration conf = playerManager.getConfiguration();
        conf.setResamplingQuality(AudioConfiguration.ResamplingQuality.HIGH);
        conf.setOpusEncodingQuality(10);
        conf.setFilterHotSwapEnabled(true);

        YoutubeAudioSourceManager yt = new YoutubeAudioSourceManager();
        yt.setPlaylistPageCount(2);
        playerManager.registerSourceManager(yt);

        AudioSourceManagers.registerRemoteSources(playerManager);
        AudioSourceManagers.registerLocalSource(playerManager);
    }

    public static MusicManager get() {
        return INSTANCE;
    }
    public ServerMusicManager of(Guild guild) {
        return musicByGuild.computeIfAbsent(
                guild.getIdLong(),
                id -> new ServerMusicManager(playerManager)
        );
    }

    public AudioPlayerManager playerManager() {
        return playerManager;
    }
}