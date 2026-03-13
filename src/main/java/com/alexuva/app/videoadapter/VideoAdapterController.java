package com.alexuva.app.videoadapter;

import com.alexuva.app.videoadapter.ffmpeg.*;
import com.alexuva.app.videoadapter.util.GpuDetector;
import com.alexuva.app.videoadapter.util.GpuInfo;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
import java.nio.file.Path;

public class VideoAdapterController {

    @FXML
    private TextField filePathField;
    @FXML
    private Label errorText;
    @FXML
    private VBox videoInfo;
    @FXML
    private VBox videoInfoContainer;
    @FXML
    private Button convertBtn;
    @FXML
    private Button cancelBtn;
    @FXML
    private ProgressBar progressBar;
    @FXML
    private Label progressLabel;
    @FXML
    private VBox progressBox;
    @FXML
    private Label successText;
    @FXML
    private HBox setup;
    @FXML
    private ComboBox<String> modeSelector;
    @FXML
    private CheckBox preserveHDR;

    private MediaStreamInfo mediaStreamInfo;
    private GpuInfo gpu;
    private File selectedFile;
    private Path outputPath;
    private FfmpegRunner ffmpeg;
    private boolean isCanceled = false;
    private int qualityLvl = 18;

    @FXML
    public void initialize() {
        modeSelector.getItems().addAll("best", "better");
        modeSelector.setValue("better");
    }

