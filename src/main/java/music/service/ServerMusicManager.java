package music.service;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import music.audio.AudioPlayerSendHandler;
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel;
import net.dv8tion.jda.api.managers.AudioManager;

/**
 * 서버 하나의 음악 재생과 음성 연결을 담당하는 클래스입니다.
 *
 * 서버당 하나씩 생성되며, 서로 다른 서버에서 독립적으로 음악을 재생할 수 있습니다.
 * 재생 제어(대기열, 입장곡)와 음성 채널 연결은 모두 이 클래스를 통해서만 합니다.
 */
public class ServerMusicManager {
    private final AudioPlayerManager playerManager;
    private final AudioManager audioManager;
    private final AudioPlayer player;
    private final TrackScheduler scheduler;
    private final AudioPlayerSendHandler sendHandler;

    public ServerMusicManager(AudioPlayerManager playerManager,
                              AudioManager audioManager,
                              ScheduledExecutorService executor) {
        this.playerManager = playerManager;
        this.audioManager = audioManager;
        this.player = playerManager.createPlayer();
        this.scheduler = new TrackScheduler(player, executor);
        this.player.addListener(scheduler);
        this.sendHandler = new AudioPlayerSendHandler(player);
    }

    // ─────────────────────────────
    // 음성 연결
    // ─────────────────────────────

    /**
     * 음성 채널에 접속합니다.
     *
     * 어떤 기능에서 접속하든 송출 핸들러와 자기 음소거 설정을 항상 함께 합니다.
     * 이미 다른 채널에 접속해 있으면 해당 채널로 이동합니다.
     */
    public void connect(AudioChannel channel) {
        audioManager.setSelfDeafened(true); // 에코 방지
        audioManager.setSendingHandler(sendHandler);
        audioManager.openAudioConnection(channel);
    }

    public void disconnect() {
        audioManager.closeAudioConnection();
    }

    public boolean isConnected() {
        return audioManager.isConnected();
    }

    /**
     * @return 지금 접속해 있는 음성 채널, 접속 중이 아니면 null
     */
    public AudioChannel getConnectedChannel() {
        return audioManager.getConnectedChannel();
    }

    // ─────────────────────────────
    // 재생
    // ─────────────────────────────

    /**
     * URL, 검색어, 파일 경로 등으로 트랙을 불러옵니다.
     * 같은 서버의 요청은 들어온 순서대로 처리됩니다.
     */
    public void load(String identifier, AudioLoadResultHandler handler) {
        playerManager.loadItemOrdered(this, identifier, handler);
    }

    public void queue(AudioTrack track) {
        scheduler.queue(track);
    }

    public void skip() {
        scheduler.skip();
    }

    /**
     * 대기열을 비우고 재생을 멈춥니다. 음성 연결은 그대로 둡니다.
     */
    public void stop() {
        scheduler.stop();
    }

    public void playEntrySong(AudioTrack track, long durationMs) {
        scheduler.playEntrySong(track, durationMs);
    }

    public List<AudioTrack> getQueue() {
        return scheduler.getQueue();
    }

    public boolean isPlaying() {
        return player.getPlayingTrack() != null;
    }

    public int getVolume() {
        return player.getVolume();
    }

    public void setVolume(int volume) {
        player.setVolume(volume);
    }
}
