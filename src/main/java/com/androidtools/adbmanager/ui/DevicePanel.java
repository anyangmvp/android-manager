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
        setSpacing(20);
        setPadding(new Insets(25, 25, 25, 25));
        setPrefWidth(300);
        setStyle("-fx-background-color: white; -fx-background-radius: 12px; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 2);");
        
        // 标题区域 - 左对齐，带下划线
        VBox titleSection = new VBox(8);
        Label titleLabel = new Label("设备管理");
        titleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: linear-gradient(from 0% 0% to 100% 0%, #667eea 0%, #764ba2 100%);");
        
        Region separator = new Region();
        separator.setPrefHeight(3);
        separator.setStyle("-fx-background-color: linear-gradient(from 0% 0% to 100% 0%, #667eea 0%, #764ba2 100%); -fx-background-radius: 2px;");
        
        titleSection.getChildren().addAll(titleLabel, separator);
        
        // 扫描按钮 - 全宽
        scanButton = new Button("刷新设备");
        scanButton.setPrefWidth(Double.MAX_VALUE);
        scanButton.setPrefHeight(50);
        String scanNormalStyle = "-fx-background-color: linear-gradient(from 0% 0% to 100% 0%, #667eea 0%, #764ba2 100%); -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px; -fx-background-radius: 10px; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(102,126,234,0.4), 6, 0, 0, 2);";
        String scanHoverStyle = "-fx-background-color: linear-gradient(from 0% 0% to 100% 0%, #5a6fd6 0%, #694190 100%); -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px; -fx-background-radius: 10px; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(102,126,234,0.5), 8, 0, 0, 3);";
        scanButton.setStyle(scanNormalStyle);
        scanButton.setOnMouseEntered(e -> scanButton.setStyle(scanHoverStyle));
        scanButton.setOnMouseExited(e -> scanButton.setStyle(scanNormalStyle));
        scanButton.setOnAction(e -> scanDevices());
        
        // 设备列表区域
        VBox listSection = new VBox(10);
        listSection.setStyle("-fx-background-color: #f7fafc; -fx-background-radius: 10px; -fx-padding: 15px;");
        
        Label listTitleLabel = new Label("设备列表");
        listTitleLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #718096;");
        
        deviceListView = new ListView<>();
        deviceListView.setItems(deviceManager.getDevices());
        deviceListView.setCellFactory(param -> new DeviceListCell());
        deviceListView.setStyle("-fx-background-color: transparent; -fx-border-color: transparent; -fx-background-radius: 8px;");
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
        String selectNormalStyle = "-fx-background-color: linear-gradient(from 0% 0% to 100% 0%, #48bb78 0%, #38a169 100%); -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px; -fx-background-radius: 10px; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(72,187,120,0.4), 6, 0, 0, 2);";
        String selectHoverStyle = "-fx-background-color: linear-gradient(from 0% 0% to 100% 0%, #41a86a 0%, #32915e 100%); -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px; -fx-background-radius: 10px; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(72,187,120,0.5), 8, 0, 0, 3);";
        selectButton.setStyle(selectNormalStyle);
        selectButton.setOnMouseEntered(e -> selectButton.setStyle(selectHoverStyle));
        selectButton.setOnMouseExited(e -> selectButton.setStyle(selectNormalStyle));
        selectButton.setOnAction(e -> selectDevice());
        
        disconnectButton = new Button("断开设备");
        disconnectButton.setDisable(true);
        disconnectButton.setPrefWidth(Double.MAX_VALUE);
        disconnectButton.setPrefHeight(50);
        String disconnectNormalStyle = "-fx-background-color: linear-gradient(from 0% 0% to 100% 0%, #f56565 0%, #e53e3e 100%); -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px; -fx-background-radius: 10px; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(245,101,101,0.4), 6, 0, 0, 2);";
        String disconnectHoverStyle = "-fx-background-color: linear-gradient(from 0% 0% to 100% 0%, #dd5a5a 0%, #d03737 100%); -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px; -fx-background-radius: 10px; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(245,101,101,0.5), 8, 0, 0, 3);";
        disconnectButton.setStyle(disconnectNormalStyle);
        disconnectButton.setOnMouseEntered(e -> disconnectButton.setStyle(disconnectHoverStyle));
        disconnectButton.setOnMouseExited(e -> disconnectButton.setStyle(disconnectNormalStyle));
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
            nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #2d3748;");
            idLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #718096;");
            
            HBox topBox = new HBox(10, statusIndicator, nameLabel);
            topBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            
            container.getChildren().addAll(topBox, idLabel);
            container.setPadding(new Insets(8, 12, 8, 12));
            container.setStyle("-fx-background-color: white; -fx-background-radius: 8px;");
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
                setGraphic(container);
            }
        }
    }
}
