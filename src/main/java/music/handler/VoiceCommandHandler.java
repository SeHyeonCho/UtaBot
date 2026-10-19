package music.handler;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import common.util.CommandUtils;
import music.service.MusicManager;
import music.service.ServerMusicManager;

public class VoiceCommandHandler extends ListenerAdapter {

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        switch (event.getName()) {
            case "join" -> handleJoin(event);
            case "leave" -> handleLeave(event);
            default -> {} // 다른 핸들러가 처리
        }
    }

    private void handleJoin(SlashCommandInteractionEvent event) {
        if (!CommandUtils.requireGuild(event)) {
            return;
        }

        Member member = event.getMember();
        if (member == null || member.getVoiceState() == null
                || !member.getVoiceState().inAudioChannel()) {
            event.reply("먼저 보이스 채널에 들어가 주세요!").setEphemeral(true).queue();
            return;
        }

        AudioChannel userChannel = member.getVoiceState().getChannel();
        MusicManager.get().of(event.getGuild()).connect(userChannel);

        event.reply("🔊 `" + userChannel.getName() + "` 채널로 접속했어요!").queue();
    }

    private void handleLeave(SlashCommandInteractionEvent event) {
        if (!CommandUtils.requireGuild(event)) {
            return;
        }

        ServerMusicManager music = MusicManager.get().of(event.getGuild());
        if (music.isConnected()) {
            music.disconnect();
            event.reply("보이스 채널에서 나왔어요.").queue();
        } else {
            event.reply("지금 어떤 보이스 채널에도 연결되어 있지 않아요.").setEphemeral(true).queue();
        }
    }
}

