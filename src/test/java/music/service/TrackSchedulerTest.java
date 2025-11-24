package music.service;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Queue;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * TrackScheduler 테스트 클래스
 * 
 * 테스트 대상:
 * - queue(): 트랙을 큐에 추가하거나 바로 재생
 * - nextTrack(): 다음 트랙으로 넘기기
 * - onTrackEnd(): 트랙 종료 시 자동으로 다음 트랙 재생
 * - getQueue(): 대기열 조회
 */
@ExtendWith(MockitoExtension.class)
class TrackSchedulerTest {

    @Mock
    private AudioPlayer player;

    @Mock
    private AudioTrack track1;

    @Mock
    private AudioTrack track2;

    @Mock
    private AudioTrack track3;

    private TrackScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new TrackScheduler(player);
    }

    @Test
    @DisplayName("플레이어가 재생 중이 아니면 트랙을 즉시 재생한다")
    void shouldStartTrackImmediatelyWhenPlayerIsNotPlaying() {
        // given
        when(player.startTrack(any(AudioTrack.class), eq(true))).thenReturn(true);

        // when
        scheduler.queue(track1);

        // then
        verify(player, times(1)).startTrack(track1, true);
        assertTrue(scheduler.getQueue().isEmpty());
    }

    @Test
    @DisplayName("플레이어가 이미 재생 중이면 큐에 추가한다")
    void shouldAddToQueueWhenPlayerIsAlreadyPlaying() {
        // given
        when(player.startTrack(any(AudioTrack.class), eq(true))).thenReturn(false);

        // when
        scheduler.queue(track1);

        // then
        verify(player, times(1)).startTrack(track1, true);
        assertEquals(1, scheduler.getQueue().size());
        assertTrue(scheduler.getQueue().contains(track1));
    }

    @Test
    @DisplayName("플레이어가 재생 중일 때 여러 트랙을 큐에 추가한다")
    void shouldQueueMultipleTracksWhenPlayerIsPlaying() {
        // given
        when(player.startTrack(any(AudioTrack.class), eq(true))).thenReturn(false);

        // when
        scheduler.queue(track1);
        scheduler.queue(track2);
        scheduler.queue(track3);

        // then
        Queue<AudioTrack> queue = scheduler.getQueue();
        assertEquals(3, queue.size());
        assertEquals(track1, queue.poll());
        assertEquals(track2, queue.poll());
        assertEquals(track3, queue.poll());
    }

    @Test
    @DisplayName("큐에서 다음 트랙을 재생한다")
    void shouldPlayNextTrackFromQueue() {
        // given
        lenient().when(player.startTrack(any(AudioTrack.class), eq(true))).thenReturn(false);
        scheduler.queue(track1);
        scheduler.queue(track2);
        
        when(player.startTrack(any(AudioTrack.class), eq(false))).thenReturn(true);

        // when
        scheduler.nextTrack();

        // then
        verify(player, times(1)).startTrack(track1, false);
        assertEquals(1, scheduler.getQueue().size());
        assertTrue(scheduler.getQueue().contains(track2));
    }

    @Test
    @DisplayName("큐가 비어있으면 재생을 중지한다")
    void shouldStopPlayingWhenQueueIsEmpty() {
        // given
        // 큐가 비어있는 상태

        // when
        scheduler.nextTrack();

        // then
        verify(player, times(1)).startTrack(null, false);
        assertTrue(scheduler.getQueue().isEmpty());
    }

    @Test
    @DisplayName("트랙이 종료되면 자동으로 다음 트랙을 재생한다")
    void shouldPlayNextTrackAutomaticallyWhenTrackEnds() {
        // given
        lenient().when(player.startTrack(any(AudioTrack.class), eq(true))).thenReturn(false);
        scheduler.queue(track1);
        scheduler.queue(track2);
        
        when(player.startTrack(any(AudioTrack.class), eq(false))).thenReturn(true);
        AudioTrackEndReason endReason = AudioTrackEndReason.FINISHED;

        // when
        scheduler.onTrackEnd(player, track1, endReason);

        // then
        verify(player, times(1)).startTrack(track1, false);
        assertEquals(1, scheduler.getQueue().size());
    }

    @Test
    @DisplayName("종료 이유가 다음 재생을 허용하지 않으면 다음 트랙을 재생하지 않는다")
    void shouldNotPlayNextTrackWhenEndReasonDoesNotAllowIt() {
        // given
        when(player.startTrack(any(AudioTrack.class), eq(true))).thenReturn(false);
        scheduler.queue(track1);
        scheduler.queue(track2);
        
        AudioTrackEndReason endReason = AudioTrackEndReason.STOPPED; // mayStartNext = false

        // when
        scheduler.onTrackEnd(player, track1, endReason);

        // then
        verify(player, never()).startTrack(any(), eq(false));
        assertEquals(2, scheduler.getQueue().size()); // 큐에 그대로 남아있음
    }

    @Test
    @DisplayName("큐를 안전하게 반환한다")
    void shouldReturnQueueSafely() {
        // given
        when(player.startTrack(any(AudioTrack.class), eq(true))).thenReturn(false);
        scheduler.queue(track1);
        scheduler.queue(track2);

        // when
        Queue<AudioTrack> queue = scheduler.getQueue();

        // then
        assertNotNull(queue);
        assertEquals(2, queue.size());
        // 원본 큐를 반환하므로 수정 가능해야 함
        queue.clear();
        assertTrue(scheduler.getQueue().isEmpty());
    }

    @Test
    @DisplayName("동시에 큐 작업을 처리한다")
    void shouldHandleConcurrentQueueOperations() throws InterruptedException {
        // given
        when(player.startTrack(any(AudioTrack.class), eq(true))).thenReturn(false);
        
        // when
        Thread t1 = new Thread(() -> {
            for (int i = 0; i < 10; i++) {
                scheduler.queue(track1);
            }
        });
        Thread t2 = new Thread(() -> {
            for (int i = 0; i < 10; i++) {
                scheduler.queue(track2);
            }
        });
        
        t1.start();
        t2.start();
        t1.join();
        t2.join();

        // then
        assertEquals(20, scheduler.getQueue().size());
    }
}

