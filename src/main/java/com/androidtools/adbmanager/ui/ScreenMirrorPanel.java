package com.androidtools.adbmanager.ui;

import com.androidtools.adbmanager.manager.AdbManager;
import com.androidtools.adbmanager.manager.DeviceManager;
import com.androidtools.adbmanager.manager.DeviceManager.DeviceInfo;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 屏幕投射控制面板
 * 使用 scrcpy 实现手机屏幕投射到电脑
 */
public class ScreenMirrorPanel extends VBox {
    
    private final DeviceManager deviceManager;
    private final AdbManager adbManager;
    private final ConsolePanel consolePanel;
    
    private ComboBox<String> resolutionCombo;
    private ComboBox<String> bitrateCombo;
    private ComboBox<String> fpsCombo;
    private CheckBox fullscreenCheckBox;
    private CheckBox stayAwakeCheckBox;
    private CheckBox turnScreenOffCheckBox;
    private Button startMirrorButton;
    private Button stopMirrorButton;
    private Label statusLabel;
    
    private Process scrcpyProcess;
    private ScheduledExecutorService executorService;
    
    public ScreenMirrorPanel(DeviceManager deviceManager, AdbManager adbManager, ConsolePanel consolePanel) {
        this.deviceManager = deviceManager;
        this.adbManager = adbManager;
        this.consolePanel = consolePanel;
        buildUI();
    }
    
    private void buildUI() {
        getStyleClass().add("screen-mirror-panel");
        setPadding(new Insets(15));
        
        Label titleLabel = new Label("屏幕投射控制");
        titleLabel.getStyleClass().add("title-label");
        
        VBox settingsBox = createSettingsSection();
        
        VBox optionsBox = createOptionsSection();
        
        HBox buttonBox = createButtonSection();
        
        statusLabel = new Label("未连接");
        statusLabel.getStyleClass().add("status-label");
        
        getChildren().addAll(titleLabel, settingsBox, optionsBox, buttonBox, statusLabel);
    }
    
    private VBox createSettingsSection() {
        VBox container = new VBox(12);
        container.setPadding(new Insets(10, 0, 10, 0));
        
        Label sectionLabel = new Label("投射设置");
        sectionLabel.getStyleClass().add("section-label");
        
        GridPane settingsGrid = new GridPane();
        settingsGrid.setHgap(15);
        settingsGrid.setVgap(10);
        
        Label resolutionLabel = new Label("分辨率：");
        resolutionLabel.getStyleClass().add("field-label");
        
        resolutionCombo = new ComboBox<>();
        resolutionCombo.getItems().addAll("原始分辨率", "1920x1080", "1280x720", "1024x576", "800x450");
        resolutionCombo.setValue("1920x1080");
        resolutionCombo.setPrefWidth(150);
        
        Label bitrateLabel = new Label("比特率：");
        bitrateLabel.getStyleClass().add("field-label");
        
        bitrateCombo = new ComboBox<>();
        bitrateCombo.getItems().addAll("8M", "4M", "2M", "1M");
        bitrateCombo.setValue("4M");
        bitrateCombo.setPrefWidth(150);
        
        Label fpsLabel = new Label("帧率：");
        fpsLabel.getStyleClass().add("field-label");
        
        fpsCombo = new ComboBox<>();
        fpsCombo.getItems().addAll("60", "30", "15");
        fpsCombo.setValue("60");
        fpsCombo.setPrefWidth(150);
        
        settingsGrid.add(resolutionLabel, 0, 0);
        settingsGrid.add(resolutionCombo, 1, 0);
        settingsGrid.add(bitrateLabel, 0, 1);
        settingsGrid.add(bitrateCombo, 1, 1);
        settingsGrid.add(fpsLabel, 0, 2);
        settingsGrid.add(fpsCombo, 1, 2);
        
        container.getChildren().addAll(sectionLabel, settingsGrid);
        return container;
    }
    
