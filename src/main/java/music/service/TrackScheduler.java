package music.service;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import java.util.LinkedList;
import java.util.Queue;

/**
 * 음악 대기열을 관리하는 클래스 입니다.
 *
 * 유저가 /play 한 트랙에 대해
 * 아무것도 재생 중이지 않으면 바로 재생,
 * 이미 재생 중일시 큐에 쌓아두고 차례대로 곡을 재생합니다.
 */
public class TrackScheduler extends AudioEventAdapter {
    private final AudioPlayer player;
    private final Queue<AudioTrack> queue = new LinkedList<>();

    public TrackScheduler(AudioPlayer player) {
        this.player = player;
    }

    /**
     * 새로운 트랙을 넣는 메소드 입니다.
     *
     * 동시에 큐를 접근할 수 없게 synchronized 사용
     * 이미 재생 중이면 큐에 삽입, 재생 안하고 있을시 바로 재생
     *
     * @param track 재생 또는 큐에 추가할 오디오 트랙
     */
    public synchronized void queue(AudioTrack track) {
        if (!player.startTrack(track, true)) {
            queue.offer(track);
        }
    }

    /**
     * 다음 트랙으로 넘기는 메소드 입니다.
     *
     * queue.poll() 이 null 이면 재생 멈춤
     *
     */
    public synchronized void nextTrack() {
        player.startTrack(queue.poll(), false);
    }

    /**
     * 트랙 대기열 목록 반환하는 메소드 입니다.
     *
     * @return 내부 큐
     */
    public synchronized Queue<AudioTrack> getQueue() {
        return queue;
    }

    /**
     * 트랙이 종료될때 호출되는 메서드 입니다.
     *
     * endReason에 따라 대기열에서 다음 트랙을 꺼내 재생합니다.
     *
     * @param p         종료가 발생한 AudioPlayer
     * @param t         재생이 끝난 오디오 트랙
     * @param endReason 트랙이 종료된 이유
     */
    @Override
    public void onTrackEnd(AudioPlayer p, AudioTrack t, AudioTrackEndReason endReason) {
        if (endReason.mayStartNext) {
            nextTrack();
        }
    }
}


