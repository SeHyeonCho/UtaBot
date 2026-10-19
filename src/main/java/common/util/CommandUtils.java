package common.util;

import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

public class CommandUtils {

    private CommandUtils() {
    }

    public static boolean requireGuild(SlashCommandInteractionEvent event) {
        if (!event.isFromGuild()) {
            event.reply("서버에서만 사용할 수 있어요.")
                    .setEphemeral(true)
                    .queue();
            return false;
        }
        return true;
    }

    /**
     * 명령어를 쓴 유저가 들어가 있는 음성 채널을 돌려줍니다.
     * 음성 채널에 없으면 안내 메시지를 보내고 null을 돌려줍니다.
     */
    public static AudioChannel requireVoiceChannel(SlashCommandInteractionEvent event) {
        Member member = event.getMember();
        GuildVoiceState voiceState = member == null ? null : member.getVoiceState();
        if (voiceState == null || !voiceState.inAudioChannel()) {
            event.reply("먼저 보이스 채널에 들어가 주세요!").setEphemeral(true).queue();
            return null;
        }
        return voiceState.getChannel();
    }
}

