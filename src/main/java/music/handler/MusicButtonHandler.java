package music.handler;

import music.command.MusicControls;
import music.service.MusicManager;
import music.service.ServerMusicManager;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

/**
 * /nowplaying 메시지의 조작 버튼(⏯ ⏭ 🔁 🔀 ⏹)을 처리하는 핸들러입니다.
 */
public class MusicButtonHandler extends ListenerAdapter {

    private final MusicManager musicManager;

    public MusicButtonHandler(MusicManager musicManager) {
        this.musicManager = musicManager;
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        String id = event.getComponentId();
        if (!id.startsWith(MusicControls.BUTTON_PREFIX) || event.getGuild() == null) {
            return;
        }

        ServerMusicManager music = musicManager.of(event.getGuild());
        MusicControls.Reply reply = switch (id) {
            case MusicControls.PAUSE -> MusicControls.togglePause(music);
            case MusicControls.SKIP -> MusicControls.skip(music);
            case MusicControls.LOOP -> MusicControls.cycleRepeat(music);
            case MusicControls.SHUFFLE -> MusicControls.shuffle(music);
            case MusicControls.STOP -> MusicControls.stop(music);
            default -> null;
        };
        if (reply == null) {
            return;
        }

        // 다른 사람도 볼 수 있게 누가 눌렀는지 함께 표시 (실패 메시지는 누른 사람에게만)
        String message = reply.ephemeral() ? reply.message() : reply.message() + " (" + nameOf(event) + ")";
        event.reply(message).setEphemeral(reply.ephemeral()).queue();
    }

    private static String nameOf(ButtonInteractionEvent event) {
        Member member = event.getMember();
        return member != null ? member.getEffectiveName() : event.getUser().getName();
    }
}
