import common.config.BotConfig;
import common.module.BotModules;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Main 테스트 클래스
 *
 * 실제로 봇에 들어가는 기능 목록(createModules)이 문제없이 조립되는지 확인합니다.
 * 새 기능을 추가했을 때 명령어 이름이 겹치거나 디스코드 규칙(이름 형식, 설명 길이 등)을
 * 어기면 여기서 실패합니다.
 */
class MainTest {

    @TempDir
    Path tempDir;

    private BotModules createModules() {
        BotConfig config = new BotConfig("token", tempDir.resolve("data"), tempDir.resolve("uploads"),
                tempDir.resolve("music"));
        return new BotModules(Main.createModules(config));
    }

    @Test
    @DisplayName("모든 기능의 명령어가 겹치지 않고 등록된다")
    void shouldAssembleAllModules() {
        // when
        Set<String> names = Set.copyOf(createModules().dispatcher().getCommandData().stream()
                .map(data -> data.getName()).toList());

        // then
        assertTrue(names.containsAll(Set.of(
                "ping", "echo",
                "join", "leave", "play", "skip", "stop", "queue", "volume", "nowplaying", "pause", "loop", "shuffle",
                "setentrytime", "setuploadchannel", "setentrysong",
                "dice", "pick", "ladder", "conch")));
    }

    @Test
    @DisplayName("기능들이 필요로 하는 Gateway Intent만 요청한다")
    void shouldRequestIntentsOfModules() {
        assertEquals(Set.of(
                GatewayIntent.GUILD_VOICE_STATES,
                GatewayIntent.GUILD_MESSAGES,
                GatewayIntent.MESSAGE_CONTENT,
                GatewayIntent.GUILD_MEMBERS), createModules().intents());
    }
}
