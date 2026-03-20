package com.alexuva.app.videoadapter;

import com.alexuva.app.videoadapter.ffmpeg.VideoStreamInfo;
import com.alexuva.app.videoadapter.ffmpeg.AudioStreamInfo;
import com.alexuva.app.videoadapter.ffmpeg.SubtitleStreamInfo;
import atlantafx.base.layout.InputGroup;
import atlantafx.base.theme.Styles;
import atlantafx.base.util.Animations;
import com.alexuva.app.videoadapter.ffmpeg.*;
import com.alexuva.app.videoadapter.util.GpuDetector;
import com.alexuva.app.videoadapter.util.GpuInfo;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.HorizontalDirection;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import javafx.util.Duration;
import atlantafx.base.controls.ToggleSwitch;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

public class VideoAdapterController {

    @FXML
    private VBox title;
    @FXML
    private TextField filePathField;
    @FXML
    private Button browseBtn;
    @FXML
    private VBox videoInfoContainer;
    @FXML
    private HBox fileRow;
    @FXML
    private VBox infoContainer;
    @FXML
    private VBox loader;
    @FXML
    private VBox progressBarContainer;
    @FXML
    private VBox buttonsContainer;
    @FXML
    private HBox msgContainer;

    private MediaStreamInfo mediaStreamInfo;
    private GpuInfo gpu;
    private File selectedFile;
    private Path outputPath;
    private FfmpegRunner ffmpeg;
    private boolean isCanceled = false;
    private int qualityLvl = 18;
    private List<String> modeList = List.of("best", "better");
    private boolean preserveHDR = false;
    private String selectedMode = "best";

