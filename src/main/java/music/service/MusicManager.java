package music.service;

import com.sedmelluq.discord.lavaplayer.player.*;
import com.sedmelluq.discord.lavaplayer.source.*;
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager;
import net.dv8tion.jda.api.entities.Guild;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 모든 ServerMusicManager 관리하는 클래스입니다.
 *
 */
public class MusicManager {
    private static final MusicManager INSTANCE = new MusicManager();
    private final AudioPlayerManager playerManager;
    private final Map<Long, ServerMusicManager> musicByGuild = new ConcurrentHashMap<>();

    private MusicManager() {
        this.playerManager = new DefaultAudioPlayerManager();

        AudioConfiguration conf = playerManager.getConfiguration();

        // LavaPlayer 음질 세팅
        conf.setResamplingQuality(AudioConfiguration.ResamplingQuality.HIGH);
        conf.setOpusEncodingQuality(10);
        conf.setFilterHotSwapEnabled(true);


        // LavaPlayer 유튜브 링크 처리 추가
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


