package fr.anthonus.utils.anilist;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import fr.anthonus.Main;
import fr.anthonus.logs.LOGs;
import fr.anthonus.logs.logTypes.DefaultLogType;
import okhttp3.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class AniListAPICaller {
    private static final String API_URL = "https://graphql.anilist.co";
    private static final OkHttpClient client = new OkHttpClient();


    public static void updateAnimeList() {
        LOGs.sendThreadedLog("Récupération des données", DefaultLogType.AUTO_COMMAND);
        String query = getQuery();
        LOGs.sendThreadedLog("données récupérées", DefaultLogType.AUTO_COMMAND);

        if (query == null) {
            LOGs.sendLog("La requête est nulle", DefaultLogType.ERROR);
            return;
        }

        LOGs.sendThreadedLog("Création de la requête...", DefaultLogType.AUTO_COMMAND);
        JsonObject queryJson = new JsonObject();
        queryJson.addProperty("query", query);

        RequestBody body = RequestBody.create(
                queryJson.toString(),
                MediaType.parse("application/json")
        );
        Request request = new Request.Builder()
                .url(API_URL)
                .post(body)
                .build();
        LOGs.sendThreadedLog("Requête créée", DefaultLogType.AUTO_COMMAND);

        LOGs.sendThreadedLog("Envoi de la requête à l'API Anilist...", DefaultLogType.AUTO_COMMAND);
        try (Response response = client.newCall(request).execute()) {
            LOGs.sendThreadedLog("Requête envoyée avec succès", DefaultLogType.AUTO_COMMAND);
            if (!response.isSuccessful()) {
                LOGs.sendLog("Erreur lors de l'appel à l'API Anilist : " + response.body().string(), DefaultLogType.ERROR);
                return;
            }
            LOGs.sendThreadedLog("Réponse reçue de l'API Anilist", DefaultLogType.AUTO_COMMAND);


            LOGs.sendThreadedLog("Traitement de la réponse...", DefaultLogType.AUTO_COMMAND);
            LOGs.sendThreadedLog("écriture de la réponse en mémoire...", DefaultLogType.AUTO_COMMAND);
            String result = response.body().string();
            JsonObject data = JsonParser.parseString(result).getAsJsonObject().getAsJsonObject("data");

            JsonArray lists = data.getAsJsonObject("MediaListCollection").getAsJsonArray("lists");
            LOGs.sendThreadedLog("Réponse écrite en mémoire", DefaultLogType.AUTO_COMMAND);

            LOGs.sendThreadedLog("Traitement des listes d'animes...", DefaultLogType.AUTO_COMMAND);
            for (JsonElement listElem : lists) {
                JsonArray entries = listElem.getAsJsonObject().getAsJsonArray("entries");
                for (JsonElement entryElem : entries) {
                    JsonObject media = entryElem.getAsJsonObject().getAsJsonObject("media");

                    String title = media.getAsJsonObject("title").get("english").isJsonNull()
                            ? media.getAsJsonObject("title").get("romaji").getAsString()
                            : media.getAsJsonObject("title").get("english").getAsString();

                    int totalEpisodes = media.has("episodes") && !media.get("episodes").isJsonNull()
                            ? media.get("episodes").getAsInt()
                            : 0;

                    int id = media.get("id").getAsInt();

                    String url = media.get("siteUrl").getAsString();

                    String image = media.getAsJsonObject("coverImage").get("large").getAsString();

                    JsonElement nextEpElem = media.get("nextAiringEpisode");

                    if (nextEpElem != null && !nextEpElem.isJsonNull()) {
                        JsonObject nextEp = media.getAsJsonObject("nextAiringEpisode");
                        int episode = nextEp.get("episode").getAsInt();
                        updateAnime(title, id, url, image, episode);
                    } else if (totalEpisodes > 0) {
                        updateAnime(title, id, url, image, totalEpisodes);
                    }
                }
            }
            LOGs.sendThreadedLog("Listes d'animes traitées", DefaultLogType.AUTO_COMMAND);

        } catch (IOException e) {
            LOGs.sendLog(e.getMessage(), DefaultLogType.ERROR);
            return;
        }

    }

    private static void updateAnime(String title, int id, String url, String image, int episode) {
        if (!Anime.animes.containsKey(id)) {
            Anime anime = new Anime(id, title, image, url, episode);
            int lastChecked = AnimeProgressStorage.getProgress(id);
            if (lastChecked > -1) {
                anime.setLastCheckedEpisode(lastChecked);
            }
            Anime.animes.put(id, anime);
        } else {
            Anime anime = Anime.animes.get(id);
            if (anime.getLastEpisode() < episode) {
                anime.setLastEpisode(episode);
            }
        }
    }

    private static String getQuery() {
        LOGs.sendThreadedLog("Récupération de la requête Anilist", DefaultLogType.AUTO_COMMAND);
        File queryFile = new File("data/anilist.graphql");
        if (!queryFile.exists()) {
            LOGs.sendLog("Le fichier de requête Anilist n'existe pas !", DefaultLogType.ERROR);
            return null;
        }

        StringBuilder query = new StringBuilder();

        try (BufferedReader br = new BufferedReader(new FileReader(queryFile))) {
            String line;
            while ((line = br.readLine()) != null) {
                query.append(line).append("\n");
            }
        } catch (IOException e) {
            LOGs.sendLog("Erreur lors de la lecture du fichier de requête Anilist : " + e.getMessage(), DefaultLogType.ERROR);
            return null;
        }
        LOGs.sendThreadedLog("Requête Anilist récupérée", DefaultLogType.AUTO_COMMAND);

        return query.toString();
    }
}