    @FXML
    public void initialize() {
        //Create a file selector row
        filePathField = new TextField();
        filePathField.setEditable(false);
        filePathField.setPrefWidth(350);
        filePathField.setPromptText("Selecciona un archivo");
        filePathField.setOnAction(e -> selectFile());

        browseBtn = new Button("", new FontIcon("mdal-folder"));
        browseBtn.getStyleClass().add(Styles.BUTTON_ICON);
        browseBtn.setOnAction(e -> selectFile());

        InputGroup group = new InputGroup(filePathField, browseBtn);
        fileRow.getChildren().add(group);

        Animations.fadeIn(title, Duration.seconds(3)).playFromStart();
        Animations.fadeIn(fileRow, Duration.seconds(3)).playFromStart();
        //End create a file selector row

        detectGpu();
    }
    protected void detectGpu() {

        Task<GpuInfo>gpuDetectorTask = new Task<>(){
            @Override
            protected GpuInfo call() throws Exception {
                return GpuDetector.detect();
            }
        };

        gpuDetectorTask.setOnSucceeded(event -> {
            hideLoader();
            gpu = gpuDetectorTask.getValue();
        });

        gpuDetectorTask.setOnFailed(event -> {
            hideLoader();
            createMsg("No se pudo detectar la GPU", "error");
        });

        createLoader("Detectando GPU...");
        new Thread(gpuDetectorTask).start();

    }
    protected void selectFile() {
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
                hideLoader();
                mediaStreamInfo = analyzeTask.getValue();
                showVideoInfo();
                createButton("Convertir", "convert", mediaStreamInfo.getVideo().isHDR());
            });

            analyzeTask.setOnFailed(event -> {
                hideLoader();
                createMsg("No se pudo analizar el archivo", "error");
            });

            hideVideoInfo();
            hideButton();
            hideMsg();
            createLoader("Analizando archivo...");
            new Thread(analyzeTask).start();
        }
    }
    protected void convert() {
        isCanceled = false;

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Guardar como");
        switch (selectedMode) {
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

            ffmpeg = new FfmpegRunner(selectedFile.toPath(), outputPath, selectedMode, qualityLvl, preserveHDR, gpu, mediaStreamInfo, null);

            Task<Void> convertTask = new Task<>() {
                @Override
                protected Void call() throws Exception {
                    ffmpeg.run(progress -> updateProgress(progress, 100));
                    return null;
                }
            };

            convertTask.setOnSucceeded(convertEvent -> {
                hideButton();
                if (isCanceled) {
                    createMsg("Conversión cancelada", "info");
                } else {
                    createMsg("Archivo convertido correctamente", "success");
                }
            });

            convertTask.setOnFailed(convertEvent -> {
                hideButton();
                hideProgressBar();
                createMsg("No se pudo convertir el archivo", "error");
            });

            convertTask.setOnCancelled(convertEvent -> {
               hideButton();
               hideProgressBar();
            });

            hideButton();
            createButton("Cancelar", "cancel", false);
            createProgressBar(convertTask);
            new Thread(convertTask).start();
        }

    }
    protected void cancel() {
        isCanceled = true;
        if (ffmpeg != null) ffmpeg.stop();
        FfprobeRunner.stop();
    }
    protected void showVideoInfo() {

        //Aux class to type video info
        enum VideoType { VIDEO, AUDIO, SUBTITLE };
        record VideoInfo(String info, VideoType type, boolean isTitle){};

        ListView<VideoInfo> videoInfo = new ListView<>();
        videoInfo.setId("videoInfoList");
        videoInfo.setCellFactory(listView -> new ListCell<>(){
            @Override
            protected void updateItem(VideoInfo item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    HBox hbox = new HBox();
                    hbox.setSpacing(8);

                    Label label = new Label(item.info);
                    if (item.isTitle) {
                        label.setStyle("-fx-font-weight: bold;");
                        FontIcon icon = switch (item.type) {
                            case VIDEO      -> new FontIcon("mdmz-ondemand_video");
                            case AUDIO      -> new FontIcon("mdmz-music_video");
                            case SUBTITLE   -> new FontIcon("mdal-closed_caption");
                        };
                        hbox.getChildren().add(icon);
                    } else {
                        Region spacer = new Region();
                        spacer.setMinWidth(16);
                        hbox.getChildren().add(spacer);
                    }
                    hbox.getChildren().add(label);
                    setGraphic(hbox);
                }
            }
        });

        VideoStreamInfo video = mediaStreamInfo.getVideo();
        List<AudioStreamInfo> audioList = mediaStreamInfo.getAudio();
        List<SubtitleStreamInfo> subtitleList = mediaStreamInfo.getSubtitle();

        //Video
        videoInfo.getItems().add(new VideoInfo(
              "Video", VideoType.VIDEO, true
        ));
        videoInfo.getItems().add(new VideoInfo(
              video.getTitle(), VideoType.VIDEO, false
        ));
        videoInfo.getItems().add(new VideoInfo(
              video.formattedSize(), VideoType.VIDEO, false
        ));
        videoInfo.getItems().add(new VideoInfo(
              video.formattedDuration(), VideoType.VIDEO, false
        ));
        videoInfo.getItems().add(new VideoInfo(
              video.getWidth() + "x" + video.getHeight(), VideoType.VIDEO, false
        ));

        //Audio
        videoInfo.getItems().add(new VideoInfo(
                "Audio", VideoType.AUDIO, true
        ));
        audioList.forEach(audio -> {
            videoInfo.getItems().add(new VideoInfo(
                    audio.getLanguage() + " " + "[" + audio.getChannelLayout() + "]", VideoType.AUDIO, false
            ));
        });

        //Subtitle
        videoInfo.getItems().add(new VideoInfo(
                "Subtitle", VideoType.SUBTITLE, true
        ));
        subtitleList.forEach(subtitle -> {
            videoInfo.getItems().add(new VideoInfo(
                    subtitle.getLanguage() + " " + "[" + subtitle.getCodecName() + "]", VideoType.SUBTITLE, false
            ));
        });

        videoInfoContainer.getChildren().add(videoInfo);
        videoInfoContainer.setVisible(true);
        Animations.fadeIn(videoInfoContainer, Duration.seconds(1)).playFromStart();
    };
    protected void hideVideoInfo() {
        videoInfoContainer.getChildren().clear();
        videoInfoContainer.setVisible(false);
        Animations.fadeOut(videoInfoContainer, Duration.seconds(1)).playFromStart();
    }
    protected void createLoader(String message) {
        ProgressIndicator spinner = new ProgressIndicator();
        spinner.setProgress(-1);
        Label msg = new Label(message);
        VBox box = new VBox(10, spinner, msg);
        box.setAlignment(Pos.CENTER);
        loader.getChildren().add(box);

        loader.setManaged(true);
        loader.setVisible(true);
        Animations.fadeIn(loader, Duration.seconds(1)).playFromStart();
    }
    protected void hideLoader() {
        loader.setVisible(false);
        loader.setManaged(false);
        loader.getChildren().clear();
        Animations.fadeOut(loader, Duration.seconds(1)).playFromStart();
    }
    protected void createMsg(String message, String type) {
        Label msg = new Label(message);
        Region spacer = new Region();
        spacer.setMinWidth(4);
        FontIcon icon = new FontIcon("mdal-info");
        if (type.equals("error")){
            icon = new FontIcon("mdal-error");
            msg.getStyleClass().add("error");
            icon.getStyleClass().add("error");
        } else if (type.equals("success")){
            icon = new FontIcon("mdal-check");
            msg.getStyleClass().add("success");
            icon.getStyleClass().add("success");
        }
        msgContainer.getChildren().addAll(icon, spacer, msg);
        msgContainer.setManaged(true);
        msgContainer.setVisible(true);
        Animations.fadeIn(msgContainer, Duration.seconds(1)).playFromStart();
    };
    protected void hideMsg() {
        msgContainer.setVisible(false);
        msgContainer.setManaged(false);
        msgContainer.getChildren().clear();
        Animations.fadeOut(msgContainer, Duration.seconds(1)).playFromStart();
    }
    protected void createButton(String message, String type, boolean isHDR){
        if (type.equals("convert")){
            Button convertBtn = new Button(message, new FontIcon("mdal-flip_camera_android"));
            convertBtn.getStyleClass().addAll(Styles.ACCENT, Styles.TEXT_BOLDER, Styles.LARGE);
            convertBtn.setContentDisplay(ContentDisplay.LEFT);
            convertBtn.setOnAction(e -> convert());
            buttonsContainer.getChildren().addAll(createConfigBtns(isHDR), convertBtn);
            buttonsContainer.setManaged(true);
            buttonsContainer.setVisible(true);
            Animations.fadeIn(buttonsContainer, Duration.seconds(1)).playFromStart();
        } else {
            Button cancelBtn = new Button(message, new FontIcon("mdal-cancel"));
            cancelBtn.getStyleClass().addAll(Styles.DANGER, Styles.TEXT_BOLDER, Styles.LARGE);
            cancelBtn.setContentDisplay(ContentDisplay.LEFT);
            cancelBtn.setOnAction(e -> cancel());
            buttonsContainer.getChildren().add(cancelBtn);
            buttonsContainer.setManaged(true);
            buttonsContainer.setVisible(true);
            Animations.fadeIn(buttonsContainer, Duration.seconds(1)).playFromStart();
        }
    }
    protected void hideButton() {
        buttonsContainer.setVisible(false);
        buttonsContainer.setManaged(false);
        buttonsContainer.getChildren().clear();
        Animations.fadeOut(buttonsContainer, Duration.seconds(1)).playFromStart();
    }
    protected HBox createConfigBtns(boolean isHDR) {
        ComboBox<String> modeSelector = new ComboBox<>();
        modeSelector.setItems(FXCollections.observableList(modeList));
        modeSelector.getSelectionModel().selectFirst();
        modeSelector.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, newValue) -> this.selectedMode = newValue
        );

        ToggleSwitch preserveHDR = new ToggleSwitch("Preservar HDR");
        preserveHDR.setLabelPosition(HorizontalDirection.RIGHT);
        preserveHDR.selectedProperty().addListener(
                (observable, oldValue, newValue) -> this.preserveHDR = newValue
        );
        preserveHDR.setSelected(this.preserveHDR);
        if (!isHDR) preserveHDR.setDisable(true);

        HBox modeConfigHBox = new HBox(20, modeSelector, preserveHDR);
        modeConfigHBox.setAlignment(Pos.CENTER);
        return modeConfigHBox;
    }
    protected void createProgressBar(Task<?> task) {
        ProgressBar progressBar = new ProgressBar();
        progressBar.setPrefWidth(300);
        progressBar.progressProperty().bind(task.progressProperty());

        Label progressLabel = new Label();
        progressLabel.textProperty().bind(task.messageProperty());

        StackPane stackPane = new StackPane(progressBar, progressLabel);
        stackPane.setAlignment(Pos.CENTER);

        task.setOnSucceeded(e -> {
            progressBar.progressProperty().unbind();
            progressLabel.textProperty().unbind();
            hideProgressBar();
        });

        task.setOnFailed(e -> {
            progressBar.progressProperty().unbind();
            progressLabel.textProperty().unbind();
            hideProgressBar();
        });

        progressBarContainer.getChildren().add(stackPane);
        progressBarContainer.setManaged(true);
        progressBarContainer.setVisible(true);
        Animations.fadeIn(progressBarContainer, Duration.seconds(1)).playFromStart();
    }
    protected void hideProgressBar() {
        progressBarContainer.setVisible(false);
        progressBarContainer.setManaged(false);
        progressBarContainer.getChildren().clear();
    }
}