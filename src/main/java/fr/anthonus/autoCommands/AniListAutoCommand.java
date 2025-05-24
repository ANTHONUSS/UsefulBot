package fr.anthonus.autoCommands;

import fr.anthonus.logs.LOGs;
import fr.anthonus.logs.logTypes.DefaultLogType;
import fr.anthonus.utils.SettingsManager;
import fr.anthonus.utils.anilist.AniListAPICaller;
import fr.anthonus.utils.anilist.Anime;
import fr.anthonus.utils.anilist.AnimeProgressStorage;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.GenericCommandInteractionEvent;

import static fr.anthonus.Main.jda;

public class AniListAutoCommand extends AutoCommand {
    public AniListAutoCommand(GenericCommandInteractionEvent event) {
        super(event);

        LOGs.sendThreadedLog("AutoCommande AniListAutoCommand initialisée", DefaultLogType.AUTO_COMMAND);
    }

    @Override
    public void run() {
        LOGs.sendThreadedLog("Mise à jour de la liste AniList...", DefaultLogType.AUTO_COMMAND);
        AniListAPICaller.updateAnimeList();
        LOGs.sendThreadedLog("Liste AniList mise à jour", DefaultLogType.AUTO_COMMAND);

        long animeUpdateChannel = SettingsManager.animeUpdateChannel;

        for (Anime anime : Anime.animes.values()) {
            int lastEpisode = anime.getLastEpisode();
            int lastCheckedEpisode = anime.getLastCheckedEpisode();

            if (lastEpisode - lastCheckedEpisode > 0) {
                EmbedBuilder embed = new EmbedBuilder();
                embed.setTitle(anime.getTitle() + " - Épisode " + lastEpisode);
                embed.setDescription("L'épisode " + lastEpisode + " est maintenant disponible !");
                embed.setThumbnail(anime.getImageLink());

                jda.getTextChannelById(animeUpdateChannel).sendMessageEmbeds(embed.build()).queue();
                LOGs.sendThreadedLog("Mise à jour de l'épisode de " + anime.getTitle() + " : " + lastEpisode, DefaultLogType.AUTO_COMMAND);
                anime.setLastCheckedEpisode(lastEpisode);
                AnimeProgressStorage.setProgress(anime.getId(), lastEpisode);
            }
        }
        AnimeProgressStorage.save();
    }
}
