package com.example.app;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.stage.Stage;
import javafx.util.Duration;
import oshi.SystemInfo;
import oshi.hardware.*;
import oshi.software.os.OperatingSystem;

import java.awt.*;
import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Scanner;

public class HelloController {

    // --- FXML FIELDS (JavaFX) ---
    @FXML public Label timeLabel;
    @FXML private Label cpuPercentMain, cpuPercentSmall, cpuModelLabel;
    @FXML private ProgressBar cpuBarMain, cpuBarSmall;
    @FXML private Label ramPercentMain, ramValueSmall, ramDetailLabel;
    @FXML private ProgressBar ramBarMain, ramBarSmall;
    @FXML private Label osLabel, uptimeLabel, diskSpaceLabel, diskPercentLabel, statusLabel, downSpeedLabel, upSpeedLabel;
    @FXML private ProgressBar diskBar;
    @FXML private Button settingsButton;
    @FXML private Label gpuModelLabel, gpuUsageMain, gpuUsageSmall, gpuVramLabel;
    @FXML private ProgressBar gpuBarMain, gpuBarSmall;

    // --- VARIABLES ---
    private boolean isLightMode = false;
    private final SystemInfo si = new SystemInfo();
    private long[] oldTicks;
    private long lastBytesRecv = 0, lastBytesSent = 0;

