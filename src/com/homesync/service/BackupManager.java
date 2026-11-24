package com.homesync.service;

import com.homesync.utils.FileManager;
import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;

/**
 * BackupManager - copies the data files into data/backups/ and restores them.
 *
 * Each backup is a folder:  data/backups/backup_20261125_140322/
 * containing copies of devices.csv, sensors.csv, energy_records.csv, activity_log.txt.
 *
 * Protection against data loss:
 *   * Restoring first makes a safety copy "prerestore_..." of the current data,
 *     so the restore can itself be undone.
 *   * Only the newest MAX_BACKUPS backups are kept (older ones are deleted).
 *
 * Collection: Deque used as a stack (LIFO). The newest backup is on top, so
 * "restore latest" is just peek(). The oldest is at the bottom and is removed first.
 *
 * Thread: startAutoBackup() runs a daemon thread that creates a backup every
 * few minutes.
 */
public class BackupManager {

    private static final String BACKUP_PREFIX = "backup_";
    private static final String SAFETY_PREFIX = "prerestore_";
    private static final int MAX_BACKUPS = 10;
    private static final int MAX_SAFETY_COPIES = 3;
    private static final DateTimeFormatter NAME_TIME = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private static final String[] DATA_FILES = {
        FileManager.DEVICES_FILE,
        FileManager.SENSORS_FILE,
        FileManager.ENERGY_FILE,
        FileManager.ACTIVITY_LOG_FILE
    };

    private final String backupRoot;
    private final Deque<String> backupStack = new ArrayDeque<>();

    private final Object autoLock = new Object();
    private Thread autoBackupThread;

    public BackupManager() {
        this(FileManager.BACKUP_DIR);
    }

    public BackupManager(String backupRoot) {
        this.backupRoot = backupRoot;
        new File(backupRoot).mkdirs();
        rebuildStack();
    }

    // -------------------------------------------------------------- CREATE

    /** Creates a backup. Returns its name, or null if there was nothing to back up. */
    public synchronized String createBackup() {
        String name = createFolderBackup(BACKUP_PREFIX);
        if (name != null) {
            backupStack.push(name);                 // newest on top
            enforceLimit();
            ActivityLog.getInstance().logBackupCreated(name);
        }
        return name;
    }

    private String createFolderBackup(String prefix) {
        String baseName = prefix + LocalDateTime.now().format(NAME_TIME);
        String name = baseName;
        int counter = 1;
        while (new File(backupRoot, name).exists()) {   // two backups in the same second
            name = baseName + "_" + counter++;
        }

        File folder = new File(backupRoot, name);
        folder.mkdirs();

        int copied = 0;
        for (String dataFile : DATA_FILES) {
            String fileName = new File(dataFile).getName();
            if (FileManager.copyFile(dataFile, new File(folder, fileName).getPath())) {
                copied++;
            }
        }

        if (copied == 0) {
            folder.delete();    // empty folder - nothing was backed up
            return null;
        }
        return name;
    }

    // ------------------------------------------------------------- RESTORE

