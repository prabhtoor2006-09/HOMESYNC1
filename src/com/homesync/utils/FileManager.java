package com.homesync.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

/**
 * FileManager - reusable file operations for the whole HOMESYNC project.
 *
 * Every record is one line of text. Fields are separated by a comma.
 * The FIRST field of a line is its "key" (for example the device ID).
 *
 * All methods are static and synchronized. Several threads (the menu thread,
 * the auto-backup thread, the energy monitor thread) may touch files at the
 * same time. "synchronized" makes sure only one thread uses FileManager at a
 * time, so two threads can never write the same file together.
 */
public class FileManager {

    // ---- Data file locations (relative to the project folder) ----
    public static final String DATA_DIR = "data";
    public static final String BACKUP_DIR = DATA_DIR + "/backups";
    public static final String DEVICES_FILE = DATA_DIR + "/devices.csv";
    public static final String SENSORS_FILE = DATA_DIR + "/sensors.csv";
    public static final String ENERGY_FILE = DATA_DIR + "/energy_records.csv";
    public static final String ACTIVITY_LOG_FILE = DATA_DIR + "/activity_log.txt";

    public static final String SEPARATOR = ",";

    // Nobody should create a FileManager object - it only holds static tools.
    private FileManager() {
    }

    // ---------------------------------------------------------------- CREATE

    /** Creates the file (and its parent folders) if it does not exist. */
    public static synchronized boolean createFile(String path) {
        File file = new File(path);
        try {
            ensureParentFolder(file);
            if (!file.exists()) {
                file.createNewFile();
            }
        } catch (IOException e) {
            System.out.println("Could not create file " + path + ": " + e.getMessage());
        }
        return file.exists();
    }

    private static void ensureParentFolder(File file) {
        File parent = file.getAbsoluteFile().getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
    }

    public static synchronized boolean fileExists(String path) {
        return new File(path).isFile();
    }

    // ------------------------------------------------------------------ READ

