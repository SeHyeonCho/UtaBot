package entrysong.command;

import entrysong.repository.UploadChannelRepository;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SetUploadChannelCommand 테스트 클래스
 */
class SetUploadChannelCommandTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("서버 관리 권한이 있는 사람만 쓸 수 있게 등록된다")
    void shouldRequireManageServerPermission() {
        // given
        SetUploadChannelCommand command =
                new SetUploadChannelCommand(new UploadChannelRepository(tempDir.resolve("upload-channels.json")));

        // when & then
        assertEquals(DefaultMemberPermissions.enabledFor(Permission.MANAGE_SERVER),
                command.data().getDefaultPermissions());
    }
}