    public void initialize() {
        CentralProcessor processor = si.getHardware().getProcessor();
        oldTicks = processor.getSystemCpuLoadTicks();
        cpuModelLabel.setText(processor.getProcessorIdentifier().getName());
        OperatingSystem os = si.getOperatingSystem();
        osLabel.setText("OS: " + os.getFamily() + " " + os.getVersionInfo().getVersion());

        // Ενεργοποίηση System Tray
        Platform.runLater(this::setupSystemTray);

        Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> updateDashboard()));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();

        checkForUpdates();
    }

    private void setupSystemTray() {
        if (!SystemTray.isSupported()) return;

        try {
            SystemTray tray = SystemTray.getSystemTray();
            // Φόρτωση εικονιδίου
            Image image = Toolkit.getDefaultToolkit().getImage(getClass().getResource("/com/example/app/icon.png"));

            // Χρησιμοποιούμε πλήρη ονόματα (java.awt) για να μην μπερδεύονται με το JavaFX
            java.awt.PopupMenu popup = new java.awt.PopupMenu();

            java.awt.MenuItem showItem = new java.awt.MenuItem("Open Monitor");
            showItem.addActionListener(e -> Platform.runLater(() -> {
                Stage stage = (Stage) timeLabel.getScene().getWindow();
                stage.show();
                stage.setIconified(false);
                stage.toFront();
            }));

            java.awt.MenuItem exitItem = new java.awt.MenuItem("Exit");
            exitItem.addActionListener(e -> {
                Platform.exit();
                System.exit(0);
            });

            popup.add(showItem);
            popup.addSeparator();
            popup.add(exitItem);

            TrayIcon trayIcon = new TrayIcon(image, "System Monitor Pro", popup);
            trayIcon.setImageAutoSize(true);
            trayIcon.addActionListener(e -> Platform.runLater(() -> {
                Stage stage = (Stage) timeLabel.getScene().getWindow();
                stage.show();
                stage.setIconified(false);
            }));

            tray.add(trayIcon);

            Stage stage = (Stage) timeLabel.getScene().getWindow();
            stage.iconifiedProperty().addListener((ov, t, t1) -> {
                if (t1) Platform.runLater(stage::hide);
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateDashboard() {
        try {
            timeLabel.setText(LocalDateTime.now().format(DateTimeFormatter.ofPattern("hh:mm:ss a")));

            // CPU
            CentralProcessor processor = si.getHardware().getProcessor();
            double cpuLoad = processor.getSystemCpuLoadBetweenTicks(oldTicks);
            oldTicks = processor.getSystemCpuLoadTicks();
            int cpuInt = (int) (cpuLoad * 100);
            cpuPercentMain.setText(cpuInt + "%");
            cpuPercentSmall.setText(cpuInt + "%");
            cpuBarMain.setProgress(cpuLoad);
            cpuBarSmall.setProgress(cpuLoad);

            // RAM
            GlobalMemory memory = si.getHardware().getMemory();
            double totalRam = memory.getTotal() / 1073741824.0;
            double usedRam = totalRam - (memory.getAvailable() / 1073741824.0);
            double ramPercent = usedRam / totalRam;
            ramPercentMain.setText((int)(ramPercent * 100) + "%");
            ramValueSmall.setText(String.format("%.1f GB", usedRam));
            ramDetailLabel.setText(String.format("Used: %.1f GB / Total: %.1f GB", usedRam, totalRam));
            ramBarMain.setProgress(ramPercent);
            ramBarSmall.setProgress(ramPercent);

            // DISK
            File cDrive = new File("C:");
            long totalSpace = cDrive.getTotalSpace();
            double dPercent = 1.0 - ((double)cDrive.getFreeSpace() / totalSpace);
            diskPercentLabel.setText((int)(dPercent * 100) + "%");
            diskSpaceLabel.setText(String.format("%.1f GB / %.1f GB", (totalSpace - cDrive.getFreeSpace())/1073741824.0, totalSpace/1073741824.0));
            diskBar.setProgress(dPercent);

            // NETWORK
            var nics = si.getHardware().getNetworkIFs();
            if (!nics.isEmpty()) {
                NetworkIF net = nics.get(0);
                net.updateAttributes();
                if (lastBytesRecv > 0) {
                    downSpeedLabel.setText(String.format("%.1f KB/s", (net.getBytesRecv() - lastBytesRecv) / 1024.0));
                    upSpeedLabel.setText(String.format("%.1f KB/s", (net.getBytesSent() - lastBytesSent) / 1024.0));
                }
                lastBytesRecv = net.getBytesRecv();
                lastBytesSent = net.getBytesSent();
            }

            updateGpuMetrics();
            uptimeLabel.setText(String.format("Uptime: %dh %dm", si.getOperatingSystem().getSystemUptime() / 3600, (si.getOperatingSystem().getSystemUptime() % 3600) / 60));

        } catch (Exception e) {
            statusLabel.setText("Update error.");
        }
    }

    private void updateGpuMetrics() {
        List<GraphicsCard> cards = si.getHardware().getGraphicsCards();
        if (!cards.isEmpty()) {
            GraphicsCard selectedGpu = cards.get(0);
            for (GraphicsCard card : cards) {
                if (card.getVRam() > selectedGpu.getVRam()) selectedGpu = card;
            }

            final GraphicsCard finalGpu = selectedGpu;
            double vramGB = finalGpu.getVRam() / (1024.0 * 1024.0 * 1024.0);

            Platform.runLater(() -> {
                gpuModelLabel.setText(finalGpu.getName());
                gpuVramLabel.setText(String.format("VRAM: %.1f GB Total", vramGB));
            });
            getGpuUsageWindows();
        }
    }

    private void getGpuUsageWindows() {
        Thread gpuThread = new Thread(() -> {
            try {
                String command = "Get-Counter '\\GPU Engine(*engtype_3D)\\Utilization Percentage' | Select-Object -ExpandProperty CounterSamples | Measure-Object -Property CookedValue -Sum | Select-Object -ExpandProperty Sum";
                Process p = new ProcessBuilder("powershell", "-Command", command).start();
                Scanner s = new Scanner(p.getInputStream());
                if (s.hasNext()) {
                    String output = s.next().replace(",", ".");
                    double usage = Math.min(100, Math.max(0, Double.parseDouble(output)));
                    Platform.runLater(() -> {
                        gpuUsageMain.setText(String.format("%.1f%%", usage));
                        gpuUsageSmall.setText(String.format("%.0f%%", usage));
                        gpuBarMain.setProgress(usage / 100.0);
                        gpuBarSmall.setProgress(usage / 100.0);
                    });
                }
                s.close();
            } catch (Exception e) {
                Platform.runLater(() -> gpuUsageMain.setText("0.0%"));
            }
        });
        gpuThread.setDaemon(true);
        gpuThread.start();
    }

    @FXML private void handleCleanTemp() {
        statusLabel.setText("Cleaning...");
        File tempDir = new File(System.getProperty("java.io.tmpdir"));
        File[] files = tempDir.listFiles();
        int count = 0;
        if (files != null) {
            for (File f : files) if (f.delete()) count++;
        }
        statusLabel.setText("Done! Cleaned " + count + " items.");
    }

    @FXML private void handleSettings() {
        ContextMenu menu = new ContextMenu();
        javafx.scene.control.MenuItem theme = new javafx.scene.control.MenuItem(isLightMode ? "🌙 Dark Mode" : "☀ Light Mode");
        theme.setOnAction(e -> toggleTheme());
        CheckMenuItem top = new CheckMenuItem("📌 Always on Top");
        Stage stage = (Stage) settingsButton.getScene().getWindow();
        top.setSelected(stage.isAlwaysOnTop());
        top.setOnAction(e -> stage.setAlwaysOnTop(top.isSelected()));
        menu.getItems().addAll(theme, top);
        menu.show(settingsButton, javafx.geometry.Side.BOTTOM, 0, 0);
    }

    @FXML private void handleOpenExplorer() {
        try { Desktop.getDesktop().open(new File("C:\\")); }
        catch (Exception e) { statusLabel.setText("Error opening C:"); }
    }

    private void toggleTheme() {
        isLightMode = !isLightMode;
        Scene scene = settingsButton.getScene();
        scene.getStylesheets().clear();
        String css = getClass().getResource(isLightMode ? "light-style.css" : "style.css").toExternalForm();
        scene.getStylesheets().add(css);
    }

    private void checkForUpdates() {
        String versionUrl = "https://raw.githubusercontent.com/joxanpalas31-ops/SystemMonitorApp/main/version.txt?t=" + System.currentTimeMillis();
        String currentVersion = "1.0";
        Thread updateThread = new Thread(() -> {
            try {
                var client = java.net.http.HttpClient.newHttpClient();
                var request = java.net.http.HttpRequest.newBuilder().uri(java.net.URI.create(versionUrl)).build();
                var response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
                String latestVersion = response.body().trim();
                if (!latestVersion.equals(currentVersion)) {
                    Platform.runLater(() -> {
                        Alert alert = new Alert(Alert.AlertType.INFORMATION);
                        alert.setTitle("Ενημέρωση Συστήματος");
                        alert.setHeaderText("Βρέθηκε νέα έκδοση: v" + latestVersion);
                        alert.setContentText("Παρακαλώ κατεβάστε τον νέο Installer.");
                        ButtonType downloadBtn = new ButtonType("Λήψη");
                        alert.getButtonTypes().setAll(downloadBtn, ButtonType.CLOSE);
                        alert.showAndWait().ifPresent(type -> {
                            if (type == downloadBtn) {
                                try { Desktop.getDesktop().browse(new java.net.URI("https://github.com/joxanpalas31-ops/SystemMonitorApp/releases")); }
                                catch (Exception e) { e.printStackTrace(); }
                            }
                        });
                    });
                }
            } catch (Exception e) { /* Ignore update errors */ }
        });
        updateThread.setDaemon(true);
        updateThread.start();
    }
}