    @FXML
    protected void selectFile() {
        convertBtn.setVisible(false);
        convertBtn.setManaged(false);
        setup.setVisible(false);
        setup.setManaged(false);

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Selecciona un vídeo");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Archivos de vídeo", "*.mp4", "*.mkv", "*.avi", "*.mov")
        );
        Window window = filePathField.getScene().getWindow();
        File file = fileChooser.showOpenDialog(window);
        if (file != null) {
            selectedFile = file;
            filePathField.setText(file.getAbsolutePath());

            Task<MediaStreamInfo> analyzeTask = new Task<>() {
                @Override
                protected MediaStreamInfo call() throws Exception {
                    return FfprobeRunner.analyze(file.toPath());
                }
            };

            analyzeTask.setOnSucceeded(event -> {
                mediaStreamInfo = analyzeTask.getValue();
                videoInfoContainer.getChildren().clear();
                showVideoInfo();
                showAudioInfo();
                showSubtitleInfo();
                videoInfo.setVisible(true);
                convertBtn.setVisible(true);
                convertBtn.setManaged(true);
                setup.setVisible(true);
                setup.setManaged(true);
                preserveHDR.setDisable(!mediaStreamInfo.getVideo().isHDR());
                errorText.setVisible(false);
            });

            analyzeTask.setOnFailed(event -> {
                videoInfo.setVisible(false);
                convertBtn.setVisible(false);
                convertBtn.setManaged(false);
                setup.setVisible(false);
                setup.setManaged(false);
                errorText.setText(analyzeTask.getException().getMessage());
                errorText.setVisible(true);
            });

            new Thread(analyzeTask).start();
        }
    }
    @FXML
    protected void convert() {
        isCanceled = false;

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Guardar como");
        switch (modeSelector.getValue()) {
            case "best", "better" -> fileChooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("Archivos de vídeo", "*.mkv")
            );
            case "compatible" -> fileChooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("Archivos de vídeo", "*.mp4")
            );
        }
        Window window = filePathField.getScene().getWindow();
        fileChooser.setInitialFileName(selectedFile.getName() + "- convertido");
        File file = fileChooser.showSaveDialog(window);
        if (file != null) {
            outputPath = file.toPath();

            Task<GpuInfo> gpuTask = new Task<>() {
                @Override
                protected GpuInfo call() throws Exception {
                    return GpuDetector.detect();
                }
            };

            gpuTask.setOnSucceeded(event -> {
                gpu = gpuTask.getValue();
                ffmpeg = new FfmpegRunner(selectedFile.toPath(), outputPath, modeSelector.getValue(), qualityLvl, preserveHDR.isSelected(), gpu, mediaStreamInfo, null);

                Task<Void> convertTask = new Task<>() {
                    @Override
                    protected Void call() throws Exception {
                        ffmpeg.run(progress -> updateProgress(progress, 100));
                        return null;
                    }
                };

                convertTask.setOnSucceeded(convertEvent -> {
                    cancelBtn.setVisible(false);
                    cancelBtn.setManaged(false);
                    convertBtn.setVisible(true);
                    convertBtn.setManaged(true);
                    setup.setVisible(true);
                    setup.setManaged(true);
                    progressBox.setVisible(false);
                    progressBox.setManaged(false);
                    if (!isCanceled) {
                        successText.setVisible(true);
                        successText.setManaged(true);
                    }
                });

                convertTask.setOnFailed(convertEvent -> {
                    cancelBtn.setVisible(false);
                    cancelBtn.setManaged(false);
                    convertBtn.setVisible(true);
                    convertBtn.setManaged(true);
                    setup.setVisible(true);
                    setup.setManaged(true);
                    progressBox.setVisible(false);
                    progressBox.setManaged(false);
                    errorText.setText(convertTask.getException().getMessage());
                    errorText.setVisible(true);
                });

                convertTask.progressProperty().addListener((observable, oldValue, newValue) -> {
                    int percent = (int) (newValue.doubleValue() * 100);
                    progressLabel.setText(percent + "%");
                });

                successText.setVisible(false);
                successText.setManaged(false);
                cancelBtn.setVisible(true);
                cancelBtn.setManaged(true);
                errorText.setVisible(false);
                convertBtn.setVisible(false);
                convertBtn.setManaged(false);
                setup.setVisible(false);
                setup.setManaged(false);
                progressBar.progressProperty().bind(convertTask.progressProperty());
                progressBox.setVisible(true);
                progressBox.setManaged(true);
                new Thread(convertTask).start();
            });

            gpuTask.setOnFailed(event -> {
                convertBtn.setVisible(true);
                convertBtn.setManaged(true);
                setup.setVisible(true);
                setup.setManaged(true);
                errorText.setText(gpuTask.getException().getMessage());
                errorText.setVisible(true);
            });

            new Thread(gpuTask).start();
        }
    }
    @FXML
    protected void cancel() {
        isCanceled = true;
        if (ffmpeg != null) ffmpeg.stop();
    }
    protected void showVideoInfo() {

        HBox videoName = new HBox();
        StringBuilder videoNameBuilder = new StringBuilder();
        videoNameBuilder.append("Nombre: ").append(mediaStreamInfo.getVideo().getTitle());
        Label videoNameLabel = new Label(videoNameBuilder.toString());
        videoName.setSpacing(5);
        videoName.getChildren().add(videoNameLabel);

        HBox videoSize = new HBox();
        StringBuilder videoSizeBuilder = new StringBuilder();
        videoSizeBuilder.append("Tamaño: ").append(mediaStreamInfo.getVideo().formattedSize());
        Label videoSizeLabel = new Label(videoSizeBuilder.toString());
        videoSize.setSpacing(5);
        videoSize.getChildren().add(videoSizeLabel);

        HBox videoDuration = new HBox();
        StringBuilder videoDurationBuilder = new StringBuilder();
        videoDurationBuilder.append("Duración: ").append(mediaStreamInfo.getVideo().formattedDuration());
        Label videoDurationLabel = new Label(videoDurationBuilder.toString());
        videoDuration.setSpacing(5);
        videoDuration.getChildren().add(videoDurationLabel);

        HBox videoResolution = new HBox();
        StringBuilder videoResolutionBuilder = new StringBuilder();
        videoResolutionBuilder.append("Resolución: ").append(mediaStreamInfo.getVideo().getWidth() + "x" + mediaStreamInfo.getVideo().getHeight());
        Label videoResolutionLabel = new Label(videoResolutionBuilder.toString());
        videoResolution.setSpacing(5);
        videoResolution.getChildren().add(videoResolutionLabel);

        videoInfoContainer.getChildren().addAll(videoName, videoSize, videoDuration, videoResolution);
    }
    protected void showAudioInfo() {
        for (AudioStreamInfo audio : mediaStreamInfo.getAudio()) {
            HBox audioInfo = new HBox();

            StringBuilder audioInfoBuilder = new StringBuilder();
            audioInfoBuilder.append("Channel Layout: ").append(audio.getChannelLayout()).append("\n");
            audioInfoBuilder.append("Idioma: ").append(audio.getLanguage()).append("\n");

            Label audioLabel = new Label(audioInfoBuilder.toString());
            audioInfo.setSpacing(5);
            audioInfo.getChildren().add(audioLabel);

            videoInfoContainer.getChildren().add(audioInfo);
        }
    }
    protected void showSubtitleInfo() {
        for (SubtitleStreamInfo subtitle : mediaStreamInfo.getSubtitle()) {
            HBox subtitleInfo = new HBox();

            StringBuilder subtitleInfoBuilder = new StringBuilder();
            subtitleInfoBuilder.append("Codec: ").append(subtitle.getCodecName()).append("\n");
            subtitleInfoBuilder.append("Idioma: ").append(subtitle.getLanguage()).append("\n");

            Label subtitleLabel = new Label(subtitleInfoBuilder.toString());
            subtitleInfo.setSpacing(5);
            subtitleInfo.getChildren().add(subtitleLabel);

            videoInfoContainer.getChildren().add(subtitleInfo);
        }
    }
}