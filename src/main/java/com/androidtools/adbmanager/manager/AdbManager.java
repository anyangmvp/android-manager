package com.androidtools.adbmanager.manager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * ADB 命令管理器
 * 负责执行所有 ADB 相关命令
 */
public class AdbManager {
    
    private Process process;
    private String selectedDevice;
    
    /**
     * 执行 ADB 命令并返回输出
     */
    public List<String> executeAdbCommand(String... args) {
        List<String> result = new ArrayList<>();
        try {
            List<String> command = new ArrayList<>();
            command.add("adb");
            
            // 如果选择了设备，添加 -s 参数
            if (selectedDevice != null && !selectedDevice.isEmpty()) {
                command.add("-s");
                command.add(selectedDevice);
            }
            
            for (String arg : args) {
                command.add(arg);
            }
            
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            process = pb.start();
            
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    result.add(line);
                }
            }
            
            process.waitFor();
        } catch (Exception e) {
            result.add("错误：" + e.getMessage());
        }
        return result;
    }
    
    /**
     * 异步执行 ADB 命令
     */
    public CompletableFuture<List<String>> executeAdbCommandAsync(String... args) {
        return CompletableFuture.supplyAsync(() -> executeAdbCommand(args));
    }
    
    /**
     * 执行 ADB 命令并带进度回调
     */
    public void executeAdbCommandWithCallback(Consumer<String> outputCallback, String... args) {
        try {
            List<String> command = new ArrayList<>();
            command.add("adb");
            
            if (selectedDevice != null && !selectedDevice.isEmpty()) {
                command.add("-s");
                command.add(selectedDevice);
            }
            
            for (String arg : args) {
                command.add(arg);
            }
            
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            process = pb.start();
            
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (outputCallback != null) {
                        outputCallback.accept(line);
                    }
                }
            }
            
            process.waitFor();
        } catch (Exception e) {
            if (outputCallback != null) {
                outputCallback.accept("错误：" + e.getMessage());
            }
        }
    }
    
    /**
     * 获取设备列表
     */
    public List<String> getDevices() {
        List<String> devices = new ArrayList<>();
        List<String> output = executeAdbCommand("devices");
        
        for (String line : output) {
            if (line.contains("\tdevice") && !line.startsWith("List")) {
                String[] parts = line.split("\t");
                if (parts.length >= 1) {
                    devices.add(parts[0]);
                }
            }
        }
        return devices;
    }
    
    /**
     * 获取设备详细信息
     */
    public String getDeviceModel(String deviceId) {
        List<String> output = executeAdbCommand("-s", deviceId, "shell", "getprop", "ro.product.model");
        return output.isEmpty() ? "未知设备" : output.get(0).trim();
    }
    
    /**
     * 设置选中的设备
     */
    public void setSelectedDevice(String device) {
        this.selectedDevice = device;
    }
    
    /**
     * 获取当前选中的设备
     */
    public String getSelectedDevice() {
        return selectedDevice;
    }
    
    /**
     * 断开设备连接
     */
    public void disconnectDevice(String deviceId) {
        executeAdbCommand("disconnect", deviceId);
    }
    
    /**
     * 安装 APK
     */
    public boolean installApk(String apkPath) {
        List<String> output = executeAdbCommand("install", "-r", apkPath);
        for (String line : output) {
            if (line.contains("Success")) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 卸载应用
     */
    public boolean uninstallApp(String packageName) {
        List<String> output = executeAdbCommand("uninstall", packageName);
        for (String line : output) {
            if (line.contains("Success")) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 截屏
     */
    public boolean takeScreenshot(String outputPath) {
        // 先在设备上截屏到临时文件
        executeAdbCommand("shell", "screencap", "-p", "/sdcard/screenshot.png");
        // 拉取到本地
        List<String> output = executeAdbCommand("pull", "/sdcard/screenshot.png", outputPath);
        return output.stream().anyMatch(s -> s.contains("100%") || s.contains("pulled"));
    }
    
    /**
     * 检查是否是证书问题
     */
    public boolean isCertificateError(String output) {
        return output.contains("INSTALL_FAILED_UPDATE_INCOMPATIBLE") ||
               output.contains("signature") ||
               output.contains("证书") ||
               output.contains("签名");
    }
    
    /**
     * 关闭管理器
     */
    public void close() {
        if (process != null && process.isAlive()) {
            process.destroy();
        }
    }
}
