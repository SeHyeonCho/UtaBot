package handler.music;

import com.sedmelluq.discord.lavaplayer.player.*;
import net.dv8tion.jda.api.audio.AudioSendHandler;

/**
 * 서버의 음악 재생에 필요한 구성 요소를 모아놓은 클래스입니다.
 *
 * 서버당 하나씩 생성되며, 서로 다른 서버에서 독립적으로 음악을 재생할 수 있습니다.
 */
public class ServerMusicManager {
    public final AudioPlayer player;
    public final TrackScheduler scheduler;
    public final AudioPlayerHandler sendHandler;

    public ServerMusicManager(AudioPlayerManager manager) {
        this.player = manager.createPlayer();
        this.scheduler = new TrackScheduler(player);
        this.player.addListener(scheduler);
        this.sendHandler = new AudioPlayerHandler(player);
    }

    public AudioSendHandler getSendHandler() {
        return sendHandler;
    }
}
