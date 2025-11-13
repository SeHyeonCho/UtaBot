package handler.music;

import com.sedmelluq.discord.lavaplayer.player.*;

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
}
