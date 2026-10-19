package music.command;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * QueueCommand 테스트 클래스
 *
 * 테스트 대상:
 * - format(): 대기열 표시 (최대 10곡)
 */
class QueueCommandTest {

    private static List<AudioTrack> tracks(int count) {
        List<AudioTrack> tracks = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            AudioTrack track = mock(AudioTrack.class);
            when(track.getInfo()).thenReturn(new AudioTrackInfo("곡" + i, "가수", 1000L, "id" + i, false, "uri"));
            tracks.add(track);
        }
        return tracks;
    }

    @Test
    @DisplayName("대기열을 번호와 함께 표시한다")
    void shouldFormatQueueWithNumbers() {
        // when
        String result = QueueCommand.format(tracks(2));

        // then
        assertEquals("**대기열**\n1. 곡1\n2. 곡2\n", result);
    }

    @Test
    @DisplayName("정확히 10곡이면 생략 표시를 붙이지 않는다")
    void shouldNotAppendEllipsisForExactlyTenTracks() {
        // when
        String result = QueueCommand.format(tracks(10));

        // then
        assertTrue(result.endsWith("10. 곡10\n"));
        assertFalse(result.contains("..."));
    }

    @Test
    @DisplayName("10곡을 넘으면 10곡까지만 보여주고 생략 표시를 붙인다")
    void shouldTruncateAfterTenTracks() {
        // when
        String result = QueueCommand.format(tracks(11));

        // then
        assertTrue(result.contains("10. 곡10\n"));
        assertFalse(result.contains("곡11"));
        assertTrue(result.endsWith("..."));
    }
}
