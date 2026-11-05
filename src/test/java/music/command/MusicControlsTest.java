package music.command;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import music.service.RepeatMode;
import music.service.ServerMusicManager;
import net.dv8tion.jda.api.components.buttons.Button;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.*;

/**
 * MusicControls 테스트 클래스
 *
 * 테스트 대상:
 * - 슬래시 명령어와 버튼에서 같이 쓰는 일시정지, 반복, 섞기, 넘기기, 정지
 */
class MusicControlsTest {

    private ServerMusicManager music;

    @BeforeEach
    void setUp() {
        music = mock(ServerMusicManager.class);
    }

    @Test
    @DisplayName("재생 중인 곡이 없으면 일시정지하지 않고 누른 사람에게만 알린다")
    void shouldRejectPauseWhenNothingPlaying() {
        // when
        MusicControls.Reply reply = MusicControls.togglePause(music);

        // then
        assertTrue(reply.ephemeral());
        verify(music, never()).setPaused(anyBoolean());
    }

    @Test
    @DisplayName("재생 중이면 일시정지하고, 다시 누르면 이어서 재생한다")
    void shouldTogglePause() {
        // given
        when(music.isPlaying()).thenReturn(true);

        // when & then
        when(music.isPaused()).thenReturn(false);
        assertEquals("⏸ 일시정지했어요.", MusicControls.togglePause(music).message());
        verify(music).setPaused(true);

        when(music.isPaused()).thenReturn(true);
        assertEquals("▶️ 다시 재생합니다.", MusicControls.togglePause(music).message());
        verify(music).setPaused(false);
    }

    @Test
    @DisplayName("반복 모드는 끔 → 한 곡 → 전체 → 끔 순서로 바뀐다")
    void shouldCycleRepeatMode() {
        assertEquals(RepeatMode.TRACK, RepeatMode.OFF.next());
        assertEquals(RepeatMode.QUEUE, RepeatMode.TRACK.next());
        assertEquals(RepeatMode.OFF, RepeatMode.QUEUE.next());

        // given
        when(music.getRepeatMode()).thenReturn(RepeatMode.OFF);

        // when
        MusicControls.Reply reply = MusicControls.cycleRepeat(music);

        // then
        verify(music).setRepeatMode(RepeatMode.TRACK);
        assertEquals("🔁 반복 모드: **한 곡 반복**", reply.message());
    }

    @Test
    @DisplayName("대기열이 2곡 미만이면 섞지 않는다")
    void shouldRejectShuffleWithFewTracks() {
        // given
        when(music.getQueue()).thenReturn(List.of(mock(AudioTrack.class)));

        // when
        MusicControls.Reply reply = MusicControls.shuffle(music);

        // then
        assertTrue(reply.ephemeral());
        verify(music, never()).shuffle();
    }

    @Test
    @DisplayName("대기열이 2곡 이상이면 섞는다")
    void shouldShuffleQueue() {
        // given
        when(music.getQueue()).thenReturn(List.of(mock(AudioTrack.class), mock(AudioTrack.class)));

        // when
        MusicControls.Reply reply = MusicControls.shuffle(music);

        // then
        verify(music).shuffle();
        assertEquals("🔀 대기열 2곡을 섞었어요.", reply.message());
    }

    @Test
    @DisplayName("정지하면 대기열을 비우고 연결을 끊는다")
    void shouldStopAndDisconnect() {
        // given
        when(music.isConnected()).thenReturn(true);

        // when
        MusicControls.stop(music);

        // then
        verify(music).stop();
        verify(music).disconnect();
    }

    @Test
    @DisplayName("조작 버튼 5개가 버튼 핸들러가 처리하는 ID를 가진다")
    void shouldCreateButtonsWithKnownIds() {
        // when
        List<String> ids = MusicControls.buttons().getButtons().stream().map(Button::getCustomId).toList();

        // then
        assertEquals(List.of(MusicControls.PAUSE, MusicControls.SKIP, MusicControls.LOOP,
                MusicControls.SHUFFLE, MusicControls.STOP), ids);
    }
}