    /** Restores one backup by name. Returns false if it is missing or empty. */
    public synchronized boolean restoreBackup(String backupName) {
        File folder = new File(backupRoot, backupName);
        if (!folder.isDirectory() || !verifyBackup(backupName)) {
            System.out.println("Backup not found or empty: " + backupName);
            return false;
        }

        // Safety copy first, so nothing is lost by accident.
        String safety = createFolderBackup(SAFETY_PREFIX);
        if (safety != null) {
            System.out.println("Safety copy of current data saved as: " + safety);
        }

        int restored = 0;
        File[] files = folder.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile() && FileManager.copyFile(file.getPath(),
                        FileManager.DATA_DIR + "/" + file.getName())) {
                    restored++;
                }
            }
        }

        removeOldFolders(SAFETY_PREFIX, MAX_SAFETY_COPIES);
        ActivityLog.getInstance().logDataRestored(backupName);
        System.out.println("Restored " + restored + " file(s) from " + backupName);
        return restored > 0;
    }

    /** Restores the newest backup (top of the stack). */
    public synchronized boolean restoreLatestBackup() {
        String latest = backupStack.peek();
        if (latest == null) {
            System.out.println("No backups available.");
            return false;
        }
        return restoreBackup(latest);
    }

    // ------------------------------------------------------ MAINTAIN / LIST

    /** A backup is valid if its folder exists and holds at least one file. */
    public boolean verifyBackup(String backupName) {
        File[] files = new File(backupRoot, backupName).listFiles();
        return files != null && files.length > 0;
    }

    public synchronized List<String> listBackups() {
        List<String> names = new ArrayList<>(backupStack);
        Collections.sort(names);      // oldest first
        return names;
    }

    public synchronized String getLatestBackupName() {
        return backupStack.peek();
    }

    public void displayBackups() {
        List<String> names = listBackups();
        System.out.println("\n===== BACKUPS (" + backupRoot + ") =====");
        if (names.isEmpty()) {
            System.out.println("No backups found.");
            return;
        }
        for (String name : names) {
            File[] files = new File(backupRoot, name).listFiles();
            System.out.println(name + "  (" + (files == null ? 0 : files.length) + " file(s))");
        }
    }

    public synchronized boolean deleteBackup(String backupName) {
        File folder = new File(backupRoot, backupName);
        if (!folder.isDirectory()) {
            return false;
        }
        File[] files = folder.listFiles();
        if (files != null) {
            for (File file : files) {
                file.delete();
            }
        }
        backupStack.remove(backupName);
        return folder.delete();
    }

    /** Reads the backup folder so backups from earlier runs are known again. */
    private void rebuildStack() {
        backupStack.clear();
        for (String name : folderNames(BACKUP_PREFIX)) {
            backupStack.push(name);    // oldest pushed first, newest ends on top
        }
        enforceLimit();
    }

    private void enforceLimit() {
        while (backupStack.size() > MAX_BACKUPS) {
            String oldest = backupStack.removeLast();    // bottom of the stack
            deleteBackup(oldest);
        }
    }

    private void removeOldFolders(String prefix, int keep) {
        List<String> names = folderNames(prefix);
        for (int i = 0; i < names.size() - keep; i++) {
            deleteBackup(names.get(i));
        }
    }

    /** Folder names with the given prefix, oldest first. */
    private List<String> folderNames(String prefix) {
        List<String> names = new ArrayList<>();
        File[] folders = new File(backupRoot).listFiles();
        if (folders != null) {
            for (File folder : folders) {
                if (folder.isDirectory() && folder.getName().startsWith(prefix)) {
                    names.add(folder.getName());
                }
            }
        }
        Collections.sort(names);
        return names;
    }

    // --------------------------------------------------- AUTOMATIC BACKUP THREAD

    public boolean startAutoBackup(long intervalMillis) {
        if (intervalMillis <= 0) {
            throw new IllegalArgumentException("Interval must be positive.");
        }
        synchronized (autoLock) {
            if (autoBackupThread != null && autoBackupThread.isAlive()) {
                return false;
            }
            autoBackupThread = new Thread(new AutoBackupTask(intervalMillis), "AutoBackup");
            autoBackupThread.setDaemon(true);
            autoBackupThread.setPriority(Thread.MIN_PRIORITY);
            autoBackupThread.start();
            return true;
        }
    }

    public boolean stopAutoBackup() {
        Thread threadToStop;
        synchronized (autoLock) {
            threadToStop = autoBackupThread;
            autoBackupThread = null;
        }
        if (threadToStop == null) {
            return false;
        }
        threadToStop.interrupt();
        try {
            threadToStop.join(3000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return true;
    }

    public boolean isAutoBackupRunning() {
        synchronized (autoLock) {
            return autoBackupThread != null && autoBackupThread.isAlive();
        }
    }

    private class AutoBackupTask implements Runnable {

        private final long intervalMillis;

        AutoBackupTask(long intervalMillis) {
            this.intervalMillis = intervalMillis;
        }

        @Override
        public void run() {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Thread.sleep(intervalMillis);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
                createBackup();
            }
        }
    }
}