    /** Reads all non-empty lines. A missing file simply gives an empty list. */
    public static synchronized List<String> readLines(String path) {
        List<String> lines = new ArrayList<>();
        File file = new File(path);

        if (!file.exists()) {
            return lines; // missing file is not an error
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty()) {
                    lines.add(line);
                }
            }
        } catch (IOException e) {
            System.out.println("Could not read " + path + ": " + e.getMessage());
        }
        return lines;
    }

    public static synchronized int countLines(String path) {
        return readLines(path).size();
    }

    // ----------------------------------------------------------------- WRITE

    /**
     * Replaces the whole file with the given lines.
     * Safe-write: first write a temporary file, then swap it in. If the
     * program fails halfway, the original file is still intact.
     */
    public static synchronized boolean writeLines(String path, List<String> lines) {
        File target = new File(path);
        File temp = new File(path + ".tmp");
        try {
            ensureParentFolder(target);

            try (BufferedWriter writer = new BufferedWriter(new FileWriter(temp))) {
                for (String line : lines) {
                    writer.write(line);
                    writer.newLine();
                }
            }

            Files.move(temp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
            return true;
        } catch (IOException e) {
            System.out.println("Could not write " + path + ": " + e.getMessage());
            return false;
        } finally {
            // finally always runs: remove the temp file if something went wrong
            if (temp.exists()) {
                temp.delete();
            }
        }
    }

    /** Adds one line at the end of the file (creates the file if needed). */
    public static synchronized boolean appendLine(String path, String line) {
        File file = new File(path);
        ensureParentFolder(file);

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, true))) {
            writer.write(line);
            writer.newLine();
            return true;
        } catch (IOException e) {
            System.out.println("Could not append to " + path + ": " + e.getMessage());
            return false;
        }
    }

    // ---------------------------------------------------------------- SEARCH

    /** Lines that contain the keyword anywhere (ignores upper/lower case). */
    public static synchronized List<String> searchLines(String path, String keyword) {
        List<String> matches = new ArrayList<>();
        if (keyword == null || keyword.trim().isEmpty()) {
            return matches;
        }
        String lowerKeyword = keyword.trim().toLowerCase();

        for (String line : readLines(path)) {
            if (line.toLowerCase().contains(lowerKeyword)) {
                matches.add(line);
            }
        }
        return matches;
    }

    /** Overloaded: lines where one chosen column (0 = first) equals the value. */
    public static synchronized List<String> searchLines(String path, int columnIndex, String value) {
        List<String> matches = new ArrayList<>();
        if (value == null) {
            return matches;
        }

        for (String line : readLines(path)) {
            String[] fields = splitFields(line);
            if (columnIndex >= 0 && columnIndex < fields.length
                    && fields[columnIndex].equalsIgnoreCase(value.trim())) {
                matches.add(line);
            }
        }
        return matches;
    }

    /** Finds the line whose first field (key) equals the given key, or null. */
    public static synchronized String findLine(String path, String key) {
        for (String line : readLines(path)) {
            if (keyOf(line).equalsIgnoreCase(key.trim())) {
                return line;
            }
        }
        return null;
    }

    public static synchronized boolean containsKey(String path, String key) {
        return findLine(path, key) != null;
    }

    // ---------------------------------------------------------------- UPDATE

    /** Replaces the line with the given key. Returns false if key not found. */
    public static synchronized boolean updateLine(String path, String key, String newLine) {
        List<String> lines = readLines(path);
        boolean found = false;

        for (int i = 0; i < lines.size(); i++) {
            if (keyOf(lines.get(i)).equalsIgnoreCase(key.trim())) {
                lines.set(i, newLine);
                found = true;
                break;
            }
        }

        if (!found) {
            return false;
        }
        return writeLines(path, lines);
    }

    // ---------------------------------------------------------------- DELETE

    /** Removes the line with the given key. Returns false if key not found. */
    public static synchronized boolean deleteLine(String path, String key) {
        List<String> lines = readLines(path);
        boolean removed = lines.removeIf(line -> keyOf(line).equalsIgnoreCase(key.trim()));

        if (!removed) {
            return false;
        }
        return writeLines(path, lines);
    }

    public static synchronized boolean deleteFile(String path) {
        File file = new File(path);
        return file.exists() && file.delete();
    }

    // ------------------------------------------------------------ COPY (backup)

    /** Copies a file. Returns false if the source does not exist. */
    public static synchronized boolean copyFile(String sourcePath, String destinationPath) {
        File source = new File(sourcePath);
        File destination = new File(destinationPath);

        if (!source.isFile()) {
            return false;
        }

        try {
            ensureParentFolder(destination);
            Files.copy(source.toPath(), destination.toPath(), StandardCopyOption.REPLACE_EXISTING);
            return true;
        } catch (IOException e) {
            System.out.println("Could not copy " + sourcePath + ": " + e.getMessage());
            return false;
        }
    }

    // ------------------------------------------------------- RECORD HELPERS

    /** Splits "1,Light,Lamp" into ["1","Light","Lamp"]; -1 keeps empty fields. */
    public static String[] splitFields(String line) {
        String[] fields = line.split(SEPARATOR, -1);
        for (int i = 0; i < fields.length; i++) {
            fields[i] = fields[i].trim();
        }
        return fields;
    }

    public static String keyOf(String line) {
        return splitFields(line)[0];
    }

    /** A record is valid if it has the right number of fields and none is empty. */
    public static boolean isValidRecord(String line, int expectedFields) {
        if (line == null || line.trim().isEmpty()) {
            return false;
        }
        String[] fields = splitFields(line);
        if (fields.length != expectedFields) {
            return false;
        }
        for (String field : fields) {
            if (field.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    /** Commas inside text would break the CSV format, so replace them. */
    public static String cleanText(String text) {
        return text.replace(SEPARATOR, " ").trim();
    }
}
