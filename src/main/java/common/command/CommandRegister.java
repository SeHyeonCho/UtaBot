package common.command;

import static net.dv8tion.jda.api.interactions.commands.OptionType.CHANNEL;
import static net.dv8tion.jda.api.interactions.commands.OptionType.INTEGER;
import static net.dv8tion.jda.api.interactions.commands.OptionType.STRING;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;

import java.util.ArrayList;
import java.util.List;

public class CommandRegister {

    public CommandRegister() {
    }

    public static void registerCommand(JDA jda) {
        jda.addEventListener(new ListenerAdapter() {
            @Override
            public void onReady(ReadyEvent event) {
                for (Guild g : event.getJDA().getGuilds()) {
                    List<SlashCommandData> commands = buildAllCommands();
                    g.updateCommands().addCommands(commands).queue();
                }
            }
        });
    }

    private static List<SlashCommandData> buildAllCommands() {
        List<SlashCommandData> commands = new ArrayList<>();
        
        commands.addAll(buildBasicCommands());
        commands.addAll(buildVoiceCommands());
        commands.addAll(buildMusicCommands());
        commands.addAll(buildConfigCommands());
        
        return commands;
    }

    private static List<SlashCommandData> buildBasicCommands() {
        List<SlashCommandData> commands = new ArrayList<>();
        
        commands.add(Commands.slash("ping", "봇 지연시간 체크"));
        commands.add(Commands.slash("echo", "입력 텍스트를 그대로 반환")
                .addOption(STRING, "text", "보낼 텍스트", true));
        
        return commands;
    }

    private static List<SlashCommandData> buildVoiceCommands() {
        List<SlashCommandData> commands = new ArrayList<>();
        
        commands.add(Commands.slash("join", "현재 유저의 보이스 채널에 접속"));
        commands.add(Commands.slash("leave", "보이스 채널에서 나가기"));
        
        return commands;
    }

    private static List<SlashCommandData> buildMusicCommands() {
        List<SlashCommandData> commands = new ArrayList<>();
        
        commands.add(Commands.slash("play", "노래 재생/큐에 추가")
                .addOption(STRING, "query", "URL 또는 검색어", true));
        commands.add(Commands.slash("skip", "다음 트랙으로"));
        commands.add(Commands.slash("stop", "정지 및 연결 해제"));
        commands.add(Commands.slash("queue", "대기열 보기"));
        commands.add(Commands.slash("volume", "현재 음악 볼륨을 조절합니다.")
                .addOption(INTEGER, "level", "0~100 사이의 볼륨 (비우면 현재 볼륨 표시)", false));
        
        return commands;
    }

    private static List<SlashCommandData> buildConfigCommands() {
        List<SlashCommandData> commands = new ArrayList<>();
        
        commands.add(Commands.slash("setentrytime", "입장곡 재생 시간을 설정합니다.")
                .addOption(INTEGER, "start", "시작 시간(초)", true)
                .addOption(INTEGER, "duration", "재생 길이(초)", true));
        commands.add(Commands.slash("setuploadchannel", "이 서버의 mp3 업로드 채널을 설정합니다.")
                .addOption(CHANNEL, "channel", "업로드용 텍스트 채널", true));
        commands.add(Commands.slash("setentrysong", "입장곡을 유튜브 링크로 설정합니다.")
                .addOption(STRING, "url", "유튜브 또는 기타 URL", true));
        
        return commands;
    }
}


