package fr.anthonus.utils.anilist;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import fr.anthonus.logs.LOGs;
import fr.anthonus.logs.logTypes.DefaultLogType;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

public class AnimeProgressStorage {
    private static final File storageFile = new File("data/animeData.json");
    private static final Gson gson = new Gson();

    private static Map<Integer, Integer> progressMap = new HashMap<>();

    public static void setProgress(int animeId, int episode) {
        progressMap.put(animeId, episode);
    }

    public static int getProgress(int animeId) {
        return progressMap.getOrDefault(animeId, -1);
    }

    public static void save(){
        try(Writer writer = Files.newBufferedWriter(storageFile.toPath())){
            gson.toJson(progressMap, writer);
        } catch (IOException e) {
            LOGs.sendLog("Erreur lors de la sauvegarde des progrès des animes : " + e.getMessage(), DefaultLogType.ERROR);
            return;
        }
    }

    public static void load(){
        if (!storageFile.exists()) {
            throw new RuntimeException("Le fichier de stockage des progrès des animes n'existe pas");
        }

        try (var reader = Files.newBufferedReader(storageFile.toPath())) {
            Type type = new TypeToken<Map<Integer, Integer>>(){}.getType();
            progressMap = gson.fromJson(reader, type);
        } catch (IOException e) {
            throw new RuntimeException("Erreur lors du chargement des progrès des animes : " + e.getMessage());
        }
    }

}
