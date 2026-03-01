package com.androidtools.adbmanager.ui;

import com.androidtools.adbmanager.manager.AdbManager;
import com.androidtools.adbmanager.manager.DeviceManager;
import com.androidtools.adbmanager.manager.DeviceManager.DeviceInfo;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxListCell;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.Properties;
import java.util.stream.Collectors;

/**
 * 文件管理传输面板
 * 支持推送/拉取/删除/新建/重命名/排序/过滤
 */
public class FileTransferPanel extends VBox {
    
    private final DeviceManager deviceManager;
    private final AdbManager adbManager;
    private final ConsolePanel consolePanel;
    
    private String currentPhonePath = "/sdcard/";
    private static final String KEY_FILE_PC_DIR = "file.pc.dir";
    
    private String currentPcPath = System.getProperty("user.home");
    private List<FileInfo> phoneFiles = new ArrayList<>();
    private List<FileInfo> pcFiles = new ArrayList<>();
    private List<FileInfo> filteredPhoneFiles = new ArrayList<>();
    private List<FileInfo> filteredPcFiles = new ArrayList<>();
    
    private ListView<FileInfo> phoneFileList;
    private ListView<FileInfo> pcFileList;
    private Label phonePathLabel;
    private Label pcPathLabel;
    private TextField filterField;
    private ComboBox<String> sortCombo;
    private CheckBox showHiddenCheckBox;
    
    private Button pushButton;
    private Button pullButton;
    private Button deleteButton;
    private Button newFolderButton;
    private Button renameButton;
    private Button refreshButton;
    private Label statusLabel;
    
    public FileTransferPanel(DeviceManager deviceManager, AdbManager adbManager, ConsolePanel consolePanel) {
        this.deviceManager = deviceManager;
        this.adbManager = adbManager;
        this.consolePanel = consolePanel;
        buildUI();
    }
    
    private void buildUI() {
        getStyleClass().add("file-transfer-panel");
        setPadding(new Insets(15));
        
        Label titleLabel = new Label("文件管理");
        titleLabel.getStyleClass().add("title-label");
        
        HBox filterBox = createFilterRow();
        HBox contentArea = createContentArea();
        
        HBox buttonBox = createButtonSection();
        
        statusLabel = new Label("就绪");
        statusLabel.getStyleClass().add("status-label");
        
        getChildren().addAll(titleLabel, filterBox, contentArea, buttonBox, statusLabel);
        
        loadSavedPcDir();
    }
    
    private void loadSavedPcDir() {
        Properties props = loadProperties();
        String savedDir = props.getProperty(KEY_FILE_PC_DIR);
        if (savedDir != null && !savedDir.isEmpty() && new File(savedDir).exists()) {
            currentPcPath = savedDir;
        }
    }
    
    private void savePcDir(String dir) {
        Properties props = loadProperties();
        props.setProperty(KEY_FILE_PC_DIR, dir);
        saveProperties(props);
    }
    
    private Properties loadProperties() {
        Properties props = new Properties();
        try (FileInputStream in = new FileInputStream("config.properties")) {
            props.load(in);
        } catch (Exception ignored) {}
        return props;
    }
    
    private void saveProperties(Properties props) {
        try (FileOutputStream out = new FileOutputStream("config.properties")) {
            props.store(out, "ADB Manager Config");
        } catch (Exception ignored) {}
    }
    
    private HBox createFilterRow() {
        HBox filterBox = new HBox(15);
        filterBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        
        Label filterLabel = new Label("过滤：");
        filterLabel.getStyleClass().add("field-label");
        
        filterField = new TextField();
        filterField.setPromptText("输入文件名过滤");
        filterField.setPrefWidth(200);
        filterField.setOnKeyReleased(e -> applyFilter());
        
        Label sortLabel = new Label("排序：");
        sortLabel.getStyleClass().add("field-label");
        
        sortCombo = new ComboBox<>();
        sortCombo.getItems().addAll("名称↑", "名称↓", "大小↑", "大小↓", "修改时间↑", "修改时间↓");
        sortCombo.setValue("名称↑");
        sortCombo.setPrefWidth(120);
        sortCombo.setOnAction(e -> applySort());
        
        showHiddenCheckBox = new CheckBox("显示隐藏文件");
        showHiddenCheckBox.setOnAction(e -> {
            refreshPhoneFileList();
            refreshPcFileList();
        });
        
        filterBox.getChildren().addAll(filterLabel, filterField, sortLabel, sortCombo, showHiddenCheckBox);
        return filterBox;
    }
    
