package upload.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * UploadChannelRegistry 테스트 클래스
 * 
 * 테스트 대상:
 * - setUploadChannel(): 서버별 업로드 채널 설정
 * - getUploadChannel(): 서버별 업로드 채널 조회
 * - removeUploadChannel(): 서버별 업로드 채널 제거
 */
class UploadChannelRegistryTest {

    @BeforeEach
    void setUp() {
        // 각 테스트 전에 레지스트리 초기화
        // static 필드이므로 직접 초기화할 수 없지만, 테스트 간 격리를 위해 제거
        long testGuildId = 123456789L;
        if (UploadChannelRegistry.getUploadChannel(testGuildId) != null) {
            UploadChannelRegistry.removeUploadChannel(testGuildId);
        }
    }

    @Test
    @DisplayName("업로드 채널을 설정하고 조회한다")
    void shouldSetAndGetUploadChannel() {
        // given
        long guildId = 123456789L;
        long channelId = 987654321L;

        // when
        UploadChannelRegistry.setUploadChannel(guildId, channelId);
        Long result = UploadChannelRegistry.getUploadChannel(guildId);

        // then
        assertNotNull(result);
        assertEquals(channelId, result);
    }

    @Test
    @DisplayName("채널이 설정되지 않았을 때 null을 반환한다")
    void shouldReturnNullWhenChannelNotSet() {
        // given
        long guildId = 999999999L;

        // when
        Long result = UploadChannelRegistry.getUploadChannel(guildId);

        // then
        assertNull(result);
    }

    @Test
    @DisplayName("기존 채널을 업데이트한다")
    void shouldUpdateExistingChannel() {
        // given
        long guildId = 123456789L;
        long channelId1 = 111111111L;
        long channelId2 = 222222222L;

        // when
        UploadChannelRegistry.setUploadChannel(guildId, channelId1);
        UploadChannelRegistry.setUploadChannel(guildId, channelId2);
        Long result = UploadChannelRegistry.getUploadChannel(guildId);

        // then
        assertEquals(channelId2, result);
    }

    @Test
    @DisplayName("업로드 채널을 제거한다")
    void shouldRemoveUploadChannel() {
        // given
        long guildId = 123456789L;
        long channelId = 987654321L;
        UploadChannelRegistry.setUploadChannel(guildId, channelId);

        // when
        UploadChannelRegistry.removeUploadChannel(guildId);
        Long result = UploadChannelRegistry.getUploadChannel(guildId);

        // then
        assertNull(result);
    }

    @Test
    @DisplayName("여러 서버를 처리한다")
    void shouldHandleMultipleGuilds() {
        // given
        long guildId1 = 111111111L;
        long guildId2 = 222222222L;
        long channelId1 = 999999999L;
        long channelId2 = 888888888L;

        // when
        UploadChannelRegistry.setUploadChannel(guildId1, channelId1);
        UploadChannelRegistry.setUploadChannel(guildId2, channelId2);

        // then
        assertEquals(channelId1, UploadChannelRegistry.getUploadChannel(guildId1));
        assertEquals(channelId2, UploadChannelRegistry.getUploadChannel(guildId2));
    }

    @Test
    @DisplayName("존재하지 않는 채널을 안전하게 제거한다")
    void shouldRemoveNonExistentChannelSafely() {
        // given
        long guildId = 999999999L;

        // when & then
        assertDoesNotThrow(() -> UploadChannelRegistry.removeUploadChannel(guildId));
        assertNull(UploadChannelRegistry.getUploadChannel(guildId));
    }
}

