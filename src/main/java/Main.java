import common.command.CommandDispatcher;
import common.command.basic.EchoCommand;
import common.command.basic.PingCommand;
import entrysong.command.SetEntrySongCommand;
import entrysong.command.SetEntryTimeCommand;
import entrysong.command.SetUploadChannelCommand;
import entrysong.handler.EntrySongJoinHandler;
import entrysong.handler.EntrySongUploadHandler;
import entrysong.repository.EntrySongRepository;
import entrysong.repository.UploadChannelRepository;
import entrysong.service.EntrySongService;
import entrysong.service.LegacyEntrySongMigration;
import entrysong.service.LegacyEntrySongMigration.GuildMember;
import music.audio.AudioPlayerManagerFactory;
import music.command.JoinCommand;
import music.command.LeaveCommand;
import music.command.PlayCommand;
import music.command.QueueCommand;
import music.command.SkipCommand;
import music.command.StopCommand;
import music.command.VolumeCommand;
import music.service.MusicManager;
import io.github.cdimascio.dotenv.Dotenv;
import moe.kyokobot.libdave.NativeDaveFactory;
import moe.kyokobot.libdave.jda.LDJDADaveSessionFactory;
import net.dv8tion.jda.api.audio.AudioModuleConfig;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.requests.GatewayIntent;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 봇 진입점입니다.
 *
 * 봇에서 쓰는 객체는 모두 여기서 만들고 필요한 곳에 생성자로 넘겨줍니다.
 */
public class Main {

    private static final Path ENTRY_SONG_STORE = Path.of("data", "server-entry-songs.json");
    private static final Path LEGACY_ENTRY_SONG_STORE = Path.of("data", "entry-songs.json");
    private static final Path UPLOAD_DIR = Path.of("uploads");

    public static void main(String[] args) throws InterruptedException {
        Dotenv dotenv = Dotenv.load();
        String token = dotenv.get("TOKEN");

        // DAVE 네이티브 라이브러리를 이 OS에서 쓸 수 있는지 먼저 확인 (없으면 음성 연결이 전부 실패함)
        NativeDaveFactory.ensureAvailable();

        MusicManager musicManager = new MusicManager(AudioPlayerManagerFactory.create());
        EntrySongRepository entrySongRepository = new EntrySongRepository(ENTRY_SONG_STORE);
        EntrySongService entrySongService = new EntrySongService(entrySongRepository, UPLOAD_DIR);
        UploadChannelRepository uploadChannelRepository = new UploadChannelRepository();

        CommandDispatcher commandDispatcher = new CommandDispatcher(List.of(
                // 기본
                new PingCommand(),
                new EchoCommand(),
                // 보이스 채널
                new JoinCommand(musicManager),
                new LeaveCommand(musicManager),
                // 음악
                new PlayCommand(musicManager),
                new SkipCommand(musicManager),
                new StopCommand(musicManager),
                new QueueCommand(musicManager),
                new VolumeCommand(musicManager),
                // 입장곡
                new SetEntryTimeCommand(entrySongService),
                new SetUploadChannelCommand(uploadChannelRepository),
                new SetEntrySongCommand(entrySongService)
        ));

        JDA jda = JDABuilder.create(
                        token,
                        GatewayIntent.GUILD_MESSAGES,
                        GatewayIntent.GUILD_MEMBERS,
                        GatewayIntent.MESSAGE_CONTENT,
                        GatewayIntent.GUILD_VOICE_STATES
                )
                // 2026년 3월부터 디스코드 음성은 종단간 암호화(DAVE)를 지원해야만 연결됨
                .setAudioModuleConfig(new AudioModuleConfig()
                        .withDaveSessionFactory(new LDJDADaveSessionFactory(new NativeDaveFactory())))
                .addEventListeners(
                        commandDispatcher,
                        new EntrySongJoinHandler(entrySongService, musicManager),
                        new EntrySongUploadHandler(uploadChannelRepository, entrySongService)
                )
                .build();

        jda.awaitReady();
        System.out.println("로그인 성공 ~!" + jda.getSelfUser().getAsTag());

        migrateLegacyEntrySongs(jda, entrySongService, entrySongRepository);
    }

    // 예전 입장곡 데이터(username#discriminator 기준)가 남아 있으면 서버별·userId 기준으로 옮김
    private static void migrateLegacyEntrySongs(JDA jda,
                                                EntrySongService entrySongService,
                                                EntrySongRepository entrySongRepository) {
        LegacyEntrySongMigration migration = new LegacyEntrySongMigration(
                entrySongService, entrySongRepository, LEGACY_ENTRY_SONG_STORE, UPLOAD_DIR);

        try {
            if (!migration.hasLegacyData()) {
                return;
            }

            List<GuildMember> members = new ArrayList<>();
            for (Guild guild : jda.getGuilds()) {
                for (Member member : guild.getMembers()) {
                    if (member.getUser().isBot()) {
                        continue;
                    }
                    members.add(new GuildMember(guild.getIdLong(), member.getIdLong(),
                            member.getUser().getName(), member.getUser().getDiscriminator()));
                }
            }
            migration.migrate(members);
        } catch (IOException e) {
            System.out.println("[EntrySongMigration] failed:");
            e.printStackTrace();
        }
    }
}
