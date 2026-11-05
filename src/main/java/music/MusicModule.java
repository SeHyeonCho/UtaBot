package music;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import common.command.ButtonHandler;
import common.command.SlashCommand;
import common.config.BotConfig;
import common.module.BotModule;
import music.audio.AudioPlayerManagerFactory;
import music.command.JoinCommand;
import music.command.LeaveCommand;
import music.command.LoopCommand;
import music.command.NowPlayingCommand;
import music.command.PauseCommand;
import music.command.PlayCommand;
import music.command.QueueCommand;
import music.command.ShuffleCommand;
import music.command.SkipCommand;
import music.command.StopCommand;
import music.command.VolumeCommand;
import music.handler.MusicButtonHandler;
import music.service.MusicManager;
import net.dv8tion.jda.api.requests.GatewayIntent;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;

/**
 * 음악 재생 기능 (음성 채널 접속, 재생, 대기열, 반복, 조작 버튼)
 *
 * 다른 기능(입장곡 등)에서 음악 재생을 쓰려면 musicManager()를 넘겨받으면 됩니다.
 */
public class MusicModule implements BotModule {

    private final MusicManager musicManager;
    private final Path musicDir;

    public MusicModule(BotConfig config) {
        this(config, AudioPlayerManagerFactory.create());
    }

    public MusicModule(BotConfig config, AudioPlayerManager playerManager) {
        this.musicManager = new MusicManager(playerManager);
        this.musicDir = config.musicDir();
    }

    public MusicManager musicManager() {
        return musicManager;
    }

    @Override
    public List<SlashCommand> commands() {
        return List.of(
                new JoinCommand(musicManager),
                new LeaveCommand(musicManager),
                new PlayCommand(musicManager, musicDir),
                new SkipCommand(musicManager),
                new StopCommand(musicManager),
                new QueueCommand(musicManager),
                new VolumeCommand(musicManager),
                new NowPlayingCommand(musicManager),
                new PauseCommand(musicManager),
                new LoopCommand(musicManager),
                new ShuffleCommand(musicManager)
        );
    }

    @Override
    public List<ButtonHandler> buttonHandlers() {
        return List.of(new MusicButtonHandler(musicManager));
    }

    @Override
    public Set<GatewayIntent> intents() {
        // 명령어를 쓴 유저가 어느 음성 채널에 있는지 알아야 함
        return Set.of(GatewayIntent.GUILD_VOICE_STATES);
    }
}
