package fr.anthonus.commands;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

public abstract class Command {
    protected SlashCommandInteractionEvent currentEvent;

    public Command(SlashCommandInteractionEvent event) {
        this.currentEvent = event;
    }

    public abstract void run();
}
