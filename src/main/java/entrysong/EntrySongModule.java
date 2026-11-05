package entrysong;

import common.command.SlashCommand;
import common.config.BotConfig;
import common.module.BotModule;
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
import music.service.MusicManager;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.hooks.EventListener;
import net.dv8tion.jda.api.requests.GatewayIntent;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 입장곡 기능 (입장곡 설정, mp3 업로드, 음성 채널 입장 시 재생, 예전 데이터 이전)
 */
public class EntrySongModule implements BotModule {

    private final EntrySongRepository entrySongRepository;
    private final EntrySongService entrySongService;
    private final UploadChannelRepository uploadChannelRepository;
    private final MusicManager musicManager;
    private final Path legacyStorePath;
    private final Path uploadDir;

    /**
     * @param musicManager 입장곡 재생에 쓸 MusicModule의 음악 매니저
     */
    public EntrySongModule(BotConfig config, MusicManager musicManager) {
        this.entrySongRepository = new EntrySongRepository(config.dataFile("server-entry-songs.json"));
        this.entrySongService = new EntrySongService(entrySongRepository, config.uploadDir());
        this.uploadChannelRepository = new UploadChannelRepository(config.dataFile("upload-channels.json"));
        this.musicManager = musicManager;
        this.legacyStorePath = config.dataFile("entry-songs.json");
        this.uploadDir = config.uploadDir();
    }

    @Override
    public List<SlashCommand> commands() {
        return List.of(
                new SetEntryTimeCommand(entrySongService),
                new SetUploadChannelCommand(uploadChannelRepository),
                new SetEntrySongCommand(entrySongService)
        );
    }

    @Override
    public List<EventListener> listeners() {
        return List.of(
                new EntrySongJoinHandler(entrySongService, musicManager),
                new EntrySongUploadHandler(uploadChannelRepository, entrySongService)
        );
    }

    @Override
    public Set<GatewayIntent> intents() {
        return Set.of(
                GatewayIntent.GUILD_VOICE_STATES,  // 음성 채널 입장 감지
                GatewayIntent.GUILD_MESSAGES,      // 업로드 채널 메시지
                GatewayIntent.MESSAGE_CONTENT,     // 메시지의 첨부 파일
                GatewayIntent.GUILD_MEMBERS        // 예전 데이터 이전 시 멤버 목록
        );
    }

    @Override
    public void onReady(JDA jda) {
        migrateLegacyEntrySongs(jda);
    }

    // 예전 입장곡 데이터(username#discriminator 기준)가 남아 있으면 서버별·userId 기준으로 옮김
    private void migrateLegacyEntrySongs(JDA jda) {
        LegacyEntrySongMigration migration = new LegacyEntrySongMigration(
                entrySongService, entrySongRepository, legacyStorePath, uploadDir);

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
