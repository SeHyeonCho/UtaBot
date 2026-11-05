import basic.BasicModule;
import common.config.BotConfig;
import common.module.BotModule;
import common.module.BotModules;
import entrysong.EntrySongModule;
import fun.FunModule;
import io.github.cdimascio.dotenv.Dotenv;
import moe.kyokobot.libdave.NativeDaveFactory;
import moe.kyokobot.libdave.jda.LDJDADaveSessionFactory;
import music.MusicModule;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.audio.AudioModuleConfig;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.utils.messages.MessageRequest;

import java.util.EnumSet;
import java.util.List;

/**
 * 봇 진입점입니다.
 *
 * 기능은 BotModule 단위로 나뉘어 있고, createModules()의 목록에 있는 기능만 봇에 들어갑니다.
 */
public class Main {

    public static void main(String[] args) throws InterruptedException {
        // .env 파일이 없으면 환경 변수에서 읽음
        BotConfig config = BotConfig.load(Dotenv.configure().ignoreIfMissing().load());

        // DAVE 네이티브 라이브러리를 이 OS에서 쓸 수 있는지 먼저 확인 (없으면 음성 연결이 전부 실패함)
        NativeDaveFactory.ensureAvailable();

        // /echo, /pick 등은 입력을 그대로 보여주므로 봇을 통한 @everyone, @here, 역할 멘션은 막음
        MessageRequest.setDefaultMentions(EnumSet.complementOf(EnumSet.of(
                Message.MentionType.EVERYONE, Message.MentionType.HERE, Message.MentionType.ROLE)));

        BotModules modules = new BotModules(createModules(config));

        JDA jda = JDABuilder.create(config.token(), modules.intents())
                // 2026년 3월부터 디스코드 음성은 종단간 암호화(DAVE)를 지원해야만 연결됨
                .setAudioModuleConfig(new AudioModuleConfig()
                        .withDaveSessionFactory(new LDJDADaveSessionFactory(new NativeDaveFactory())))
                .addEventListeners(modules.eventListeners().toArray())
                .build();

        jda.awaitReady();
        System.out.println("로그인 성공 ~!" + jda.getSelfUser().getAsTag());

        modules.onReady(jda);
    }

    /**
     * 봇에 들어가는 기능 목록입니다. 새 기능은 BotModule을 만들어 여기에 추가하면 됩니다.
     */
    static List<BotModule> createModules(BotConfig config) {
        MusicModule music = new MusicModule(config);

        return List.of(
                new BasicModule(),
                music,
                new EntrySongModule(config, music.musicManager()),
                new FunModule()
        );
    }
}