    private VBox createOptionsSection() {
        VBox container = new VBox(12);
        container.setPadding(new Insets(10, 0, 10, 0));
        
        Label sectionLabel = new Label("选项");
        sectionLabel.getStyleClass().add("section-label");
        
        fullscreenCheckBox = new CheckBox("启动时全屏");
        fullscreenCheckBox.getStyleClass().add("check-box");
        
        stayAwakeCheckBox = new CheckBox("保持屏幕常亮");
        stayAwakeCheckBox.setSelected(true);
        stayAwakeCheckBox.getStyleClass().add("check-box");
        
        turnScreenOffCheckBox = new CheckBox("投射时关闭手机屏幕");
        turnScreenOffCheckBox.getStyleClass().add("check-box");
        
        container.getChildren().addAll(sectionLabel, fullscreenCheckBox, stayAwakeCheckBox, turnScreenOffCheckBox);
        return container;
    }
    
    private HBox createButtonSection() {
        HBox container = new HBox(15);
        container.setPadding(new Insets(15, 0, 10, 0));
        
        startMirrorButton = new Button("开始投射");
        startMirrorButton.setPrefHeight(45);
        startMirrorButton.setPrefWidth(150);
        startMirrorButton.getStyleClass().add("start-button");
        startMirrorButton.setOnAction(e -> startMirror());
        
        stopMirrorButton = new Button("停止投射");
        stopMirrorButton.setPrefHeight(45);
        stopMirrorButton.setPrefWidth(150);
        stopMirrorButton.getStyleClass().add("stop-button");
        stopMirrorButton.setDisable(true);
        stopMirrorButton.setOnAction(e -> stopMirror());
        
        container.getChildren().addAll(startMirrorButton, stopMirrorButton);
        return container;
    }
    
    private void startMirror() {
        DeviceInfo selectedDevice = deviceManager.getSelectedDevice();
        if (selectedDevice == null) {
            showError("请先选择设备");
            return;
        }
        String deviceId = selectedDevice.getDeviceId();
        
        if (!checkScrcpyInstalled()) {
            showError("scrcpy 未安装，请先安装 scrcpy\n下载地址: https://github.com/Genymobile/scrcpy");
            return;
        }
        
        consolePanel.appendLog("[屏幕投射] 正在启动 scrcpy...");
        statusLabel.setText("正在连接...");
        
        ArrayList<String> command = new ArrayList<>();
        command.add("scrcpy");
        command.add("-s");
        command.add(deviceId);
        
        String resolution = resolutionCombo.getValue();
        if (!"原始分辨率".equals(resolution)) {
            command.add("-m");
            command.add(resolution);
        }
        
        String bitrate = bitrateCombo.getValue();
        command.add("-b");
        command.add(bitrate);
        
        String fps = fpsCombo.getValue();
        command.add("--max-fps");
        command.add(fps);
        
        if (fullscreenCheckBox.isSelected()) {
            command.add("-f");
        }
        
        if (stayAwakeCheckBox.isSelected()) {
            command.add("-w");
        }
        
        if (turnScreenOffCheckBox.isSelected()) {
            command.add("--turn-screen-off");
        }
        
        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            scrcpyProcess = pb.start();
            
            executorService = Executors.newSingleThreadScheduledExecutor();
            executorService.scheduleAtFixedRate(() -> {
                if (scrcpyProcess != null && !scrcpyProcess.isAlive()) {
                    javafx.application.Platform.runLater(this::stopMirror);
                }
            }, 1, 1, TimeUnit.SECONDS);
            
            startMirrorButton.setDisable(true);
            stopMirrorButton.setDisable(false);
            statusLabel.setText("投射中: " + deviceId);
            consolePanel.appendLog("[屏幕投射] 已启动，设备: " + deviceId);
            
        } catch (IOException e) {
            showError("启动失败: " + e.getMessage());
            consolePanel.appendLog("[屏幕投射] 启动失败: " + e.getMessage());
        }
    }
    
    private void stopMirror() {
        if (scrcpyProcess != null && scrcpyProcess.isAlive()) {
            scrcpyProcess.destroy();
            consolePanel.appendLog("[屏幕投射] 已停止");
        }
        
        if (executorService != null) {
            executorService.shutdownNow();
            executorService = null;
        }
        
        startMirrorButton.setDisable(false);
        stopMirrorButton.setDisable(true);
        statusLabel.setText("未连接");
    }
    
    private boolean checkScrcpyInstalled() {
        try {
            Process pb = new ProcessBuilder("scrcpy", "--version").start();
            int exitCode = pb.waitFor();
            return exitCode == 0;
        } catch (Exception e) {
            return false;
        }
    }
    
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("错误");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    public void cleanup() {
        stopMirror();
    }
}
