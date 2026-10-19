import common.command.CommandRegister;
import common.handler.DefaultCommandHandler;
import entrysong.handler.EntrySongHandler;
import entrysong.handler.EntrySongUploadHandler;
import entrysong.repository.EntrySongRepository;
import entrysong.repository.UploadChannelRepository;
import entrysong.service.EntrySongService;
import entrysong.service.LegacyEntrySongMigration;
import entrysong.service.LegacyEntrySongMigration.GuildMember;
import music.handler.MusicCommandHandler;
import music.handler.VoiceCommandHandler;
import io.github.cdimascio.dotenv.Dotenv;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.requests.GatewayIntent;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class Main {

    private static final Path ENTRY_SONG_STORE = Path.of("data", "server-entry-songs.json");
    private static final Path LEGACY_ENTRY_SONG_STORE = Path.of("data", "entry-songs.json");
    private static final Path UPLOAD_DIR = Path.of("uploads");

    public static void main(String[] args) throws InterruptedException {
        Dotenv dotenv = Dotenv.load();
        String token = dotenv.get("TOKEN");

        EntrySongRepository entrySongRepository = new EntrySongRepository(ENTRY_SONG_STORE);
        EntrySongService entrySongService = new EntrySongService(entrySongRepository, UPLOAD_DIR);
        UploadChannelRepository uploadChannelRepository = new UploadChannelRepository();

        JDA jda = JDABuilder.create(
                        token,
                        GatewayIntent.GUILD_MESSAGES,
                        GatewayIntent.GUILD_MEMBERS,
                        GatewayIntent.MESSAGE_CONTENT,
                        GatewayIntent.GUILD_VOICE_STATES
                )
                .addEventListeners(
                        new DefaultCommandHandler(),
                        new VoiceCommandHandler(),
                        new MusicCommandHandler(),
                        new EntrySongHandler(entrySongService),
                        new EntrySongUploadHandler(uploadChannelRepository, entrySongService)
                )
                .build();

        jda.updateCommands().addCommands().queue();
        CommandRegister.registerCommand(jda);

        jda.awaitReady();
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
