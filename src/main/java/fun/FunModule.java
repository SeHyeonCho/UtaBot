package fun;

import common.command.SlashCommand;
import common.module.BotModule;
import fun.command.ConchCommand;
import fun.command.DiceCommand;
import fun.command.LadderCommand;
import fun.command.PickCommand;

import java.util.List;

/**
 * 놀이 기능 (주사위, 골라줘, 사다리 타기, 마법의 소라고둥)
 */
public class FunModule implements BotModule {

    @Override
    public List<SlashCommand> commands() {
        return List.of(
                new DiceCommand(),
                new PickCommand(),
                new LadderCommand(),
                new ConchCommand()
        );
    }
}
