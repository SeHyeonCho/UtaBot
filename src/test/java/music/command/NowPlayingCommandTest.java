package music.command;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import music.service.RepeatMode;
import music.service.ServerMusicManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * NowPlayingCommand 테스트 클래스
 *
 * 테스트 대상:
 * - format(): 지금 재생 중인 곡 정보
 * - progressBar(), formatTime(): 진행 바와 시간 표시
 */
class NowPlayingCommandTest {

    private static ServerMusicManager musicPlaying(AudioTrackInfo info, long position, long duration) {
        AudioTrack track = mock(AudioTrack.class);
        when(track.getInfo()).thenReturn(info);
        when(track.getPosition()).thenReturn(position);
        when(track.getDuration()).thenReturn(duration);

        ServerMusicManager music = mock(ServerMusicManager.class);
        when(music.getPlayingTrack()).thenReturn(track);
        when(music.getRepeatMode()).thenReturn(RepeatMode.OFF);
        when(music.getQueue()).thenReturn(List.of());
        return music;
    }

    @Test
    @DisplayName("시간을 분:초, 1시간이 넘으면 시:분:초로 표시한다")
    void shouldFormatTime() {
        assertEquals("0:00", NowPlayingCommand.formatTime(0));
        assertEquals("1:05", NowPlayingCommand.formatTime(65_000));
        assertEquals("1:02:05", NowPlayingCommand.formatTime(3_725_000));
    }

    @Test
    @DisplayName("진행 바의 동그라미가 재생 위치에 맞게 움직인다")
    void shouldDrawProgressBar() {
        assertEquals("🔘" + "▬".repeat(14), NowPlayingCommand.progressBar(0, 100_000));
        assertEquals("▬".repeat(7) + "🔘" + "▬".repeat(7), NowPlayingCommand.progressBar(50_000, 100_000));
        assertEquals("▬".repeat(14) + "🔘", NowPlayingCommand.progressBar(100_000, 100_000));
        assertEquals("🔘" + "▬".repeat(14), NowPlayingCommand.progressBar(0, 0));
    }

    @Test
    @DisplayName("곡 제목, 진행 상황, 반복 모드와 대기열 수를 보여준다")
    void shouldFormatNowPlaying() {
        // given
        ServerMusicManager music = musicPlaying(
                new AudioTrackInfo("노래", "가수", 200_000L, "id", false, "uri"), 65_000, 200_000);
        when(music.getRepeatMode()).thenReturn(RepeatMode.TRACK);
        when(music.getQueue()).thenReturn(List.of(mock(AudioTrack.class), mock(AudioTrack.class)));

        // when
        String text = NowPlayingCommand.format(music);

        // then
        assertTrue(text.startsWith("🎶 **지금 재생 중**\n**노래** - 가수\n"));
        assertTrue(text.contains("`1:05 / 3:20`"));
        assertTrue(text.endsWith("🔁 한 곡 반복 · 대기열 2곡"));
    }

    @Test
    @DisplayName("일시정지 중이면 일시정지라고 표시한다")
    void shouldShowPaused() {
        // given
        ServerMusicManager music = musicPlaying(
                new AudioTrackInfo("노래", "가수", 200_000L, "id", false, "uri"), 0, 200_000);
        when(music.isPaused()).thenReturn(true);

        // when & then
        assertTrue(NowPlayingCommand.format(music).startsWith("⏸ **일시정지 중**"));
    }

    @Test
    @DisplayName("라이브 방송은 진행 바 대신 LIVE로 표시한다")
    void shouldShowLiveForStreams() {
        // given
        ServerMusicManager music = musicPlaying(
                new AudioTrackInfo("라이브", "채널", Long.MAX_VALUE, "id", true, "uri"), 10_000, Long.MAX_VALUE);

        // when & then
        assertTrue(NowPlayingCommand.format(music).contains("🔴 LIVE"));
    }

    @Test
    @DisplayName("재생 중인 곡이 없으면 null을 돌려준다")
    void shouldReturnNullWhenNothingPlaying() {
        assertNull(NowPlayingCommand.format(mock(ServerMusicManager.class)));
    }
}
