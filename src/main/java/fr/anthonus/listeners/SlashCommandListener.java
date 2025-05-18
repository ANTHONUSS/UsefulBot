package fr.anthonus.listeners;

import fr.anthonus.commands.DownloadCommand;
import fr.anthonus.logs.LOGs;
import fr.anthonus.commands.PingCommand;
import fr.anthonus.logs.logTypes.DefaultLogType;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class SlashCommandListener extends ListenerAdapter {
    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        String commandName = event.getName();

        switch (commandName) {
            case "ping" -> {
                PingCommand pingCommand = new PingCommand(event);
                pingCommand.run();
            }

            case "download" -> {
                String lien = event.getOption("lien").getAsString();
                boolean isMusic = event.getOption("musique").getAsBoolean();

                DownloadCommand downloadCommand = new DownloadCommand(event, lien, isMusic);
                downloadCommand.run();
            }
        }

        LOGs.sendLog("Commande /" + commandName + " terminée", DefaultLogType.COMMAND);
    }
}
