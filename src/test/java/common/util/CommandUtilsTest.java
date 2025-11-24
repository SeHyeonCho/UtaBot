package common.util;

import io.github.cdimascio.dotenv.Dotenv;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static java.lang.reflect.Modifier.isStatic;
import static org.junit.jupiter.api.Assertions.*;

/**
 * CommandUtils 통합 테스트 클래스
 * 
 * 테스트 대상:
 * - requireGuild(): 서버에서만 사용 가능한지 검증
 * 
 * 주의: 실제 Discord 봇 토큰이 필요합니다.
 * .env 파일에 TOKEN이 설정되어 있어야 합니다.
 */
class CommandUtilsTest {

    private JDA jda;
    private String token;

    @BeforeEach
    void setUp() throws InterruptedException {
        // .env 파일에서 토큰 읽기
        Dotenv dotenv = Dotenv.load();
        token = dotenv.get("TOKEN");
        
        // 토큰이 없으면 테스트 실패
        assertNotNull(token, ".env 파일에 TOKEN이 설정되어 있지 않습니다.");
        assertFalse(token.isEmpty(), ".env 파일의 TOKEN이 비어있습니다.");

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
    @DisplayName("requireGuild 메서드가 존재하고 올바른 시그니처를 가진다")
    void shouldHaveRequireGuildMethod() {
        // given & when
        Method method = assertDoesNotThrow(() -> {
            return CommandUtils.class.getMethod("requireGuild", 
                SlashCommandInteractionEvent.class);
        });
        
        // then
        assertNotNull(method);
        assertEquals(boolean.class, method.getReturnType());
        assertTrue(isStatic(method.getModifiers()));
    }

    @Test
    @DisplayName("JDA 인스턴스가 준비되면 서버 목록을 가져올 수 있다")
    void shouldGetGuildsWhenJDAIsReady() {
        // given
        assertNotNull(token, ".env 파일에 TOKEN이 설정되어 있지 않습니다.");
        assertNotNull(jda, "JDA 인스턴스가 생성되지 않았습니다.");

        // when
        var guilds = jda.getGuilds();

        // then
        assertNotNull(guilds);
        // 봇이 서버에 초대되어 있으면 서버 목록이 있을 수 있음
    }
}
