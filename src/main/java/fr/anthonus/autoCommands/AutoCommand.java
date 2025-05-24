package fr.anthonus.autoCommands;

import net.dv8tion.jda.api.events.interaction.command.GenericCommandInteractionEvent;

public abstract class AutoCommand {
    protected GenericCommandInteractionEvent currentEvent;

    public AutoCommand(GenericCommandInteractionEvent event) {
        this.currentEvent = event;
    }

    public abstract void run();
}