    private HBox createContentArea() {
        HBox container = new HBox(15);
        HBox.setHgrow(container, Priority.ALWAYS);
        
        VBox phoneBox = createPhoneSection();
        VBox pcBox = createPcSection();
        
        HBox.setHgrow(phoneBox, Priority.ALWAYS);
        HBox.setHgrow(pcBox, Priority.ALWAYS);
        
        container.getChildren().addAll(phoneBox, pcBox);
        return container;
    }
    
    private VBox createPhoneSection() {
        VBox container = new VBox(8);
        container.setPadding(new Insets(10));
        container.getStyleClass().add("file-section-box");
        
        Label title = new Label("手机存储");
        title.getStyleClass().add("section-title");
        
        HBox pathBox = new HBox(10);
        phonePathLabel = new Label(currentPhonePath);
        phonePathLabel.getStyleClass().add("path-label");
        
        Button goUpButton = new Button("↑");
        goUpButton.setPrefWidth(30);
        goUpButton.setOnAction(e -> goUpPhoneDirectory());
        
        Button quickPathButton = new Button("快捷目录");
        quickPathButton.setOnAction(e -> showQuickPathMenu(quickPathButton));
        
        pathBox.getChildren().addAll(goUpButton, phonePathLabel, quickPathButton);
        
        phoneFileList = new ListView<>();
        phoneFileList.setCellFactory(param -> new ListCell<FileInfo>() {
            @Override
            protected void updateItem(FileInfo item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(item.getDisplayName());
                    String icon = item.isDirectory() ? "📁" : "📄";
                    setGraphic(new Label(icon));
                }
            }
        });
        
