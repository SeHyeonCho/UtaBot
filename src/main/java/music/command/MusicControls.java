package music.command;

import music.service.RepeatMode;
import music.service.ServerMusicManager;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;

/**
 * 일시정지, 반복, 섞기처럼 슬래시 명령어와 /nowplaying 버튼에서 같이 쓰는 음악 조작입니다.
 */
public final class MusicControls {

    public static final String BUTTON_PREFIX = "music:";
    public static final String PAUSE = BUTTON_PREFIX + "pause";
    public static final String SKIP = BUTTON_PREFIX + "skip";
    public static final String LOOP = BUTTON_PREFIX + "loop";
    public static final String SHUFFLE = BUTTON_PREFIX + "shuffle";
    public static final String STOP = BUTTON_PREFIX + "stop";

    private static final String NOTHING_PLAYING = "지금 재생 중인 곡이 없어요.";

    private MusicControls() {
    }

    /**
     * 조작 결과 메시지. 실패(재생 중인 곡이 없음 등)는 누른 사람에게만 보여줍니다.
     */
    public record Reply(String message, boolean ephemeral) {

        static Reply done(String message) {
            return new Reply(message, false);
        }

        static Reply rejected(String message) {
            return new Reply(message, true);
        }
    }

    public static ActionRow buttons() {
        return ActionRow.of(
                Button.secondary(PAUSE, "⏯ 일시정지"),
                Button.secondary(SKIP, "⏭ 다음 곡"),
                Button.secondary(LOOP, "🔁 반복"),
                Button.secondary(SHUFFLE, "🔀 섞기"),
                Button.danger(STOP, "⏹ 정지")
        );
    }

    public static Reply togglePause(ServerMusicManager music) {
        if (!music.isPlaying()) {
            return Reply.rejected(NOTHING_PLAYING);
        }

        boolean paused = !music.isPaused();
        music.setPaused(paused);
        return Reply.done(paused ? "⏸ 일시정지했어요." : "▶️ 다시 재생합니다.");
    }

    public static Reply skip(ServerMusicManager music) {
        music.skip();
        return Reply.done("⏭ 다음 트랙으로 넘어갑니다.");
    }

    public static Reply stop(ServerMusicManager music) {
        music.stop();
        if (music.isConnected()) {
            music.disconnect();
        }
        return Reply.done("⏹ 정지하고 연결을 종료했어요.");
    }

    public static Reply setRepeat(ServerMusicManager music, RepeatMode mode) {
        music.setRepeatMode(mode);
        return Reply.done("🔁 반복 모드: **" + mode.label() + "**");
    }

    public static Reply cycleRepeat(ServerMusicManager music) {
        return setRepeat(music, music.getRepeatMode().next());
    }

    public static Reply shuffle(ServerMusicManager music) {
        int size = music.getQueue().size();
        if (size < 2) {
            return Reply.rejected("섞으려면 대기열에 2곡 이상 있어야 해요.");
        }

        music.shuffle();
        return Reply.done("🔀 대기열 " + size + "곡을 섞었어요.");
    }
}
