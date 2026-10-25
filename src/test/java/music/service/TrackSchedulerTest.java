package music.service;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * TrackScheduler 테스트 클래스
 *
 * 테스트 대상:
 * - queue(): 트랙을 큐에 추가하거나 바로 재생
 * - skip(): 다음 트랙으로 넘기기
 * - stop(): 대기열 비우고 정지
 * - onTrackEnd(): 트랙 종료 시 자동으로 다음 트랙 재생
 * - playEntrySong(): 입장곡 끼워넣기와 원래 곡 복귀
 *
 * executor의 execute()는 바로 실행하고, schedule()로 예약된 작업은 모아뒀다가
 * 테스트에서 직접 실행해서 시간에 의존하지 않게 합니다.
 */
@ExtendWith(MockitoExtension.class)
class TrackSchedulerTest {

    private static final long ENTRY_DURATION_MS = 7_000L;

    @Mock
    private AudioPlayer player;

    @Mock
    private AudioTrack track1;

    @Mock
    private AudioTrack track2;

    @Mock
    private AudioTrack entryTrack;

    @Mock
    private AudioTrack otherEntryTrack;

    @Mock
    private AudioTrack originalTrack;

    @Mock
    private AudioTrack originalClone;

    @Mock
    private ScheduledExecutorService executor;

    @Mock
    private ScheduledFuture<Object> future;

    private final List<Runnable> scheduledTasks = new ArrayList<>();

    private TrackScheduler scheduler;

    @BeforeEach
    void setUp() {
        lenient().doAnswer(invocation -> {
            ((Runnable) invocation.getArgument(0)).run();
            return null;
        }).when(executor).execute(any(Runnable.class));
        lenient().doAnswer(invocation -> {
            scheduledTasks.add(invocation.getArgument(0));
            return future;
        }).when(executor).schedule(any(Runnable.class), anyLong(), any(TimeUnit.class));

        scheduler = new TrackScheduler(player, executor);
    }

    private void givenOriginalTrackPlayingAt(long positionMs) {
        when(player.getPlayingTrack()).thenReturn(originalTrack);
        when(originalTrack.getPosition()).thenReturn(positionMs);
        when(originalTrack.makeClone()).thenReturn(originalClone);
    }

    // ─────────────────────────────
    // 대기열
    // ─────────────────────────────

    @Test
    @DisplayName("플레이어가 재생 중이 아니면 트랙을 즉시 재생한다")
    void shouldStartTrackImmediatelyWhenPlayerIsNotPlaying() {
        // when
        scheduler.queue(track1);

        // then
        verify(player).startTrack(track1, false);
        assertTrue(scheduler.getQueue().isEmpty());
    }

    @Test
    @DisplayName("플레이어가 이미 재생 중이면 큐에 추가한다")
    void shouldAddToQueueWhenPlayerIsAlreadyPlaying() {
        // given
        when(player.getPlayingTrack()).thenReturn(originalTrack);

        // when
        scheduler.queue(track1);
        scheduler.queue(track2);

        // then
        verify(player, never()).startTrack(any(), anyBoolean());
        assertEquals(List.of(track1, track2), scheduler.getQueue());
    }

    @Test
    @DisplayName("다음 트랙으로 넘기면 큐 맨 앞 트랙을 재생한다")
    void shouldPlayNextTrackFromQueueOnSkip() {
        // given
        when(player.getPlayingTrack()).thenReturn(originalTrack);
        scheduler.queue(track1);
        scheduler.queue(track2);

        // when
        scheduler.skip();

        // then
        verify(player).startTrack(track1, false);
        assertEquals(List.of(track2), scheduler.getQueue());
    }

    @Test
    @DisplayName("큐가 비어있을 때 넘기면 재생을 중지한다")
    void shouldStopPlayingWhenQueueIsEmpty() {
        // when
        scheduler.skip();

        // then
        verify(player).startTrack(null, false);
        assertTrue(scheduler.getQueue().isEmpty());
    }

    @Test
    @DisplayName("정지하면 대기열을 비우고 재생을 멈춘다")
    void shouldClearQueueAndStopOnStop() {
        // given
        when(player.getPlayingTrack()).thenReturn(originalTrack);
        scheduler.queue(track1);

        // when
        scheduler.stop();

        // then
        verify(player).stopTrack();
        assertTrue(scheduler.getQueue().isEmpty());
    }

