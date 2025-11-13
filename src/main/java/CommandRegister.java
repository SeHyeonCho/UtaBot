import static net.dv8tion.jda.api.interactions.commands.OptionType.INTEGER;
import static net.dv8tion.jda.api.interactions.commands.OptionType.STRING;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.SubscribeEvent;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

public class CommandRegister {

    public CommandRegister() {
    }

    public static void registerCommand(JDA jda) {
        jda.addEventListener(new Object() {
            @SubscribeEvent
            public void onReady(ReadyEvent event) {
                jda.getGuilds().forEach(g -> g.updateCommands()
                        .addCommands(
                                Commands.slash("ping", "봇 지연시간 체크"),
                                Commands.slash("echo", "입력 텍스트를 그대로 반환")
                                        .addOption(STRING, "text", "보낼 텍스트", true),
                                Commands.slash("join", "현재 유저의 보이스 채널에 접속"),
                                Commands.slash("leave", "보이스 채널에서 나가기"),
                                Commands.slash("play", "노래 재생/큐에 추가")
                                        .addOption(STRING, "query", "URL 또는 검색어", true),
                                Commands.slash("skip", "다음 트랙으로"),
                                Commands.slash("stop", "정지 및 연결 해제"),
                                Commands.slash("queue", "대기열 보기"),
                                Commands.slash("volume", "현재 음악 볼륨을 조절합니다.")
                                        .addOption(INTEGER, "level", "0~100 사이의 볼륨 (비우면 현재 볼륨 표시)", false)
                        )
                        .queue());
            }
        });
    }
}