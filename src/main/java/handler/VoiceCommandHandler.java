package handler;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.managers.AudioManager;
import util.Util;

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
        if (!Util.requireGuild(event)) {
            return;
        }

        Member member = event.getMember();
        if (member == null || member.getVoiceState() == null
                || !member.getVoiceState().inAudioChannel()) {
            event.reply("먼저 보이스 채널에 들어가 주세요!").setEphemeral(true).queue();
            return;
        }

        AudioChannel userChannel = member.getVoiceState().getChannel();
        AudioManager audioManager = event.getGuild().getAudioManager();
        audioManager.setSelfDeafened(true); // 에코 방지
        audioManager.openAudioConnection(userChannel);

        event.reply("🔊 `" + userChannel.getName() + "` 채널로 접속했어요!").queue();
    }

    private void handleLeave(SlashCommandInteractionEvent event) {
        if (!Util.requireGuild(event)) {
            return;
        }

        AudioManager audioManager = event.getGuild().getAudioManager();
        if (audioManager.isConnected()) {
            audioManager.closeAudioConnection();
            event.reply("보이스 채널에서 나왔어요.").queue();
        } else {
            event.reply("지금 어떤 보이스 채널에도 연결되어 있지 않아요.").setEphemeral(true).queue();
        }
    }
}