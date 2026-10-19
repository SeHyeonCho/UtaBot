package integration;

import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import io.github.cdimascio.dotenv.Dotenv;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import music.service.MusicManager;
import music.service.ServerMusicManager;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * 통합 테스트: 실제 Discord 서버 정보로 서버별 음악 매니저 확인
 *
 * 주의: 실제 Discord 봇 토큰이 필요합니다.
 * .env 파일에 TOKEN이 없으면 건너뜁니다. 실행: ./gradlew integrationTest
 */
@Tag("integration")
class MusicPlayFlowIntegrationTest {

    private static JDA jda;

    @BeforeAll
    static void setUp() throws InterruptedException {
        // .env 파일에서 토큰 읽기 (없으면 테스트 건너뜀)
        String token = Dotenv.configure().ignoreIfMissing().load().get("TOKEN");
        assumeTrue(token != null && !token.isBlank(), ".env 파일에 TOKEN이 없어 통합 테스트를 건너뜁니다.");

        jda = JDABuilder.create(
                token,
                GatewayIntent.GUILD_MESSAGES,
                GatewayIntent.GUILD_MEMBERS,
                GatewayIntent.MESSAGE_CONTENT,
                GatewayIntent.GUILD_VOICE_STATES
        )
        .disableCache(CacheFlag.MEMBER_OVERRIDES, CacheFlag.VOICE_STATE)
        .build();

        jda.awaitReady();
    }

    @AfterAll
    static void tearDown() throws InterruptedException {
        if (jda != null) {
            jda.shutdown();
            jda.awaitShutdown();
        }
    }

    private static List<Guild> requireGuilds() {
        List<Guild> guilds = jda.getGuilds();
        assumeFalse(guilds.isEmpty(), "봇이 들어가 있는 서버가 없어 건너뜁니다.");
        return guilds;
    }

    @Test
    @DisplayName("여러 서버를 독립적으로 처리한다")
    void shouldHandleMultipleGuildsIndependently() {
        // given
        List<Guild> guilds = requireGuilds();
        MusicManager musicManager = new MusicManager(new DefaultAudioPlayerManager());

        // when & then: 각 서버에 대해 ServerMusicManager가 생성되는지 확인
        for (Guild guild : guilds) {
            ServerMusicManager serverMusic = musicManager.of(guild);
            assertNotNull(serverMusic);
            assertTrue(serverMusic.getQueue().isEmpty());
            assertFalse(serverMusic.isPlaying());
        }
    }

    @Test
    @DisplayName("같은 서버에 대해 동일한 ServerMusicManager를 반환한다")
    void shouldReturnSameServerMusicManagerForSameGuild() {
        // given
        Guild guild = requireGuilds().get(0);
        MusicManager musicManager = new MusicManager(new DefaultAudioPlayerManager());

        // when
        ServerMusicManager serverMusic1 = musicManager.of(guild);
        ServerMusicManager serverMusic2 = musicManager.of(guild);

        // then
        assertSame(serverMusic1, serverMusic2);
    }
}
