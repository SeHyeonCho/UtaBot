package music.handler;

import io.github.cdimascio.dotenv.Dotenv;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
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
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * MusicCommandHandler 통합 테스트 클래스
 * 
 * 테스트 대상:
 * - MusicCommandHandler 인스턴스 생성 및 기본 동작
 * - 실제 JDA 인스턴스를 사용한 통합 테스트
 * 
 * 주의: 실제 Discord 봇 토큰이 필요합니다.
 * .env 파일에 TOKEN이 설정되어 있어야 합니다.
 */
class MusicCommandHandlerTest {

    private JDA jda;
    private MusicCommandHandler handler;
    private String token;

    @BeforeEach
    void setUp() throws InterruptedException {
        // .env 파일에서 토큰 읽기
        Dotenv dotenv = Dotenv.load();
        token = dotenv.get("TOKEN");
        
        // 토큰이 없으면 테스트 실패
        assertNotNull(token, ".env 파일에 TOKEN이 설정되어 있지 않습니다.");
        assertFalse(token.isEmpty(), ".env 파일의 TOKEN이 비어있습니다.");

        handler = new MusicCommandHandler();
        
        CountDownLatch readyLatch = new CountDownLatch(1);
        
        jda = JDABuilder.create(
                token,
                GatewayIntent.GUILD_MESSAGES,
                GatewayIntent.GUILD_MEMBERS,
                GatewayIntent.MESSAGE_CONTENT,
                GatewayIntent.GUILD_VOICE_STATES
        )
        .disableCache(CacheFlag.MEMBER_OVERRIDES, CacheFlag.VOICE_STATE)
        .addEventListeners(handler)
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
    @DisplayName("MusicCommandHandler 인스턴스를 생성할 수 있다")
    void shouldCreateMusicCommandHandler() {
        // when
        MusicCommandHandler handler = new MusicCommandHandler();

        // then
        assertNotNull(handler);
    }

    @Test
    @DisplayName("MusicCommandHandler가 ListenerAdapter를 상속한다")
    void shouldExtendListenerAdapter() {
        // when
        MusicCommandHandler handler = new MusicCommandHandler();

        // then
        assertTrue(handler instanceof ListenerAdapter);
    }

    @Test
    @DisplayName("실제 JDA 인스턴스에 핸들러를 등록할 수 있다")
    void shouldRegisterHandlerWithJDA() {
        // given
        assertNotNull(token, ".env 파일에 TOKEN이 설정되어 있지 않습니다.");

        // when
        MusicCommandHandler handler = new MusicCommandHandler();
        JDA testJda = JDABuilder.create(
                token,
                GatewayIntent.GUILD_MESSAGES,
                GatewayIntent.GUILD_MEMBERS,
                GatewayIntent.MESSAGE_CONTENT,
                GatewayIntent.GUILD_VOICE_STATES
        )
        .disableCache(CacheFlag.MEMBER_OVERRIDES, CacheFlag.VOICE_STATE)
        .addEventListeners(handler)
        .build();

        // then
        assertNotNull(testJda);
        assertTrue(testJda.getEventManager().getRegisteredListeners().contains(handler));
        
        // cleanup
        testJda.shutdown();
    }

    @Test
    @DisplayName("JDA가 준비되면 서버 목록을 가져올 수 있다")
    void shouldGetGuildsWhenReady() {
        // given
        assertNotNull(token, ".env 파일에 TOKEN이 설정되어 있지 않습니다.");
        assertNotNull(jda, "JDA 인스턴스가 생성되지 않았습니다.");

        // when
        var guilds = jda.getGuilds();

        // then
        assertNotNull(guilds);
        // 봇이 서버에 초대되어 있으면 서버 목록이 있을 수 있음
    }

    @Test
    @DisplayName("MusicManager가 youtube-source를 사용하여 YouTube URL을 로드할 수 있다")
    void shouldLoadYouTubeUrlWithYoutubeSource() throws InterruptedException {
        // given
        assertNotNull(token, ".env 파일에 TOKEN이 설정되어 있지 않습니다.");
        assertNotNull(jda, "JDA 인스턴스가 생성되지 않았습니다.");
        
        var guilds = jda.getGuilds();
        if (guilds.isEmpty()) {
            // 봇이 서버에 초대되지 않은 경우 테스트 스킵
            return;
        }
        
        MusicManager musicManager = MusicManager.get();
        ServerMusicManager serverMusic = musicManager.of(guilds.get(0));
        String testUrl = "https://www.youtube.com/watch?v=dQw4w9WgXcQ";
        
        CountDownLatch loadLatch = new CountDownLatch(1);
        final boolean[] loadSuccess = {false};

        // when
        musicManager.playerManager().loadItemOrdered(
            serverMusic,
            testUrl,
            new AudioLoadResultHandler() {
                @Override
                public void trackLoaded(AudioTrack track) {
                    loadSuccess[0] = true;
                    loadLatch.countDown();
                }

                @Override
                public void playlistLoaded(AudioPlaylist playlist) {
                    loadSuccess[0] = true;
                    loadLatch.countDown();
                }

                @Override
                public void noMatches() {
                    loadLatch.countDown();
                }

                @Override
                public void loadFailed(FriendlyException exception) {
                    loadLatch.countDown();
                }
            }
        );

        // then
        boolean completed = loadLatch.await(30, TimeUnit.SECONDS);
        assertTrue(completed, "YouTube URL 로드가 30초 내에 완료되지 않았습니다.");
        assertTrue(loadSuccess[0], "YouTube URL 로드에 실패했습니다.");
    }
}
