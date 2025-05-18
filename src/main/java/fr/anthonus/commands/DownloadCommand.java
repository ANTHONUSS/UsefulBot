package fr.anthonus.commands;


import fr.anthonus.logs.LOGs;
import fr.anthonus.logs.logTypes.DefaultLogType;
import fr.anthonus.utils.SettingsManager;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.stream.Stream;

public class DownloadCommand extends Command {
    private final String lien;
    private final boolean isMusic;

    public DownloadCommand(SlashCommandInteractionEvent event, String lien, boolean isMusic) {
        super(event);
        this.lien = lien;
        this.isMusic = isMusic;

        LOGs.sendLog("Commande /download lancée en mode " + (isMusic ? "musique" : "vidéo"), DefaultLogType.COMMAND);
    }

    @Override
    public void run() {
        currentEvent.deferReply().queue();

        download();
    }

    private void download() {
        Thread downloadThread = new Thread(() -> {

            currentEvent.getHook().sendMessage("## :arrow_down: Téléchargement de la " + (isMusic ? "musique" : "vidéo") + " en cours...")
                    .queue();
            LOGs.sendThreadedLog("Début du téléchargement de la " + (isMusic ? "musique" : "vidéo") + "...", DefaultLogType.DOWNLOAD);

            clearTempFolder();

            String videoName = getVideoNameFromURL();

            currentEvent.getHook().editOriginal("## :arrow_down: Téléchargement de `" + videoName + "` en cours...").queue();
            LOGs.sendThreadedLog("Téléchargement de `" + videoName + "` en cours...", DefaultLogType.DOWNLOAD);

            File tempFolder = new File("temp");
            if (!tempFolder.exists()) {
                if (tempFolder.mkdir()) {
                    LOGs.sendThreadedLog("Dossier temp créé", DefaultLogType.FILE_LOADING);
                } else {
                    currentEvent.getHook().editOriginal("## :x: Erreur lors de la création du dossier temporaire.").queue();
                    LOGs.sendThreadedLog("Erreur lors de la création du dossier temporaire", DefaultLogType.ERROR);
                    return;
                }
            }

            LOGs.sendThreadedLog("création du process...", DefaultLogType.DOWNLOAD);
            ProcessBuilder processBuilder;

            if (isMusic) processBuilder = new ProcessBuilder(
                    "yt-dlp.exe",
                    "--embed-thumbnail",
                    "--embed-metadata",
                    "-f", "bestaudio",
                    "-x",
                    "--audio-format", "mp3",
                    "--audio-quality", "320k",
                    "--output", "temp/%(title)s.%(ext)s",
                    lien
            );
            else processBuilder = new ProcessBuilder(
                    "yt-dlp.exe",
                    "--embed-thumbnail",
                    "--embed-metadata",
                    "-f", "bestvideo[ext=mp4]+bestaudio[ext=m4a]/best[ext=mp4]/best",
                    "--merge-output-format", "mp4",
                    "--output", "temp/%(title)s.%(ext)s",
                    lien
            );
            LOGs.sendThreadedLog("process créé", DefaultLogType.DOWNLOAD);


            try {
                LOGs.sendThreadedLog("lancement du téléchargement...", DefaultLogType.DOWNLOAD);
                Process process = processBuilder.start();

                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
                String line;

                while ((line = reader.readLine()) != null) {
                    LOGs.sendThreadedLog(line, DefaultLogType.DOWNLOAD_CMD);
                }

                while ((line = errorReader.readLine()) != null) {
                    LOGs.sendThreadedLog(line, DefaultLogType.ERROR_CMD);
                }

                int exitCode = process.waitFor();

                if (exitCode != 0) {
                    currentEvent.getHook().editOriginal("## :x: Erreur lors du téléchargement de `" + videoName + "` avec le code d'erreur " + exitCode).queue();
                    LOGs.sendThreadedLog("Erreur lors du téléchargement de `" + videoName + "` avec le code d'erreur " + exitCode, DefaultLogType.ERROR);
                    return;
                }

            } catch (IOException | InterruptedException e) {
                currentEvent.getHook().editOriginal("## :x: Erreur lors du téléchargement de `" + videoName + "`").queue();
                LOGs.sendThreadedLog("Erreur lors du téléchargement de " + videoName + " : " + e.getMessage(), DefaultLogType.ERROR);
                return;
            }

            uploadFile();

        });
        downloadThread.start();
        LOGs.sendLog("Thread de téléchargement lancé", DefaultLogType.COMMAND);
    }

