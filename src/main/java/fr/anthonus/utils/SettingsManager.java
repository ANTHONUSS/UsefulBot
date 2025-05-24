package fr.anthonus.utils;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.io.FileReader;

public class SettingsManager {
    private static final File settingsFile = new File("conf/settings.json");
    public static File shareFolder;
    public static String shareURL;

    public static long animeUpdateChannel;

    public static void loadSettings() {
        try (FileReader reader = new FileReader(settingsFile)) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();

            if (!json.has("sharingDirectory")
                    || !json.has("sharingURL")
                    || !json.has("animeUpdateChannel")) {
                throw new RuntimeException("Le fichier settings.json doit contenir toutes les clés.");
            }

            if (json.get("sharingDirectory").isJsonNull()
                    || json.get("sharingURL").isJsonNull()
                    || json.get("animeUpdateChannel").isJsonNull()) {
                throw new RuntimeException("Les valeurs des clés du json ne doivent pas être nulles.");
            }

            String dir = json.get("sharingDirectory").getAsString();
            String url = json.get("sharingURL").getAsString();

            if (dir.isEmpty() || url.isEmpty()) {
                throw new RuntimeException("Les valeurs des clés 'sharingDirectory' et 'sharingURL' ne doivent pas être vides.");
            }

            shareFolder = new File(dir);
            shareURL = url;
            animeUpdateChannel = json.get("animeUpdateChannel").getAsLong();
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors du chargement des paramètres : " + e.getMessage(), e);
        }
    }


}