        phoneFileList.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                FileInfo selected = phoneFileList.getSelectionModel().getSelectedItem();
                if (selected != null && selected.isDirectory()) {
                    navigateToPhoneDirectory(selected.getName());
                }
            }
        });
        
        VBox.setVgrow(phoneFileList, Priority.ALWAYS);
        
        container.getChildren().addAll(title, pathBox, phoneFileList);
        return container;
    }
    
    private VBox createPcSection() {
        VBox container = new VBox(8);
        container.setPadding(new Insets(10));
        container.getStyleClass().add("file-section-box");
        
        Label title = new Label("电脑存储");
        title.getStyleClass().add("section-title");
        
        HBox pathBox = new HBox(10);
        pcPathLabel = new Label(currentPcPath);
        pcPathLabel.getStyleClass().add("path-label");
        
        Button goUpButton = new Button("↑");
        goUpButton.setPrefWidth(30);
        goUpButton.setOnAction(e -> goUpPcDirectory());
        
        Button browseButton = new Button("浏览...");
        browseButton.setOnAction(e -> browsePcDirectory());
        
        pathBox.getChildren().addAll(goUpButton, pcPathLabel, browseButton);
        
        pcFileList = new ListView<>();
        pcFileList.setCellFactory(param -> new ListCell<FileInfo>() {
            @Override
            protected void updateItem(FileInfo item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(item.getDisplayName());
                    String icon = item.isDirectory() ? "📁" : "📄";
                    setGraphic(new Label(icon));
                }
            }
        });
        
        pcFileList.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                FileInfo selected = pcFileList.getSelectionModel().getSelectedItem();
                if (selected != null && selected.isDirectory()) {
                    navigateToPcDirectory(selected.getName());
                }
            }
        });
        
        VBox.setVgrow(pcFileList, Priority.ALWAYS);
        
        container.getChildren().addAll(title, pathBox, pcFileList);
        return container;
    }
    
    private HBox createButtonSection() {
        HBox container = new HBox(15);
        container.setPadding(new Insets(15, 0, 10, 0));
        
        pushButton = new Button("← 推送");
        pushButton.setPrefHeight(40);
        pushButton.setPrefWidth(100);
        pushButton.getStyleClass().add("push-button");
        pushButton.setOnAction(e -> pushFiles());
        
        pullButton = new Button("拉取 →");
        pullButton.setPrefHeight(40);
        pullButton.setPrefWidth(100);
        pullButton.getStyleClass().add("pull-button");
        pullButton.setOnAction(e -> pullFiles());
        
        refreshButton = new Button("刷新");
        refreshButton.setPrefHeight(40);
        refreshButton.setPrefWidth(80);
        refreshButton.getStyleClass().add("refresh-button");
        refreshButton.setOnAction(e -> refreshAll());
        
        deleteButton = new Button("删除");
        deleteButton.setPrefHeight(40);
        deleteButton.setPrefWidth(80);
        deleteButton.getStyleClass().add("delete-button");
        deleteButton.setOnAction(e -> deleteSelected());
        
        newFolderButton = new Button("新建文件夹");
        newFolderButton.setPrefHeight(40);
        newFolderButton.setPrefWidth(100);
        newFolderButton.getStyleClass().add("new-folder-button");
        newFolderButton.setOnAction(e -> createNewFolder());
        
        renameButton = new Button("重命名");
        renameButton.setPrefHeight(40);
        renameButton.setPrefWidth(80);
        renameButton.getStyleClass().add("rename-button");
        renameButton.setOnAction(e -> renameSelected());
        
        container.getChildren().addAll(pushButton, pullButton, refreshButton, deleteButton, newFolderButton, renameButton);
        return container;
    }
    
    private void refreshPhoneFileList() {
        DeviceInfo selectedDevice = deviceManager.getSelectedDevice();
        if (selectedDevice == null) {
            showError("请先选择设备");
            return;
        }
        String deviceId = selectedDevice.getDeviceId();
        
        consolePanel.appendLog("[文件管理] 正在刷新手机目录: " + currentPhonePath);
        
        new Thread(() -> {
            phoneFiles.clear();
            List<String> output = adbManager.executeAdbCommand("-s", deviceId, "shell", "ls", "-la", currentPhonePath);
            
            for (String line : output) {
                if (line.startsWith("d") || line.startsWith("-")) {
                    try {
                        String[] parts = line.split("\\s+");
                        if (parts.length >= 8) {
                            boolean isDir = line.startsWith("d");
                            String name = parts[parts.length - 1];
                            
                            if (!showHiddenCheckBox.isSelected() && name.startsWith(".")) {
                                continue;
                            }
                            
                            if (".".equals(name) || "..".equals(name)) {
                                continue;
                            }
                            
                            long size = 0;
                            try {
                                size = Long.parseLong(parts[4]);
                            } catch (Exception ignored) {}
                            
                            String date = parts[5] + " " + parts[6];
                            
                            FileInfo info = new FileInfo(name, isDir, size, date, currentPhonePath + name);
                            phoneFiles.add(info);
                        }
                    } catch (Exception ignored) {}
                }
            }
            
            applyFilterAndSort();
            
            Platform.runLater(() -> {
                phonePathLabel.setText(currentPhonePath);
                statusLabel.setText("手机: " + phoneFiles.size() + " 个文件");
            });
        }).start();
    }
    
    private void refreshPcFileList() {
        new Thread(() -> {
            pcFiles.clear();
            File dir = new File(currentPcPath);
            
            if (dir.exists() && dir.isDirectory()) {
                File[] files = dir.listFiles();
                if (files != null) {
                    for (File file : files) {
                        if (!showHiddenCheckBox.isSelected() && file.getName().startsWith(".")) {
                            continue;
                        }
                        
                        String date;
                        try {
                            date = LocalDateTime.ofInstant(
                                java.nio.file.Files.getLastModifiedTime(file.toPath()).toInstant(),
                                java.time.ZoneId.systemDefault()
                            ).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
                        } catch (IOException e) {
                            date = "未知";
                        }
                        
                        FileInfo info = new FileInfo(
                            file.getName(),
                            file.isDirectory(),
                            file.length(),
                            date,
                            file.getAbsolutePath()
                        );
                        pcFiles.add(info);
                    }
                }
            }
            
            applyFilterAndSort();
            
            Platform.runLater(() -> {
                pcPathLabel.setText(currentPcPath);
                statusLabel.setText("电脑: " + pcFiles.size() + " 个文件");
            });
        }).start();
    }
    
    private void applyFilter() {
        applyFilterAndSort();
    }
    
    private void applySort() {
        applyFilterAndSort();
    }
    
    private void applyFilterAndSort() {
        String filterText = filterField.getText().toLowerCase();
        String sortType = sortCombo.getValue();
        
        List<FileInfo> filteredPhone = phoneFiles.stream()
            .filter(f -> filterText.isEmpty() || f.getName().toLowerCase().contains(filterText))
            .collect(Collectors.toList());
        
        filteredPhoneFiles = sortFileList(filteredPhone, sortType);
        
        filteredPcFiles = pcFiles;
        
        final List<FileInfo> finalFilteredPhone = filteredPhoneFiles;
        final List<FileInfo> finalFilteredPc = filteredPcFiles;
        Platform.runLater(() -> {
            phoneFileList.getItems().setAll(finalFilteredPhone);
            pcFileList.getItems().setAll(finalFilteredPc);
        });
    }
    
    private List<FileInfo> sortFileList(List<FileInfo> list, String sortType) {
        switch (sortType) {
            case "名称↑":
                return list.stream()
                    .sorted(Comparator.comparing(FileInfo::isDirectory).reversed()
                    .thenComparing(FileInfo::getName))
                    .collect(Collectors.toList());
            case "名称↓":
                return list.stream()
                    .sorted(Comparator.comparing(FileInfo::isDirectory).reversed()
                    .thenComparing(FileInfo::getName).reversed())
                    .collect(Collectors.toList());
            case "大小↑":
                return list.stream()
                    .sorted(Comparator.comparing(FileInfo::isDirectory).reversed()
                    .thenComparingLong(FileInfo::getSize))
                    .collect(Collectors.toList());
            case "大小↓":
                return list.stream()
                    .sorted(Comparator.comparing(FileInfo::isDirectory).reversed()
                    .thenComparingLong(FileInfo::getSize).reversed())
                    .collect(Collectors.toList());
            case "修改时间↑":
                return list.stream()
                    .sorted(Comparator.comparing(FileInfo::isDirectory).reversed()
                    .thenComparing(FileInfo::getDate))
                    .collect(Collectors.toList());
            case "修改时间↓":
                return list.stream()
                    .sorted(Comparator.comparing(FileInfo::isDirectory).reversed()
                    .thenComparing(FileInfo::getDate).reversed())
                    .collect(Collectors.toList());
            default:
                return list;
        }
    }
    
    private void navigateToPhoneDirectory(String dirName) {
        if (currentPhonePath.endsWith("/")) {
            currentPhonePath += dirName + "/";
        } else {
            currentPhonePath += "/" + dirName + "/";
        }
        refreshPhoneFileList();
    }
    
    private void goUpPhoneDirectory() {
        if (!currentPhonePath.equals("/sdcard/") && !currentPhonePath.equals("/")) {
            String parent = currentPhonePath.substring(0, currentPhonePath.length() - 1);
            int lastSlash = parent.lastIndexOf('/');
            if (lastSlash > 0) {
                currentPhonePath = parent.substring(0, lastSlash + 1);
            } else {
                currentPhonePath = "/sdcard/";
            }
            if (!currentPhonePath.endsWith("/")) {
                currentPhonePath += "/";
            }
            refreshPhoneFileList();
        }
    }
    
    private void showQuickPathMenu(Button button) {
        ContextMenu menu = new ContextMenu();
        menu.getItems().add(createMenuItem("/sdcard/", "sdcard"));
        menu.getItems().add(createMenuItem("/sdcard/Download/", "下载"));
        menu.getItems().add(createMenuItem("/sdcard/Pictures/", "图片"));
        menu.getItems().add(createMenuItem("/sdcard/DCIM/", "相机"));
        menu.getItems().add(createMenuItem("/sdcard/Android/data/", "应用数据"));
        menu.getItems().add(createMenuItem("/sdcard/Documents/", "文档"));
        menu.show(button, javafx.geometry.Side.BOTTOM, 0, 0);
    }
    
    private MenuItem createMenuItem(String path, String name) {
        MenuItem item = new MenuItem(name + " (" + path + ")");
        item.setOnAction(e -> {
            currentPhonePath = path;
            refreshPhoneFileList();
        });
        return item;
    }
    
    private void navigateToPcDirectory(String dirName) {
        if (currentPcPath.endsWith("\\") || currentPcPath.endsWith("/")) {
            currentPcPath += dirName;
        } else {
            currentPcPath += File.separator + dirName;
        }
        savePcDir(currentPcPath);
        refreshPcFileList();
    }
    
    private void goUpPcDirectory() {
        File current = new File(currentPcPath);
        File parent = current.getParentFile();
        if (parent != null) {
            currentPcPath = parent.getAbsolutePath();
            savePcDir(currentPcPath);
            refreshPcFileList();
        }
    }
    
    private void browsePcDirectory() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setInitialDirectory(new File(currentPcPath));
        File selected = chooser.showDialog(getScene().getWindow());
        if (selected != null) {
            currentPcPath = selected.getAbsolutePath();
            savePcDir(currentPcPath);
            refreshPcFileList();
        }
    }
    
    private void pushFiles() {
        FileInfo selected = pcFileList.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("请先在电脑端选择文件");
            return;
        }
        
        DeviceInfo selectedDevice = deviceManager.getSelectedDevice();
        if (selectedDevice == null) {
            showError("请先选择设备");
            return;
        }
        String deviceId = selectedDevice.getDeviceId();
        
        String sourcePath = selected.getFullPath();
        String destPath = currentPhonePath + selected.getName();
        
        consolePanel.appendLog("[文件管理] 推送: " + sourcePath + " -> " + destPath);
        
        new Thread(() -> {
            List<String> result;
            if (selected.isDirectory()) {
                result = adbManager.executeAdbCommand("-s", deviceId, "push", sourcePath, destPath);
            } else {
                result = adbManager.executeAdbCommand("-s", deviceId, "push", sourcePath, destPath);
            }
            
            final String finalDest = destPath;
            Platform.runLater(() -> {
                consolePanel.appendLog("[文件管理] 推送完成");
                statusLabel.setText("已推送到: " + finalDest);
                refreshPhoneFileList();
            });
        }).start();
    }
    
    private void pullFiles() {
        FileInfo selected = phoneFileList.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("请先在手机端选择文件");
            return;
        }
        
        DeviceInfo selectedDevice = deviceManager.getSelectedDevice();
        if (selectedDevice == null) {
            showError("请先选择设备");
            return;
        }
        String deviceId = selectedDevice.getDeviceId();
        
        String sourcePath = selected.getFullPath();
        String destPath = currentPcPath + File.separator + selected.getName();
        
        consolePanel.appendLog("[文件管理] 拉取: " + sourcePath + " -> " + destPath);
        
        new Thread(() -> {
            List<String> result = adbManager.executeAdbCommand("-s", deviceId, "pull", sourcePath, destPath);
            
            Platform.runLater(() -> {
                consolePanel.appendLog("[文件管理] 拉取完成");
                statusLabel.setText("已拉取到: " + destPath);
                refreshPcFileList();
            });
        }).start();
    }
    
    private void deleteSelected() {
        FileInfo selectedPhone = phoneFileList.getSelectionModel().getSelectedItem();
        
        if (selectedPhone == null) {
            showError("请先选择要删除的文件或文件夹");
            return;
        }
        
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("确认删除");
        confirm.setHeaderText(null);
        confirm.setContentText("确定要删除 " + selectedPhone.getName() + " 吗？此操作不可恢复！");
        
        if (confirm.showAndWait().get() != ButtonType.OK) {
            return;
        }
        
        DeviceInfo selectedDevice = deviceManager.getSelectedDevice();
        if (selectedDevice == null) {
            showError("请先选择设备");
            return;
        }
        String deviceId = selectedDevice.getDeviceId();
        
        consolePanel.appendLog("[文件管理] 删除: " + selectedPhone.getFullPath());
        
        new Thread(() -> {
            List<String> result;
            if (selectedPhone.isDirectory()) {
                result = adbManager.executeAdbCommand("-s", deviceId, "shell", "rm", "-rf", selectedPhone.getFullPath());
            } else {
                result = adbManager.executeAdbCommand("-s", deviceId, "shell", "rm", selectedPhone.getFullPath());
            }
            
            Platform.runLater(() -> {
                consolePanel.appendLog("[文件管理] 删除完成");
                statusLabel.setText("已删除: " + selectedPhone.getName());
                refreshPhoneFileList();
            });
        }).start();
    }
    
    private void createNewFolder() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("新建文件夹");
        dialog.setHeaderText(null);
        dialog.setContentText("请输入文件夹名称：");
        
        Optional<String> result = dialog.showAndWait();
        if (result.isPresent() && !result.get().trim().isEmpty()) {
            String folderName = result.get().trim();
            DeviceInfo selectedDevice = deviceManager.getSelectedDevice();
            if (selectedDevice == null) {
                showError("请先选择设备");
                return;
            }
            String deviceId = selectedDevice.getDeviceId();
            
            String newPath = currentPhonePath + folderName;
            consolePanel.appendLog("[文件管理] 创建文件夹: " + newPath);
            
            new Thread(() -> {
                List<String> resultCmd = adbManager.executeAdbCommand("-s", deviceId, "shell", "mkdir", newPath);
                
                Platform.runLater(() -> {
                    consolePanel.appendLog("[文件管理] 创建完成");
                    statusLabel.setText("已创建文件夹: " + folderName);
                    refreshPhoneFileList();
                });
            }).start();
        }
    }
    
    private void renameSelected() {
        FileInfo selected = phoneFileList.getSelectionModel().getSelectedItem();
        
        if (selected == null) {
            showError("请先选择要重命名的文件或文件夹");
            return;
        }
        
        TextInputDialog dialog = new TextInputDialog(selected.getName());
        dialog.setTitle("重命名");
        dialog.setHeaderText(null);
        dialog.setContentText("请输入新名称：");
        
        Optional<String> result = dialog.showAndWait();
        if (result.isPresent() && !result.get().trim().isEmpty() && !result.get().equals(selected.getName())) {
            String newName = result.get().trim();
            DeviceInfo selectedDevice = deviceManager.getSelectedDevice();
            if (selectedDevice == null) {
                showError("请先选择设备");
                return;
            }
            String deviceId = selectedDevice.getDeviceId();
            
            String oldPath = selected.getFullPath();
            String parentPath = currentPhonePath;
            String newPath = parentPath + newName;
            
            consolePanel.appendLog("[文件管理] 重命名: " + oldPath + " -> " + newPath);
            
            new Thread(() -> {
                List<String> resultCmd = adbManager.executeAdbCommand("-s", deviceId, "shell", "mv", oldPath, newPath);
                
                Platform.runLater(() -> {
                    consolePanel.appendLog("[文件管理] 重命名完成");
                    statusLabel.setText("已重命名为: " + newName);
                    refreshPhoneFileList();
                });
            }).start();
        }
    }
    
    private void refreshAll() {
        refreshPhoneFileList();
        refreshPcFileList();
    }
    
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("错误");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    public void refresh() {
        refreshPhoneFileList();
        refreshPcFileList();
    }
    
    /**
     * 文件信息类
     */
    public static class FileInfo {
        private String name;
        private boolean isDirectory;
        private long size;
        private String date;
        private String fullPath;
        
        public FileInfo(String name, boolean isDirectory, long size, String date, String fullPath) {
            this.name = name;
            this.isDirectory = isDirectory;
            this.size = size;
            this.date = date;
            this.fullPath = fullPath;
        }
        
        public String getName() { return name; }
        public boolean isDirectory() { return isDirectory; }
        public long getSize() { return size; }
        public String getDate() { return date; }
        public String getFullPath() { return fullPath; }
        
        public String getDisplayName() {
            if (isDirectory) {
                return name + "/";
            } else {
                return name + "  (" + formatSize(size) + ")";
            }
        }
        
        private String formatSize(long size) {
            if (size < 1024) return size + " B";
            if (size < 1024 * 1024) return String.format("%.1f KB", size / 1024.0);
            if (size < 1024 * 1024 * 1024) return String.format("%.1f MB", size / (1024.0 * 1024));
            return String.format("%.1f GB", size / (1024.0 * 1024 * 1024));
        }
    }
}
