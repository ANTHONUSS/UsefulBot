package fr.anthonus.commands;


import fr.anthonus.logs.LOGs;
import fr.anthonus.logs.logTypes.DefaultLogType;
import fr.anthonus.utils.SettingsManager;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.utils.FileUpload;

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
import java.util.Comparator;
import java.util.Optional;
import java.util.stream.Stream;

public class DownloadCommand extends Command {
    private final String lien;
    private final boolean isMusic;

    public DownloadCommand(SlashCommandInteractionEvent event, String lien, boolean isMusic) {
        super(event);
        this.lien = lien;
        this.isMusic = isMusic;

        LOGs.sendLog("Commande /download lancée", DefaultLogType.COMMAND);
    }

    @Override
    public void run() {
        currentEvent.deferReply().queue();

        download();
        currentEvent.getHook().sendMessage("## :arrow_down: Téléchargement de la " + (isMusic ? "musique" : "vidéo") + " en cours...")
                .queue();
        LOGs.sendLog("Début du téléchargement de la " + (isMusic ? "musique" : "vidéo") + "...", DefaultLogType.DOWNLOAD);

    }

    private void download() {
        Thread downloadThread = new Thread(() -> {

            try (Stream<Path> files = Files.list(Paths.get("temp"))) {
                files.forEach(path -> {
                    try {
                        Files.deleteIfExists(path);
                    } catch (IOException e) {
                        LOGs.sendLog("Erreur lors de la suppression du fichier temporaire : " + path.getFileName() + " - " + e.getMessage(), DefaultLogType.ERROR);
                    }
                });
            } catch (IOException e) {
                LOGs.sendLog("Erreur lors du nettoyage du dossier temp : " + e.getMessage(), DefaultLogType.ERROR);
            }


            String videoName = getVideoNameFromURL();

            currentEvent.getHook().editOriginal("## :arrow_down: Téléchargement de `" + videoName + "` en cours...")
                    .queue();
            LOGs.sendLog("Téléchargement de `" + videoName + "` en cours...", DefaultLogType.DOWNLOAD);

            File tempFolder = new File("temp");
            if (!tempFolder.exists()) {
                if (tempFolder.mkdir()) {
                    LOGs.sendLog("Dossier temporaire créé", DefaultLogType.DOWNLOAD);
                } else {
                    currentEvent.getHook().editOriginal("## :x: Erreur lors de la création du dossier temporaire.").queue();
                    LOGs.sendLog("Erreur lors de la création du dossier temporaire", DefaultLogType.ERROR);
                    return;
                }
            }

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

            try {
                Process process = processBuilder.start();

                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
                String line;

                while ((line = reader.readLine()) != null) {
                    LOGs.sendLog(line, DefaultLogType.DOWNLOAD);
                }

                while ((line = errorReader.readLine()) != null) {
                    LOGs.sendLog(line, DefaultLogType.ERROR);
                }

                int exitCode = process.waitFor();

                if (exitCode == 0) {
                    currentEvent.getHook().editOriginal("## :arrow_up: upload de `" + videoName + "` en cours...").queue();
                    LOGs.sendLog("upload en cours...", DefaultLogType.DOWNLOAD);
                } else {
                    currentEvent.getHook().editOriginal("## :x: Erreur lors du téléchargement de `" + videoName + "`").queue();
                    LOGs.sendLog("Erreur lors du téléchargement de `" + videoName + "`", DefaultLogType.ERROR);
                    return;
                }

            } catch (IOException | InterruptedException e) {
                currentEvent.getHook().editOriginal("## :x: Erreur lors du téléchargement de `" + videoName + "`").queue();
                LOGs.sendLog("Erreur lors du téléchargement de `" + videoName + "`" + e, DefaultLogType.ERROR);
                return;
            }

            uploadFile();

        });
        downloadThread.start();
    }

    private String getVideoNameFromURL() {
        String videoName;

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

            if (videoName == null || videoName.isEmpty()) videoName = "Titre inconnu";

            return videoName;
        } catch (InterruptedException | IOException e) {
            currentEvent.getHook().editOriginal("## :x: Erreur lors de la récupération du titre de la vidéo.").queue();
            LOGs.sendLog("Erreur lors de la récupération du titre de la vidéo", DefaultLogType.ERROR);
            throw new RuntimeException(e);
        }
    }

    private void uploadFile() {
        Path tempPath = Paths.get("temp");
        File videoFile = null;

        try (Stream<Path> files = Files.list(tempPath)) {
            Optional<Path> file = files.findFirst();

            if (file.isEmpty()) {
                currentEvent.getHook().editOriginal("## :x: Une erreur est survenue lors de l'upload du fichier...").queue();
                LOGs.sendLog("Aucun fichier trouvé dans le dossier temp", DefaultLogType.ERROR);
                return;
            }

            videoFile = file.get().toFile();

        } catch (IOException e) {
            currentEvent.getHook().editOriginal("## :x: Une erreur est survenue lors de l'upload du fichier...").queue();
            LOGs.sendLog(e.getMessage(), DefaultLogType.ERROR);
            return;
        }

        if (videoFile == null) {
            currentEvent.getHook().editOriginal("## :x: Une erreur est survenue lors de l'upload du fichier...").queue();
            LOGs.sendLog("Aucun fichier trouvé dans le dossier temp", DefaultLogType.ERROR);
            return;
        }

        long taille = videoFile.length();
        double tailleMo = (double) taille / 1024 / 1024;
        if (tailleMo < 8) {
            currentEvent.getHook().editOriginal("## :white_check_mark: Upload terminé !\n" +
                            "Voici le fichier à télécharger :")
                    .queue();
            currentEvent.getHook().sendFiles(FileUpload.fromData(videoFile)).queue();
            LOGs.sendLog("Upload terminé dans discord !", DefaultLogType.DOWNLOAD);
        } else {
            Path source = Paths.get(videoFile.getAbsolutePath());
            Path destination = Paths.get(SettingsManager.shareFolder + "/" + videoFile.getName());
            try {
                Files.move(source, destination, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                currentEvent.getHook().editOriginal("## :x: Une erreur est survenue lors de l'upload du fichier...").queue();
                LOGs.sendLog(e.getMessage(), DefaultLogType.ERROR);
            }

            String encodedFileName = URLEncoder.encode(videoFile.getName(), StandardCharsets.UTF_8);
            String musicURI = SettingsManager.shareURL + "/" + encodedFileName;
            musicURI = musicURI.replace("+", "%20");
            currentEvent.getHook().editOriginal("## :white_check_mark: Upload terminé !\n" +
                            "Voici le lien de téléchargement : " + musicURI)
                    .queue();
            LOGs.sendLog("Upload terminé sur le serveur !", DefaultLogType.DOWNLOAD);
        }

    }
}
