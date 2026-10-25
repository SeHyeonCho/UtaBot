package entrysong.handler;

import net.dv8tion.jda.api.entities.Message;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * EntrySongUploadHandler 테스트 클래스
 *
 * 테스트 대상:
 * - findMp3(): 첨부 파일 중 입장곡으로 쓸 mp3 고르기
 */
class EntrySongUploadHandlerTest {

    private static Message.Attachment attachment(String fileName) {
        Message.Attachment attachment = mock(Message.Attachment.class);
        when(attachment.getFileName()).thenReturn(fileName);
        return attachment;
    }

    @Test
    @DisplayName("확장자가 대문자인 mp3도 찾는다")
    void shouldFindMp3IgnoringCase() {
        // given
        Message.Attachment upper = attachment("SONG.MP3");

        // when & then
        assertSame(upper, EntrySongUploadHandler.findMp3(List.of(upper)).orElseThrow());
    }

    @Test
    @DisplayName("mp3가 여러 개면 첫 번째 것만 쓴다")
    void shouldUseFirstMp3Only() {
        // given
        Message.Attachment image = attachment("cover.png");
        Message.Attachment first = attachment("first.mp3");
        Message.Attachment second = attachment("second.mp3");

        // when & then
        assertSame(first, EntrySongUploadHandler.findMp3(List.of(image, first, second)).orElseThrow());
    }

    @Test
    @DisplayName("mp3가 없으면 아무것도 고르지 않는다")
    void shouldFindNothingWithoutMp3() {
        assertTrue(EntrySongUploadHandler.findMp3(List.of(attachment("song.wav"))).isEmpty());
        assertTrue(EntrySongUploadHandler.findMp3(List.of()).isEmpty());
    }
}
