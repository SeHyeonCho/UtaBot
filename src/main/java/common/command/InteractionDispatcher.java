package common.command;

import common.util.CommandUtils;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 슬래시 명령어와 버튼을 알맞은 곳으로 보내는 클래스입니다.
 *
 * - 슬래시 명령어: 서버에 등록하고, 들어오면 이름으로 찾아 실행 (서버 전용 명령어는 서버에서만)
 * - 버튼: 버튼 ID 앞부분("music:pause"의 "music")으로 ButtonHandler를 찾아 실행
 */
public class InteractionDispatcher extends ListenerAdapter {

    private final Map<String, SlashCommand> commandsByName = new HashMap<>();
    private final Map<String, ButtonHandler> buttonHandlersByPrefix = new HashMap<>();
    private final List<SlashCommandData> commandData = new ArrayList<>();

    /**
     * @throws IllegalArgumentException 명령어 이름이나 버튼 ID 앞부분이 겹치는 경우
     */
    public InteractionDispatcher(List<SlashCommand> commands, List<ButtonHandler> buttonHandlers) {
        for (SlashCommand command : commands) {
            SlashCommandData data = command.data();
            if (commandsByName.putIfAbsent(data.getName(), command) != null) {
                throw new IllegalArgumentException("중복된 명령어 이름: " + data.getName());
            }
            commandData.add(data);
        }

        for (ButtonHandler handler : buttonHandlers) {
            if (buttonHandlersByPrefix.putIfAbsent(handler.idPrefix(), handler) != null) {
                throw new IllegalArgumentException("중복된 버튼 ID 앞부분: " + handler.idPrefix());
            }
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
        if (command == null) {
            return;
        }
        if (command.guildOnly() && !CommandUtils.requireGuild(event)) {
            return;
        }
        command.execute(event);
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        ButtonHandler handler = buttonHandlersByPrefix.get(prefixOf(event.getComponentId()));
        if (handler == null || event.getGuild() == null) {
            return;
        }
        handler.handle(event);
    }

    static String prefixOf(String buttonId) {
        int separator = buttonId.indexOf(ButtonHandler.ID_SEPARATOR);
        return separator < 0 ? buttonId : buttonId.substring(0, separator);
    }

    private void registerTo(Guild guild) {
        guild.updateCommands().addCommands(commandData).queue();
    }
}
