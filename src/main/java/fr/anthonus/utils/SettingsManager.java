package fr.anthonus.utils;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.io.FileReader;

public class SettingsManager {
    private static final File settingsFile = new File("conf/settings.json");
    public static File shareFolder;
    public static String shareURL;

    public static void loadSettings() {
        try (FileReader reader = new FileReader(settingsFile)) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();

            if (!json.has("sharingDirectory") || !json.has("sharingURL")) {
                throw new RuntimeException("Le fichier settings.json doit contenir les clés 'sharingDirectory' et 'sharingURL'.");
            }

            if (json.get("sharingDirectory").isJsonNull() || json.get("sharingURL").isJsonNull()) {
                throw new RuntimeException("Les valeurs des clés 'sharingDirectory' et 'sharingURL' ne doivent pas être nulles.");
            }

            String dir = json.get("sharingDirectory").getAsString();
            String url = json.get("sharingURL").getAsString();

            if (dir.isEmpty() || url.isEmpty()) {
                throw new RuntimeException("Les valeurs des clés 'sharingDirectory' et 'sharingURL' ne doivent pas être vides.");
            }

            shareFolder = new File(dir);
            shareURL = url;
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors du chargement des paramètres : " + e.getMessage(), e);
        }
    }


}