    @Test
    @DisplayName("트랙이 끝나면 자동으로 다음 트랙을 재생한다")
    void shouldPlayNextTrackAutomaticallyWhenTrackEnds() {
        // given
        when(player.getPlayingTrack()).thenReturn(originalTrack);
        scheduler.queue(track1);
        scheduler.queue(track2);
        when(player.getPlayingTrack()).thenReturn(null); // 곡이 끝나서 비어 있음

        // when
        scheduler.onTrackEnd(player, originalTrack, AudioTrackEndReason.FINISHED);

        // then
        verify(player).startTrack(track1, false);
        assertEquals(List.of(track2), scheduler.getQueue());
    }

    @Test
    @DisplayName("종료 이유가 다음 재생을 허용하지 않으면 다음 트랙을 재생하지 않는다")
    void shouldNotPlayNextTrackWhenEndReasonDoesNotAllowIt() {
        // given
        when(player.getPlayingTrack()).thenReturn(originalTrack);
        scheduler.queue(track1);

        // when
        scheduler.onTrackEnd(player, originalTrack, AudioTrackEndReason.STOPPED);

        // then
        verify(player, never()).startTrack(any(), anyBoolean());
        assertEquals(List.of(track1), scheduler.getQueue());
    }

    @Test
    @DisplayName("곡이 끝난 사이에 다른 곡이 이미 시작됐으면 다음 트랙으로 넘기지 않는다")
    void shouldNotSkipWhenAnotherTrackAlreadyStarted() {
        // given
        when(player.getPlayingTrack()).thenReturn(originalTrack);
        scheduler.queue(track1);

        // when: 종료 이벤트가 처리되기 전에 이미 다른 곡이 재생 중
        scheduler.onTrackEnd(player, track2, AudioTrackEndReason.FINISHED);

        // then
        verify(player, never()).startTrack(any(), anyBoolean());
        assertEquals(List.of(track1), scheduler.getQueue());
    }

    @Test
    @DisplayName("반환된 대기열은 복사본이라 수정할 수 없다")
    void shouldReturnUnmodifiableQueueSnapshot() {
        // given
        when(player.getPlayingTrack()).thenReturn(originalTrack);
        scheduler.queue(track1);

        // when
        List<AudioTrack> queue = scheduler.getQueue();

        // then
        assertThrows(UnsupportedOperationException.class, queue::clear);
        assertEquals(List.of(track1), scheduler.getQueue());
    }

    @Test
    @DisplayName("여러 스레드에서 동시에 추가해도 모든 트랙이 대기열에 들어간다")
    void shouldHandleConcurrentQueueOperations() throws Exception {
        // given: 실제 단일 스레드 executor 사용
        ScheduledExecutorService realExecutor = Executors.newSingleThreadScheduledExecutor();
        try {
            TrackScheduler realScheduler = new TrackScheduler(player, realExecutor);
            when(player.getPlayingTrack()).thenReturn(originalTrack);

            // when
            Thread t1 = new Thread(() -> {
                for (int i = 0; i < 10; i++) {
                    realScheduler.queue(track1);
                }
            });
            Thread t2 = new Thread(() -> {
                for (int i = 0; i < 10; i++) {
                    realScheduler.queue(track2);
                }
            });
            t1.start();
            t2.start();
            t1.join();
            t2.join();
            realExecutor.submit(() -> { }).get(5, TimeUnit.SECONDS); // 앞서 넘긴 작업이 끝날 때까지 대기

            // then
            assertEquals(20, realScheduler.getQueue().size());
        } finally {
            realExecutor.shutdownNow();
        }
    }

    @Test
    @DisplayName("재생 중 오류가 나면 곡을 요청한 채널에 이유를 알린다")
    void shouldNotifyRequestChannelOnTrackException() {
        // given
        MessageChannel channel = mock(MessageChannel.class);
        MessageCreateAction action = mock(MessageCreateAction.class);
        when(channel.sendMessage(any(CharSequence.class))).thenReturn(action);
        when(track1.getUserData()).thenReturn(channel);
        when(track1.getInfo()).thenReturn(new AudioTrackInfo("노래", "가수", 1000L, "id", false, "uri"));

        // when
        scheduler.onTrackException(player, track1,
                new FriendlyException("This video requires login.", FriendlyException.Severity.COMMON, null));

        // then
        verify(channel).sendMessage("⚠️ **노래** 재생 중 오류가 나서 건너뜁니다: This video requires login.");
        verify(action).queue();
    }

    @Test
    @DisplayName("알릴 채널이 없는 트랙(입장곡 등)의 오류도 문제없이 처리한다")
    void shouldHandleTrackExceptionWithoutChannel() {
        // given
        when(entryTrack.getInfo()).thenReturn(new AudioTrackInfo("입장곡", "가수", 1000L, "id", false, "uri"));

        // when & then
        assertDoesNotThrow(() -> scheduler.onTrackException(player, entryTrack,
                new FriendlyException("failed", FriendlyException.Severity.COMMON, null)));
    }

