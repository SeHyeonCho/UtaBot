package handler;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class DefaultCommandHandler extends ListenerAdapter {

    @Override
    public void onReady(ReadyEvent event) {
        System.out.println("로그인 성공 ~!" + event.getJDA().getSelfUser().getAsTag());
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        switch (event.getName()) {
            case "ping" -> event.reply("Pong! " + event.getJDA().getGatewayPing() + "ms").queue();
            case "echo" -> {
                String text = event.getOption("text").getAsString();
                event.reply(text).queue();
            }
            default -> {}
        }
    }
}