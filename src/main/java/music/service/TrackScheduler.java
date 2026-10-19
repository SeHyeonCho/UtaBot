package music.service;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * 음악 대기열과 입장곡 끼워넣기를 관리하는 클래스 입니다.
 *
 * 대기열에 들어온 곡은 순서대로 재생하고,
 * 입장곡이 들어오면 현재 곡을 잠시 멈췄다가 입장곡이 끝나면 원래 위치부터 이어서 재생합니다.
 *
 * 상태 변경은 모두 executor 스레드 하나에서만 합니다.
 * Lavaplayer는 트랙 종료 이벤트를 내부 락을 잡은 채로 호출하기 때문에,
 * 여기서 synchronized로 락을 잡고 player를 호출하면 교착 상태가 생길 수 있습니다.
 */
public class TrackScheduler extends AudioEventAdapter {
    private final AudioPlayer player;
    private final ScheduledExecutorService executor;
    private final Queue<AudioTrack> queue = new LinkedList<>();
    private volatile List<AudioTrack> queueSnapshot = List.of();

    private AudioTrack entryTrack;        // 지금 끼어들어 재생 중인 입장곡
    private AudioTrack resumeTrack;       // 입장곡이 끝나면 이어서 재생할 원래 곡
    private ScheduledFuture<?> entryEndTask;

    public TrackScheduler(AudioPlayer player, ScheduledExecutorService executor) {
        this.player = player;
        this.executor = executor;
    }

    /**
     * 트랙을 대기열 끝에 추가하는 메소드 입니다.
     *
     * 재생 중인 곡이 없으면 대기열 맨 앞 곡을 바로 재생합니다.
     *
     * @param track 재생 또는 큐에 추가할 오디오 트랙
     */
    public void queue(AudioTrack track) {
        run(() -> {
            queue.offer(track);
            if (player.getPlayingTrack() == null) {
                playNextFromQueue();
            }
            publishQueue();
        });
    }

    /**
     * 현재 곡을 건너뛰는 메소드 입니다.
     *
     * 입장곡 재생 중이면 입장곡만 끝내고 원래 곡으로 돌아갑니다.
     */
    public void skip() {
        run(() -> {
            if (entryTrack != null) {
                finishEntrySong(entryTrack);
            } else {
                playNextFromQueue();
                publishQueue();
            }
        });
    }

    /**
     * 대기열을 비우고 재생을 멈추는 메소드 입니다.
     */
    public void stop() {
        run(() -> {
            queue.clear();
            clearEntrySong();
            player.stopTrack();
            publishQueue();
        });
    }

    /**
     * 현재 곡을 잠시 멈추고 입장곡을 재생하는 메소드 입니다.
     *
     * durationMs가 지나면 원래 곡을 멈췄던 위치부터 다시 재생합니다.
     * 입장곡 재생 중에 다른 입장곡이 들어오면 입장곡만 교체하고, 돌아갈 원래 곡은 그대로 둡니다.
     *
     * @param track      입장곡 트랙 (시작 위치는 미리 설정되어 있어야 함)
     * @param durationMs 입장곡 재생 길이
     */
    public void playEntrySong(AudioTrack track, long durationMs) {
        run(() -> {
            if (entryTrack == null) {
                resumeTrack = cloneAtCurrentPosition(player.getPlayingTrack());
            } else {
                entryEndTask.cancel(false);
            }
            entryTrack = track;
            player.startTrack(track, false);
            entryEndTask = executor.schedule(() -> run(() -> finishEntrySong(track)),
                    durationMs, TimeUnit.MILLISECONDS);
        });
    }

    /**
     * 트랙 대기열 목록 반환하는 메소드 입니다.
     *
     * @return 대기열의 복사본 (수정 불가)
     */
    public List<AudioTrack> getQueue() {
        return queueSnapshot;
    }

    /**
     * 트랙이 종료될때 호출되는 메서드 입니다.
     *
     * Lavaplayer 내부 락을 잡은 상태로 호출되므로 처리는 executor로 넘기고 바로 반환합니다.
     *
     * @param p         종료가 발생한 AudioPlayer
     * @param track     재생이 끝난 오디오 트랙
     * @param endReason 트랙이 종료된 이유
     */
    @Override
    public void onTrackEnd(AudioPlayer p, AudioTrack track, AudioTrackEndReason endReason) {
        run(() -> handleTrackEnd(track, endReason));
    }

    private void handleTrackEnd(AudioTrack track, AudioTrackEndReason endReason) {
        if (track == entryTrack) {
            // 입장곡이 정해진 시간보다 먼저 끝났거나 재생에 실패한 경우
            if (endReason.mayStartNext) {
                finishEntrySong(track);
            }
            return;
        }

        // 그 사이에 다른 곡이 이미 시작됐으면(입장곡 복귀, 새로 추가된 곡 등) 건드리지 않음
        if (endReason.mayStartNext && player.getPlayingTrack() == null) {
            playNextFromQueue();
            publishQueue();
        }
    }

    private void finishEntrySong(AudioTrack track) {
        if (track != entryTrack) {
            return; // 이미 끝났거나 다른 입장곡으로 바뀐 경우
        }

        AudioTrack resume = resumeTrack;
        clearEntrySong();

        if (resume != null) {
            player.startTrack(resume, false);
        } else {
            playNextFromQueue();
        }
        publishQueue();
    }

    private void clearEntrySong() {
        if (entryEndTask != null) {
            entryEndTask.cancel(false);
        }
        entryEndTask = null;
        entryTrack = null;
        resumeTrack = null;
    }

    // 대기열이 비어 있으면 startTrack(null)이 되어 재생이 멈춤
    private void playNextFromQueue() {
        player.startTrack(queue.poll(), false);
    }

    private void publishQueue() {
        queueSnapshot = List.copyOf(queue);
    }

    private AudioTrack cloneAtCurrentPosition(AudioTrack track) {
        if (track == null) {
            return null;
        }
        AudioTrack clone = track.makeClone();
        clone.setPosition(track.getPosition());
        return clone;
    }

    private void run(Runnable task) {
        executor.execute(() -> {
            try {
                task.run();
            } catch (Exception e) {
                System.out.println("[TrackScheduler] task failed:");
                e.printStackTrace();
            }
        });
    }
}
