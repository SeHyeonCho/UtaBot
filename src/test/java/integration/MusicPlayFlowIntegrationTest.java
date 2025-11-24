package integration;

import io.github.cdimascio.dotenv.Dotenv;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import music.service.MusicManager;
import music.service.ServerMusicManager;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 통합 테스트: 음악 재생 전체 플로우
 * 
 * 테스트 대상:
 * - MusicManager와 실제 JDA 인스턴스를 사용한 통합 테스트
 * 
 * 주의: 실제 Discord 봇 토큰이 필요합니다.
 * .env 파일에 TOKEN이 설정되어 있어야 합니다.
 */
class MusicPlayFlowIntegrationTest {

    private JDA jda;
    private MusicManager musicManager;
    private String token;

    @BeforeEach
    void setUp() throws InterruptedException {
        // .env 파일에서 토큰 읽기
        Dotenv dotenv = Dotenv.load();
        token = dotenv.get("TOKEN");
        
        // 토큰이 없으면 테스트 실패
        assertNotNull(token, ".env 파일에 TOKEN이 설정되어 있지 않습니다.");
        assertFalse(token.isEmpty(), ".env 파일의 TOKEN이 비어있습니다.");

        musicManager = MusicManager.get();
        
        CountDownLatch readyLatch = new CountDownLatch(1);
        
        jda = JDABuilder.create(
                token,
                GatewayIntent.GUILD_MESSAGES,
                GatewayIntent.GUILD_MEMBERS,
                GatewayIntent.MESSAGE_CONTENT,
                GatewayIntent.GUILD_VOICE_STATES
        )
        .disableCache(CacheFlag.MEMBER_OVERRIDES, CacheFlag.VOICE_STATE)
        .addEventListeners(new ListenerAdapter() {
            @Override
            public void onReady(ReadyEvent event) {
                readyLatch.countDown();
            }
        })
        .build();

        // JDA가 준비될 때까지 대기 (최대 30초)
        assertTrue(readyLatch.await(30, TimeUnit.SECONDS), "JDA가 준비되지 않았습니다.");
    }

    @AfterEach
    void tearDown() {
        if (jda != null) {
            jda.shutdown();
            try {
                jda.awaitShutdown();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    @Test
    @DisplayName("여러 서버를 독립적으로 처리한다")
    void shouldHandleMultipleGuildsIndependently() {
        // given
        assertNotNull(token, ".env 파일에 TOKEN이 설정되어 있지 않습니다.");
        assertNotNull(jda, "JDA 인스턴스가 생성되지 않았습니다.");

        MusicManager musicManager = MusicManager.get();

        // when
        var guilds = jda.getGuilds();
        
        if (guilds.isEmpty()) {
            // 봇이 서버에 초대되지 않은 경우 테스트 스킵
            return;
        }

        // then
        // MusicManager가 싱글톤으로 동작하는지 확인
        assertNotNull(musicManager);
        assertSame(musicManager, MusicManager.get());
        
        // 각 서버에 대해 ServerMusicManager가 생성되는지 확인
        for (Guild guild : guilds) {
            ServerMusicManager serverMusic = musicManager.of(guild);
            assertNotNull(serverMusic);
            assertNotNull(serverMusic.player);
            assertNotNull(serverMusic.scheduler);
            assertNotNull(serverMusic.sendHandler);
        }
    }

    @Test
    @DisplayName("같은 서버에 대해 동일한 ServerMusicManager를 반환한다")
    void shouldReturnSameServerMusicManagerForSameGuild() {
        // given
        assertNotNull(token, ".env 파일에 TOKEN이 설정되어 있지 않습니다.");
        assertNotNull(jda, "JDA 인스턴스가 생성되지 않았습니다.");

        var guilds = jda.getGuilds();
        if (guilds.isEmpty()) {
            return;
        }

        Guild guild = guilds.get(0);
        MusicManager musicManager = MusicManager.get();

        // when
        ServerMusicManager serverMusic1 = musicManager.of(guild);
        ServerMusicManager serverMusic2 = musicManager.of(guild);

        // then
        assertSame(serverMusic1, serverMusic2);
    }
}
