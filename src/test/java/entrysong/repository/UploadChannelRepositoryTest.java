package entrysong.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * UploadChannelRepository 테스트 클래스
 *
 * 테스트 대상:
 * - setUploadChannel(): 서버별 업로드 채널 설정
 * - getUploadChannel(): 서버별 업로드 채널 조회
 * - removeUploadChannel(): 서버별 업로드 채널 제거
 */
class UploadChannelRepositoryTest {

    private UploadChannelRepository repository;

    @BeforeEach
    void setUp() {
        repository = new UploadChannelRepository();
    }

    @Test
    @DisplayName("업로드 채널을 설정하고 조회한다")
    void shouldSetAndGetUploadChannel() {
        // given
        long guildId = 123456789L;
        long channelId = 987654321L;

        // when
        repository.setUploadChannel(guildId, channelId);
        Long result = repository.getUploadChannel(guildId);

        // then
        assertNotNull(result);
        assertEquals(channelId, result);
    }

    @Test
    @DisplayName("채널이 설정되지 않았을 때 null을 반환한다")
    void shouldReturnNullWhenChannelNotSet() {
        // when
        Long result = repository.getUploadChannel(999999999L);

        // then
        assertNull(result);
    }

    @Test
    @DisplayName("기존 채널을 업데이트한다")
    void shouldUpdateExistingChannel() {
        // given
        long guildId = 123456789L;

        // when
        repository.setUploadChannel(guildId, 111111111L);
        repository.setUploadChannel(guildId, 222222222L);

        // then
        assertEquals(222222222L, repository.getUploadChannel(guildId));
    }

    @Test
    @DisplayName("업로드 채널을 제거한다")
    void shouldRemoveUploadChannel() {
        // given
        long guildId = 123456789L;
        repository.setUploadChannel(guildId, 987654321L);

        // when
        repository.removeUploadChannel(guildId);

        // then
        assertNull(repository.getUploadChannel(guildId));
    }

    @Test
    @DisplayName("여러 서버를 처리한다")
    void shouldHandleMultipleGuilds() {
        // when
        repository.setUploadChannel(111111111L, 999999999L);
        repository.setUploadChannel(222222222L, 888888888L);

        // then
        assertEquals(999999999L, repository.getUploadChannel(111111111L));
        assertEquals(888888888L, repository.getUploadChannel(222222222L));
    }

    @Test
    @DisplayName("존재하지 않는 채널을 안전하게 제거한다")
    void shouldRemoveNonExistentChannelSafely() {
        // when & then
        assertDoesNotThrow(() -> repository.removeUploadChannel(999999999L));
        assertNull(repository.getUploadChannel(999999999L));
    }
}
