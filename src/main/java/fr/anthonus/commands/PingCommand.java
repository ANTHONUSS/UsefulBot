package fr.anthonus.commands;

import fr.anthonus.logs.LOGs;
import fr.anthonus.logs.logTypes.DefaultLogType;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

public class PingCommand extends Command {
    public PingCommand(SlashCommandInteractionEvent event) {
        super(event);

        LOGs.sendLog("Commande /ping lancée", DefaultLogType.COMMAND);
    }

    @Override
    public void run() {
        long ping = currentEvent.getJDA().getGatewayPing();
        currentEvent.reply("Pong! `" + ping + " ms`").queue();
        LOGs.sendLog("Ping: " + ping + " ms", DefaultLogType.COMMAND);
    }
}
