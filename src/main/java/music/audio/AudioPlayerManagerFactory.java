package music.audio;

import com.sedmelluq.discord.lavaplayer.player.AudioConfiguration;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import dev.lavalink.youtube.YoutubeAudioSourceManager;
import dev.lavalink.youtube.clients.Android;
import dev.lavalink.youtube.clients.Web;
import dev.lavalink.youtube.clients.WebEmbedded;

/**
 * 봇에서 사용할 AudioPlayerManager를 만드는 클래스입니다.
 *
 * 음질 설정과 음원 소스(youtube-source 등) 등록은 이 클래스에서만 합니다.
 */
public final class AudioPlayerManagerFactory {

    private AudioPlayerManagerFactory() {
    }

    public static AudioPlayerManager create() {
        AudioPlayerManager playerManager = new DefaultAudioPlayerManager();

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

        // 원격 소스(SoundCloud 등)는 등록하지 않고 로컬 파일 소스만 등록
        AudioSourceManagers.registerLocalSource(playerManager);

        return playerManager;
    }
}
