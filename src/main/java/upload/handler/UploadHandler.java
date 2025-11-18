package upload.handler;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.entities.User;
import entrysong.repository.EntrySongRegistry;
import upload.repository.UploadChannelRegistry;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 길드별로 설정된 업로드 채널에서 .mp3 파일이 올라오면
 * 서버 로컬에 저장하고 해당 유저의 입장곡으로 등록하는 핸들러.
 * (username#0000 기반 저장)
 */
public class UploadHandler extends ListenerAdapter {

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        switch (event.getName()) {
            case "setuploadchannel" -> {
                if (!event.isFromGuild()) {
                    event.reply("❌ 서버 안에서만 사용할 수 있는 명령어입니다.")
                            .setEphemeral(true).queue();
                    return;
                }

                var option = event.getOption("channel");
                var channel = option.getAsChannel();

                if (!(channel instanceof net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel messageChannel)) {
                    event.reply("❌ 텍스트 채널만 업로드 채널로 설정할 수 있어요!")
                            .setEphemeral(true).queue();
                    return;
                }

                long guildId = event.getGuild().getIdLong();
                UploadChannelRegistry.setUploadChannel(guildId, messageChannel.getIdLong());

                event.reply("📥 이 서버의 업로드 채널이 <#" + messageChannel.getIdLong() + "> 로 설정되었습니다!")
                        .queue();
            }
            default -> {}
        }
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) return;
        if (!event.isFromGuild()) return;

        long guildId = event.getGuild().getIdLong();
        Long uploadChannelId = UploadChannelRegistry.getUploadChannel(guildId);

        if (uploadChannelId == null) return;
        if (event.getChannel().getIdLong() != uploadChannelId) return;

        var attachments = event.getMessage().getAttachments();
        if (attachments.isEmpty()) return;

        attachments.stream()
                .filter(a -> a.getFileName().endsWith(".mp3"))
                .forEach(a -> saveMp3File(event, a));
    }

    private void saveMp3File(MessageReceivedEvent event,
                             net.dv8tion.jda.api.entities.Message.Attachment file) {
        try {
            Path baseDir = Path.of("uploads");
            Files.createDirectories(baseDir);

            User user = event.getAuthor();

            String username = sanitize(user.getName());
            String discriminator = sanitize(user.getDiscriminator());

            // "username#0000.mp3"
            String fileName = username + "#" + discriminator + ".mp3";

            Path savePath = baseDir.resolve(fileName);

            file.getProxy().downloadToPath(savePath).whenComplete((v, err) -> {
                if (err != null) {
                    event.getChannel().sendMessage("❌ 파일 저장 실패: " + err.getMessage()).queue();
                    return;
                }

                // 기본 재생 구간을 설정: 0~10초
                EntrySongRegistry.setSong(username, discriminator, fileName, 0, 10);

                event.getChannel().sendMessage(
                        "🎵 `" + fileName + "` 를 입장곡으로 설정했어요! (0초 ~ 10초)"
                ).queue();
            });

        } catch (Exception e) {
            event.getChannel().sendMessage("❌ 오류: " + e.getMessage()).queue();
        }
    }

    private String sanitize(String s) {
        // 파일명에 사용할 수 없는 문자 제거
        return s.replaceAll("[^a-zA-Z0-9._-가-힣]", "_");
    }
}