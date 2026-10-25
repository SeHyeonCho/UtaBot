package music.audio;

import com.sedmelluq.discord.lavaplayer.player.AudioConfiguration;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import dev.lavalink.youtube.YoutubeAudioSourceManager;
import dev.lavalink.youtube.clients.AndroidVr;
import dev.lavalink.youtube.clients.Music;
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

        // youtube-source 등록. 클라이언트는 youtube-source README의 권장 구성을 따름
        // (앞에서부터 차례로 시도, ANDROID는 "자주 동작하지 않음"으로 안내되어 제외)
        YoutubeAudioSourceManager ytSourceManager = new YoutubeAudioSourceManager(
                new Music(),
                new AndroidVr(),
                new Web(),
                new WebEmbedded()
        );
        playerManager.registerSourceManager(ytSourceManager);

        // 원격 소스(SoundCloud 등)는 등록하지 않고 로컬 파일 소스만 등록
        AudioSourceManagers.registerLocalSource(playerManager);

        return playerManager;
    }
}
