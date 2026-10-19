package music.service;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import net.dv8tion.jda.api.entities.Guild;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * 모든 ServerMusicManager 관리하는 클래스입니다.
 *
 */
public class MusicManager {
    private final AudioPlayerManager playerManager;
    // 모든 서버의 대기열/입장곡 상태 변경을 처리하는 스레드
    private final ScheduledExecutorService schedulerExecutor;
    private final Map<Long, ServerMusicManager> musicByGuild = new ConcurrentHashMap<>();

    public MusicManager(AudioPlayerManager playerManager) {
        this.playerManager = playerManager;
        this.schedulerExecutor = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "track-scheduler");
            thread.setDaemon(true);
            return thread;
        });
    }

    public ServerMusicManager of(Guild guild) {
        return musicByGuild.computeIfAbsent(
                guild.getIdLong(),
                id -> new ServerMusicManager(playerManager, guild.getAudioManager(), schedulerExecutor)
        );
    }
}
