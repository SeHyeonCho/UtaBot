package music.command;

import common.command.SlashCommand;
import common.util.CommandUtils;
import music.service.MusicManager;
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;

public class JoinCommand implements SlashCommand {

    private final MusicManager musicManager;

    public JoinCommand(MusicManager musicManager) {
        this.musicManager = musicManager;
    }

    @Override
    public SlashCommandData data() {
        return Commands.slash("join", "현재 유저의 보이스 채널에 접속");
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        if (!CommandUtils.requireGuild(event)) {
            return;
        }

        AudioChannel userChannel = CommandUtils.requireVoiceChannel(event);
        if (userChannel == null) {
            return;
        }

        musicManager.of(event.getGuild()).connect(userChannel);
        event.reply("🔊 `" + userChannel.getName() + "` 채널로 접속했어요!").queue();
    }
}
