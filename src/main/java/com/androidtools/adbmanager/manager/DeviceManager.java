package com.androidtools.adbmanager.manager;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;

import java.util.List;

/**
 * 设备管理器
 * 管理 Android 设备的连接、断开和选择
 */
public class DeviceManager {
    
    private final AdbManager adbManager;
    private final ObservableList<DeviceInfo> devices;
    private final ObjectProperty<DeviceInfo> selectedDevice;
    private final StringProperty statusMessage;
    
    public DeviceManager(AdbManager adbManager) {
        this.adbManager = adbManager;
        this.devices = FXCollections.observableArrayList();
        this.selectedDevice = new SimpleObjectProperty<>();
        this.statusMessage = new SimpleStringProperty("就绪");
        
        // 监听设备选择变化
        selectedDevice.addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                adbManager.setSelectedDevice(newVal.getDeviceId());
                statusMessage.set("已选择设备：" + newVal.getDeviceName());
            } else {
                adbManager.setSelectedDevice(null);
                statusMessage.set("未选择设备");
            }
        });
    }
    
    /**
     * 扫描设备
     */
    public Task<List<DeviceInfo>> scanDevices() {
        Task<List<DeviceInfo>> task = new Task<>() {
            @Override
            protected List<DeviceInfo> call() {
                updateMessage("正在扫描设备...");
                
                List<String> deviceIds = adbManager.getDevices();
                List<DeviceInfo> newDevices = new java.util.ArrayList<>();
                
                for (String deviceId : deviceIds) {
                    if (isCancelled()) break;
                    
                    String model = adbManager.getDeviceModel(deviceId);
                    DeviceInfo info = new DeviceInfo(deviceId, model);
                    newDevices.add(info);
                    
                    updateMessage("发现设备：" + model);
                }
                
                // 在 JavaFX 线程中更新列表
                javafx.application.Platform.runLater(() -> {
                    devices.setAll(newDevices);
                });
                
                updateMessage("扫描完成，发现 " + newDevices.size() + " 个设备");
                return newDevices;
            }
        };
        
        task.setOnSucceeded(e -> {
            statusMessage.set("设备扫描完成");
        });
        
        task.setOnFailed(e -> {
            statusMessage.set("设备扫描失败：" + e.getSource().getException().getMessage());
        });
        
        new Thread(task).start();
        return task;
    }
    
    /**
     * 选择设备
     */
    public void selectDevice(DeviceInfo device) {
        selectedDevice.set(device);
    }
    
    /**
     * 断开设备
     */
    public void disconnectDevice(DeviceInfo device) {
        adbManager.disconnectDevice(device.getDeviceId());
        devices.remove(device);
        
        if (selectedDevice.get() == device) {
            selectedDevice.set(null);
        }
        
        statusMessage.set("已断开设备：" + device.getDeviceName());
    }
    
    /**
     * 获取所有设备列表
     */
    public ObservableList<DeviceInfo> getDevices() {
        return devices;
    }
    
    /**
     * 获取选中的设备
     */
    public DeviceInfo getSelectedDevice() {
        return selectedDevice.get();
    }
    
    /**
     * 选中设备属性
     */
    public ObjectProperty<DeviceInfo> selectedDeviceProperty() {
        return selectedDevice;
    }
    
    /**
     * 状态消息
     */
    public String getStatusMessage() {
        return statusMessage.get();
    }
    
    public StringProperty statusMessageProperty() {
        return statusMessage;
    }
    
    /**
     * 设备信息类
     */
    public static class DeviceInfo {
        private final String deviceId;
        private final String deviceName;
        
        public DeviceInfo(String deviceId, String deviceName) {
            this.deviceId = deviceId;
            this.deviceName = deviceName;
        }
        
        public String getDeviceId() {
            return deviceId;
        }
        
        public String getDeviceName() {
            return deviceName;
        }
        
        @Override
        public String toString() {
            return deviceName + " (" + deviceId + ")";
        }
    }
}
