package entrysong.handler;

import entrysong.model.EntrySongConfig;
import entrysong.repository.UploadChannelRepository;
import entrysong.service.EntrySongService;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * 서버별로 설정된 업로드 채널에서 .mp3 파일이 올라오면
 * 서버 로컬에 저장하고 그 서버에서 해당 유저의 입장곡으로 등록하는 핸들러.
 */
public class EntrySongUploadHandler extends ListenerAdapter {

    private final UploadChannelRepository uploadChannelRepository;
    private final EntrySongService entrySongService;

    public EntrySongUploadHandler(UploadChannelRepository uploadChannelRepository,
                                  EntrySongService entrySongService) {
        this.uploadChannelRepository = uploadChannelRepository;
        this.entrySongService = entrySongService;
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) return;
        if (!event.isFromGuild()) return;

        long guildId = event.getGuild().getIdLong();
        Long uploadChannelId = uploadChannelRepository.getUploadChannel(guildId);

        if (uploadChannelId == null) return;
        if (event.getChannel().getIdLong() != uploadChannelId) return;

        findMp3(event.getMessage().getAttachments())
                .ifPresent(a -> saveMp3File(event, a));
    }

    // 한 사람의 입장곡은 파일 하나라서, mp3가 여러 개 올라오면 첫 번째 것만 사용
    static Optional<Message.Attachment> findMp3(List<Message.Attachment> attachments) {
        return attachments.stream()
                .filter(a -> a.getFileName().toLowerCase(Locale.ROOT).endsWith(".mp3"))
                .findFirst();
    }

    private void saveMp3File(MessageReceivedEvent event,
                             Message.Attachment file) {
        long guildId = event.getGuild().getIdLong();
        long userId = event.getAuthor().getIdLong();

        try {
            Path savePath = entrySongService.prepareUploadPath(guildId, userId);

            file.getProxy().downloadToPath(savePath).whenComplete((v, err) -> {
                if (err != null) {
                    event.getChannel().sendMessage("❌ 파일 저장 실패: " + err.getMessage()).queue();
                    return;
                }

                EntrySongConfig config = entrySongService.registerUpload(guildId, userId);

                event.getChannel().sendMessage(
                        "🎵 `" + file.getFileName() + "` 를 입장곡으로 설정했어요! (" +
                                config.startSec + "초 ~ " + (config.startSec + config.durationSec) + "초)"
                ).queue();
            });

        } catch (Exception e) {
            event.getChannel().sendMessage("❌ 오류: " + e.getMessage()).queue();
        }
    }
}
