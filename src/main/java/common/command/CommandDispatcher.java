package common.command;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 슬래시 명령어를 서버에 등록하고, 들어온 명령어를 이름으로 찾아 실행하는 클래스입니다.
 */
public class CommandDispatcher extends ListenerAdapter {

    private final Map<String, SlashCommand> commandsByName = new HashMap<>();
    private final List<SlashCommandData> commandData = new ArrayList<>();

    public CommandDispatcher(List<SlashCommand> commands) {
        for (SlashCommand command : commands) {
            SlashCommandData data = command.data();
            if (commandsByName.putIfAbsent(data.getName(), command) != null) {
                throw new IllegalArgumentException("중복된 명령어 이름: " + data.getName());
            }
            commandData.add(data);
        }
    }

    public List<SlashCommandData> getCommandData() {
        return List.copyOf(commandData);
    }

    @Override
    public void onReady(ReadyEvent event) {
        // 전역 명령어는 비우고 서버별로만 등록
        event.getJDA().updateCommands().queue();
        for (Guild guild : event.getJDA().getGuilds()) {
            registerTo(guild);
        }
    }

    @Override
    public void onGuildJoin(GuildJoinEvent event) {
        registerTo(event.getGuild());
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        SlashCommand command = commandsByName.get(event.getName());
        if (command != null) {
            command.execute(event);
        }
    }

    private void registerTo(Guild guild) {
        guild.updateCommands().addCommands(commandData).queue();
    }
}
