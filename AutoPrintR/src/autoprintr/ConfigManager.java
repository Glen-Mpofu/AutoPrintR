package autoprintr;

import java.io.*;
import java.time.Instant;
import java.util.Properties;

public class ConfigManager {
    private final File configFile;
    private final Properties props;

    public ConfigManager() {
        File appFolder = new File(System.getenv("ProgramData"), "AutoPrintR");
        if (!appFolder.exists()) appFolder.mkdirs();
        configFile = new File(appFolder, "config.properties");
        props = new Properties();
        load();
    }

    private void load() {
        if (configFile.exists()) {
            try (FileInputStream fis = new FileInputStream(configFile)) {
                props.load(fis);
            } catch (IOException ignored) {}
        }
    }

    private void save() {
        try (FileOutputStream fos = new FileOutputStream(configFile)) {
            props.store(fos, "AutoPrintR Config");
        } catch (IOException ignored) {}
    }

    public int getCopiesPerDocument() {
        return Integer.parseInt(props.getProperty("copies", "3"));
    }

    public void setCopiesPerDocument(int copies) {
        props.setProperty("copies", String.valueOf(copies));
        save();
    }

    public String getWatchFolder() {
        return props.getProperty("watchFolder", System.getProperty("user.home"));
    }

    public void setWatchFolder(String folder) {
        props.setProperty("watchFolder", folder);
        save();
    }

    public Instant getOrCreateInstallInstant() {
        String val = props.getProperty("installInstant");
        if (val == null) {
            Instant now = Instant.now();
            props.setProperty("installInstant", now.toString());
            save();
            return now;
        }
        return Instant.parse(val);
    }
}
