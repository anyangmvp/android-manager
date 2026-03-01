package com.androidtools.adbmanager.ui;

import com.androidtools.adbmanager.manager.DeviceManager;
import com.androidtools.adbmanager.manager.DeviceManager.DeviceInfo;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

/**
 * 设备管理面板
 */
public class DevicePanel extends VBox {
    
    private final DeviceManager deviceManager;
    private ListView<DeviceInfo> deviceListView;
    private Button scanButton;
    private Button disconnectButton;
    private Button selectButton;
    
    public DevicePanel(DeviceManager deviceManager) {
        this.deviceManager = deviceManager;
        buildUI();
    }
    
    private void buildUI() {
        getStyleClass().add("device-panel");
        
        // 标题区域 - 左对齐，带下划线
        VBox titleSection = new VBox(8);
        Label titleLabel = new Label("设备管理");
        titleLabel.getStyleClass().add("title-label");
        
        Region separator = new Region();
        separator.setPrefHeight(3);
        separator.getStyleClass().add("title-separator");
        
        titleSection.getChildren().addAll(titleLabel, separator);
        
        // 扫描按钮 - 全宽
        scanButton = new Button("刷新设备");
        scanButton.setPrefWidth(Double.MAX_VALUE);
        scanButton.setPrefHeight(50);
        scanButton.getStyleClass().addAll("scan-button", "button-hover-scale");
        scanButton.setOnAction(e -> scanDevices());
        
        // 设备列表区域
        VBox listSection = new VBox(10);
        listSection.getStyleClass().add("list-section");
        
        Label listTitleLabel = new Label("设备列表");
        listTitleLabel.getStyleClass().add("list-title");
        
        deviceListView = new ListView<>();
        deviceListView.setItems(deviceManager.getDevices());
        deviceListView.setCellFactory(param -> new DeviceListCell());
        deviceListView.getStyleClass().add("device-list");
        deviceListView.setPrefHeight(350);
        VBox.setVgrow(deviceListView, Priority.ALWAYS);
        
        // 防止空列表时的选择问题
        deviceListView.getSelectionModel().clearSelection();
        
        listSection.getChildren().addAll(listTitleLabel, deviceListView);
        
        // 按钮区域 - 垂直排列，每个按钮全宽
        VBox buttonBox = new VBox(12);
        buttonBox.setPrefWidth(Double.MAX_VALUE);
        
        selectButton = new Button("选择设备");
        selectButton.setDisable(true);
        selectButton.setPrefWidth(Double.MAX_VALUE);
        selectButton.setPrefHeight(50);
        selectButton.getStyleClass().addAll("action-button", "select-button", "button-hover-scale");
        selectButton.setOnAction(e -> selectDevice());
        
        disconnectButton = new Button("断开设备");
        disconnectButton.setDisable(true);
        disconnectButton.setPrefWidth(Double.MAX_VALUE);
        disconnectButton.setPrefHeight(50);
        disconnectButton.getStyleClass().addAll("action-button", "disconnect-button", "button-hover-scale");
        disconnectButton.setOnAction(e -> disconnectDevice());
        
        buttonBox.getChildren().addAll(selectButton, disconnectButton);
        
        // 监听选择变化
        deviceListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            boolean selected = newVal != null;
            selectButton.setDisable(!selected);
            disconnectButton.setDisable(!selected);
        });
        
        getChildren().addAll(titleSection, scanButton, listSection, buttonBox);
    }
    
    private void scanDevices() {
        scanButton.setDisable(true);
        scanButton.setText("扫描中...");
        
        var task = deviceManager.scanDevices();
        task.setOnSucceeded(e -> {
            scanButton.setDisable(false);
            scanButton.setText("刷新设备");
        });
        
        task.setOnFailed(e -> {
            scanButton.setDisable(false);
            scanButton.setText("刷新设备");
        });
    }
    
    private void selectDevice() {
        DeviceInfo selected = deviceListView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            deviceManager.selectDevice(selected);
        }
    }
    
    private void disconnectDevice() {
        DeviceInfo selected = deviceListView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("确认断开");
            alert.setHeaderText(null);
            alert.setContentText("确定要断开设备 " + selected.getDeviceName() + " 吗？");
            alert.getDialogPane().setStyle("-fx-background-color: white;");
            
            alert.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    deviceManager.disconnectDevice(selected);
                }
            });
        }
    }
    
    /**
     * 设备列表单元格
     */
    private class DeviceListCell extends ListCell<DeviceInfo> {
        private final VBox container = new VBox(5);
        private final Label nameLabel = new Label();
        private final Label idLabel = new Label();
        private final CircleIndicator statusIndicator = new CircleIndicator();
        
        public DeviceListCell() {
            nameLabel.getStyleClass().add("name-label");
            idLabel.getStyleClass().add("id-label");
            
            HBox topBox = new HBox(10, statusIndicator, nameLabel);
            topBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            
            container.getStyleClass().add("device-list-cell");
            container.setPadding(new Insets(8, 12, 8, 12));
            container.getChildren().addAll(topBox, idLabel);
            
            // 监听选中状态变化，直接设置样式
            selectedProperty().addListener((obs, oldVal, newVal) -> {
                updateSelectionStyle(newVal);
            });
        }
        
        private void updateSelectionStyle(boolean isSelected) {
            if (isSelected) {
                container.setStyle("-fx-background-color: #edf2f7; -fx-background-radius: 8px;");
                nameLabel.setStyle("-fx-text-fill: #000000; -fx-font-weight: bold; -fx-font-size: 15px;");
                idLabel.setStyle("-fx-text-fill: #2d3748; -fx-font-size: 12px;");
            } else {
                container.setStyle("-fx-background-color: white; -fx-background-radius: 8px;");
                nameLabel.setStyle("-fx-text-fill: #2d3748; -fx-font-weight: bold; -fx-font-size: 14px;");
                idLabel.setStyle("-fx-text-fill: #718096; -fx-font-size: 12px;");
            }
        }
        
        @Override
        protected void updateItem(DeviceInfo item, boolean empty) {
            super.updateItem(item, empty);
            
            if (empty || item == null) {
                setGraphic(null);
            } else {
                nameLabel.setText(item.getDeviceName());
                idLabel.setText(item.getDeviceId());
                statusIndicator.setActive(true);
                updateSelectionStyle(isSelected());
                setGraphic(container);
            }
        }
    }
}
