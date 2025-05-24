package fr.anthonus;

import fr.anthonus.autoCommands.AniListAutoCommand;
import fr.anthonus.logs.LOGs;
import fr.anthonus.listeners.SlashCommandListener;
import fr.anthonus.logs.logTypes.DefaultLogType;
import fr.anthonus.utils.SettingsManager;
import fr.anthonus.utils.anilist.AnimeProgressStorage;
import io.github.cdimascio.dotenv.Dotenv;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.interactions.InteractionContextType;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.requests.restaction.CommandListUpdateAction;

import java.io.File;
import java.io.IOException;
import java.util.EnumSet;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static net.dv8tion.jda.api.interactions.commands.OptionType.*;

public class Main {
    private static String tokenDiscord;

    public static JDA jda;

    private static final ScheduledExecutorService animeUpdateExecutor = Executors.newSingleThreadScheduledExecutor();


    public static void main(String[] args) throws IOException, InterruptedException {
        loadEnv();

        LOGs.sendLog("Chargement du bot...", DefaultLogType.LOADING);
        initBot();
        LOGs.sendLog("Bot chargé !", DefaultLogType.LOADING);

        LOGs.sendLog("Chargement des paramètres...", DefaultLogType.LOADING);
        SettingsManager.loadSettings();
        LOGs.sendLog("Paramètres chargés !", DefaultLogType.LOADING);

        LOGs.sendLog("Chargement des progrès des animes...", DefaultLogType.LOADING);
        AnimeProgressStorage.load();
        LOGs.sendLog("Progrès des animes chargés !", DefaultLogType.LOADING);

        LOGs.sendLog("Démarrage de l'exécution des commandes automatiques...", DefaultLogType.LOADING);
        animeUpdateExecutor.scheduleAtFixedRate(
                () -> new AniListAutoCommand(null).run(),
                0,
                1,
                TimeUnit.HOURS
        );
    }

    private static void loadEnv() throws IOException {
        LOGs.sendLog("Chargement de l'environnement...", DefaultLogType.LOADING);
        File configFolder = new File("conf");
        File envFile = new File("conf/.env");
        File settingsFile = new File("conf/settings.json");
        if (!configFolder.exists()) {
            configFolder.mkdirs();
            envFile.createNewFile();
            settingsFile.createNewFile();
            throw new RuntimeException("Les fichiers de configuration ont automatiquement été créés. Veuillez les remplir avec les informations nécessaires.");
        }
        LOGs.sendLog("Environnement chargé !", DefaultLogType.LOADING);


        Dotenv dotenv = Dotenv.configure()
                .directory("conf")
                .load();

        LOGs.sendLog("Chargement du token Discord...", DefaultLogType.LOADING);
        tokenDiscord = dotenv.get("DISCORD_TOKEN");
        if (tokenDiscord == null || tokenDiscord.isEmpty()) {
            throw new RuntimeException("Token Discord non trouvé dans le fichier .env");
        }
        LOGs.sendLog("Token Discord chargé", DefaultLogType.LOADING);
    }

    private static void initBot() throws InterruptedException {
        jda = JDABuilder.createDefault(tokenDiscord)
                .addEventListeners(new SlashCommandListener())
                .build();

        jda.awaitReady();
        LOGs.sendLog("Bot démarré !", DefaultLogType.LOADING);

        LOGs.sendLog("Chargement des commandes...", DefaultLogType.LOADING);
        CommandListUpdateAction commands = jda.updateCommands();
        commands.addCommands(
                Commands.slash("ping", "Ping le bot"),

                Commands.slash("download", "Télécharge une vidéo Youtube")
                        .addOption(STRING, "lien", "Lien de la vidéo Youtube", true)
                        .addOption(BOOLEAN, "musique", "Télécharger la musique ?", true)
        );
        commands.queue();
        LOGs.sendLog("Commandes chargées !", DefaultLogType.LOADING);
    }
}