    // ─────────────────────────────
    // 입장곡
    // ─────────────────────────────

    @Test
    @DisplayName("입장곡이 끝나면 원래 곡을 멈췄던 위치부터 다시 재생한다")
    void shouldResumeOriginalTrackAfterEntrySong() {
        // given
        givenOriginalTrackPlayingAt(42_000L);

        // when
        scheduler.playEntrySong(entryTrack, ENTRY_DURATION_MS);

        // then: 입장곡 재생 + 종료 예약
        verify(player).startTrack(entryTrack, false);
        verify(executor).schedule(any(Runnable.class), eq(ENTRY_DURATION_MS), eq(TimeUnit.MILLISECONDS));
        verify(originalClone).setPosition(42_000L);

        // when: 재생 시간이 지남
        scheduledTasks.get(0).run();

        // then
        verify(player).startTrack(originalClone, false);
    }

    @Test
    @DisplayName("원래 곡이 없을 때 입장곡 중에 추가된 곡은 입장곡이 끝나면 재생된다")
    void shouldPlayTrackQueuedDuringEntrySong() {
        // given: 아무것도 재생 중이지 않은 상태에서 입장곡 시작
        scheduler.playEntrySong(entryTrack, ENTRY_DURATION_MS);
        when(player.getPlayingTrack()).thenReturn(entryTrack);
        scheduler.queue(track1); // 입장곡 재생 중이라 대기열로 감

        // when
        scheduledTasks.get(0).run();

        // then
        verify(player).startTrack(track1, false);
        assertTrue(scheduler.getQueue().isEmpty());
    }

    @Test
    @DisplayName("입장곡이 겹쳐도 처음 재생 중이던 원래 곡으로 돌아간다")
    void shouldResumeOriginalTrackWhenEntrySongsOverlap() {
        // given
        givenOriginalTrackPlayingAt(42_000L);
        scheduler.playEntrySong(entryTrack, ENTRY_DURATION_MS);

        // when: 첫 입장곡이 끝나기 전에 다른 사람이 들어옴
        scheduler.playEntrySong(otherEntryTrack, ENTRY_DURATION_MS);

        // then: 첫 입장곡의 종료 예약은 취소되고, 원래 곡은 한 번만 백업됨
        verify(future).cancel(false);
        verify(originalTrack, times(1)).makeClone();
        verify(player).startTrack(otherEntryTrack, false);

        // when: 첫 입장곡 예약이 늦게 실행되어도 무시되고, 두 번째 입장곡이 끝나면 원래 곡으로 복귀
        scheduledTasks.get(0).run();
        verify(player, never()).startTrack(originalClone, false);
        scheduledTasks.get(1).run();

        // then
        verify(player).startTrack(originalClone, false);
    }

    @Test
    @DisplayName("입장곡이 정해진 시간보다 먼저 끝나면 바로 원래 곡으로 돌아가고, 예약된 복귀는 무시된다")
    void shouldResumeImmediatelyWhenEntrySongEndsEarly() {
        // given
        givenOriginalTrackPlayingAt(42_000L);
        scheduler.playEntrySong(entryTrack, ENTRY_DURATION_MS);

        // when
        scheduler.onTrackEnd(player, entryTrack, AudioTrackEndReason.FINISHED);

        // then
        verify(player).startTrack(originalClone, false);

        // when: 예약된 복귀가 뒤늦게 실행됨
        scheduledTasks.get(0).run();

        // then: 원래 곡을 다시 시작하지 않음
        verify(player, times(1)).startTrack(originalClone, false);
    }

    @Test
    @DisplayName("입장곡 재생 중 넘기면 입장곡만 끝내고 원래 곡으로 돌아간다")
    void shouldResumeOriginalTrackWhenSkippingEntrySong() {
        // given
        givenOriginalTrackPlayingAt(42_000L);
        scheduler.queue(track1);
        scheduler.playEntrySong(entryTrack, ENTRY_DURATION_MS);

        // when
        scheduler.skip();

        // then
        verify(player).startTrack(originalClone, false);
        assertEquals(List.of(track1), scheduler.getQueue());
    }

    @Test
    @DisplayName("입장곡 재생 중 정지하면 원래 곡으로 돌아가지 않는다")
    void shouldNotResumeAfterStopDuringEntrySong() {
        // given
        givenOriginalTrackPlayingAt(42_000L);
        scheduler.playEntrySong(entryTrack, ENTRY_DURATION_MS);

        // when
        scheduler.stop();
        scheduledTasks.get(0).run();

        // then
        verify(player).stopTrack();
        verify(player, never()).startTrack(originalClone, false);
    }
}