    private void clearTempFolder() {
        LOGs.sendThreadedLog("Nettoyage du dossier temp...", DefaultLogType.FILE_LOADING);
        try (Stream<Path> files = Files.list(Paths.get("temp"))) {
            files.forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                    LOGs.sendThreadedLog("Fichier temporaire supprimé : " + path.getFileName(), DefaultLogType.FILE_LOADING);
                } catch (IOException e) {
                    LOGs.sendThreadedLog("Erreur lors de la suppression du fichier temporaire : " + path.getFileName() + " - " + e.getMessage(), DefaultLogType.ERROR);
                }
            });

            LOGs.sendThreadedLog("dossier temp nettoyé", DefaultLogType.FILE_LOADING);
        } catch (IOException e) {
            LOGs.sendThreadedLog("Erreur lors du nettoyage du dossier temp : " + e.getMessage(), DefaultLogType.ERROR);
        }
    }

    private String getVideoNameFromURL() {
        String videoName;

        LOGs.sendThreadedLog("Récupération du titre de la vidéo...", DefaultLogType.DOWNLOAD);
        try {
            ProcessBuilder titleProcessBuilder = new ProcessBuilder(
                    "yt-dlp.exe",
                    "--get-title",
                    "--no-playlist",
                    lien
            );
            Process titleProcess = titleProcessBuilder.start();

            BufferedReader titleReader = new BufferedReader(new InputStreamReader(titleProcess.getInputStream()));
            videoName = titleReader.readLine();
            titleProcess.waitFor();

            if (videoName == null || videoName.isEmpty()) {
                videoName = "Titre inconnu";
                LOGs.sendThreadedLog("Titre de la vidéo introuvable", DefaultLogType.WARNING);
            }

            LOGs.sendThreadedLog("Titre de la vidéo : " + videoName, DefaultLogType.DOWNLOAD);
            return videoName;
        } catch (InterruptedException | IOException e) {
            currentEvent.getHook().editOriginal("## :x: Erreur lors de la récupération du titre de la vidéo.").queue();
            LOGs.sendThreadedLog("Erreur lors de la récupération du titre de la vidéo", DefaultLogType.ERROR);
            return null;
        }

    }

    private void uploadFile() {
        LOGs.sendThreadedLog("Upload du fichier sur le serveur...", DefaultLogType.DOWNLOAD);
        Path tempPath = Paths.get("temp");
        File videoFile;

        LOGs.sendThreadedLog("Recherche du fichier dans le dossier temp...", DefaultLogType.DOWNLOAD);
        try (Stream<Path> files = Files.list(tempPath)) {
            Optional<Path> file = files.findFirst();

            if (file.isEmpty()) {
                currentEvent.getHook().editOriginal("## :x: Une erreur est survenue lors de l'upload du fichier...").queue();
                LOGs.sendThreadedLog("Aucun fichier trouvé dans le dossier temp", DefaultLogType.ERROR);
                return;
            }

            videoFile = file.get().toFile();
            LOGs.sendThreadedLog("Fichier trouvé : " + videoFile.getName(), DefaultLogType.DOWNLOAD);

        } catch (IOException e) {
            currentEvent.getHook().editOriginal("## :x: Une erreur est survenue lors de l'upload du fichier...").queue();
            LOGs.sendThreadedLog(e.getMessage(), DefaultLogType.ERROR);
            return;
        }

        currentEvent.getHook().editOriginal("## :arrow_up: upload de `" + videoFile.getName() + "` en cours...").queue();

        LOGs.sendThreadedLog("Déplacement du fichier vers le dossier de partage...", DefaultLogType.DOWNLOAD);
        Path source = Paths.get(videoFile.getAbsolutePath());
        Path destination = Paths.get(SettingsManager.shareFolder + "/" + videoFile.getName());
        try {
            Files.move(source, destination, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            currentEvent.getHook().editOriginal("## :x: Une erreur est survenue lors de l'upload du fichier...").queue();
            LOGs.sendThreadedLog(e.getMessage(), DefaultLogType.ERROR);
        }
        LOGs.sendThreadedLog("Fichier déplacé vers le dossier de partage", DefaultLogType.DOWNLOAD);

        LOGs.sendThreadedLog("Création du lien de partage...", DefaultLogType.DOWNLOAD);
        String encodedFileName = URLEncoder.encode(videoFile.getName(), StandardCharsets.UTF_8);
        String musicURI = SettingsManager.shareURL + "/" + encodedFileName;
        musicURI = musicURI.replace("+", "%20");
        LOGs.sendThreadedLog("Lien de partage créé : " + musicURI, DefaultLogType.DOWNLOAD);

        currentEvent.getHook().editOriginal("## :white_check_mark: Upload terminé !\n" +
                        "Voici le lien de téléchargement : " + musicURI)
                .queue();
        LOGs.sendThreadedLog("Upload terminé !", DefaultLogType.DOWNLOAD);

    }
}
