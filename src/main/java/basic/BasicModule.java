package basic;

import basic.command.EchoCommand;
import basic.command.PingCommand;
import common.command.SlashCommand;
import common.module.BotModule;

import java.util.List;

/**
 * 기본 명령어 (ping, echo)
 */
public class BasicModule implements BotModule {

    @Override
    public List<SlashCommand> commands() {
        return List.of(
                new PingCommand(),
                new EchoCommand()
        );
    }
}
