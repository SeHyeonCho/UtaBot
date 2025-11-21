package music.service;

import com.sedmelluq.discord.lavaplayer.player.*;
import com.sedmelluq.discord.lavaplayer.source.*;
import dev.lavalink.youtube.YoutubeAudioSourceManager;
import dev.lavalink.youtube.clients.Web;
import dev.lavalink.youtube.clients.Android;
import dev.lavalink.youtube.clients.WebEmbedded;
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


        // youtube-source 등록 (여러 클라이언트 사용으로 안정성 향상)
        YoutubeAudioSourceManager ytSourceManager = new YoutubeAudioSourceManager(
                new Web(),
                new Android(),
                new WebEmbedded()
        );
        playerManager.registerSourceManager(ytSourceManager);

        // 다른 원격 소스들을 개별적으로 등록 (deprecated YouTube source는 제외)
        // SoundCloud, Twitch, Bandcamp, Vimeo 등은 registerRemoteSources에서 자동 등록되지만
        // deprecated YouTube source는 이미 위에서 새로운 것으로 대체했으므로
        // registerRemoteSources를 사용하지 않고 필요한 소스만 개별 등록
        // 또는 registerRemoteSources를 사용하되 exclude 파라미터가 지원되는 버전인지 확인 필요
        
        // 현재는 registerRemoteSources를 사용하지 않고 로컬 소스만 등록
        // (원격 소스는 필요시 개별적으로 추가 가능)
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


