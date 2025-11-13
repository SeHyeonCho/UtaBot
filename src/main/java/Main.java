import handler.DefaultCommandHandler;
import handler.VoiceCommandHandler;
import handler.music.MusicCommandHandler;
import io.github.cdimascio.dotenv.Dotenv;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;

public class Main {

    public static void main(String[] args) {
        Dotenv dotenv = Dotenv.load();
        String token = dotenv.get("TOKEN");

        JDA jda = JDABuilder.create(
                        token,
                        GatewayIntent.GUILD_MESSAGES,
                        GatewayIntent.GUILD_MEMBERS,
                        GatewayIntent.MESSAGE_CONTENT,
                        GatewayIntent.GUILD_VOICE_STATES
                )
                .addEventListeners(
                        new DefaultCommandHandler(),
                        new VoiceCommandHandler(),
                        new MusicCommandHandler()
                )
                .build();

        CommandRegister.registerCommand(jda);
    }
}