package autoprintr;

import java.io.*;
import java.time.Instant;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ConfigManager {
    private final File configFile;
    private final Properties props;
    private File appFolder;
    
    private File logFile;
    
    public ConfigManager() {
        appFolder = new File(System.getenv("ProgramData"), "AutoPrintR");
        if (!appFolder.exists()) appFolder.mkdirs();
        configFile = new File(appFolder, "config.properties");
        props = new Properties();
        logFile = new File(appFolder, "log_file.txt");
        
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
        File watchFile = new File(appFolder, "watch_folder.txt");
        if (!watchFile.exists()) {
            try {
                watchFile.createNewFile();
            } catch (IOException ex) {
                Logger.getLogger(ConfigManager.class.getName()).log(Level.SEVERE, null, ex);
            }
            return ""; // no folder yet
        }

        try (BufferedReader br = new BufferedReader(new FileReader(watchFile))) {
            String folder = br.readLine();
            return folder != null ? folder.trim() : "";
        } catch (IOException ex) {
            Logger.getLogger(ConfigManager.class.getName()).log(Level.SEVERE, null, ex);
            return "";
        }
    }

    public void setWatchFolder(String folder) {
        File watchFile = new File(appFolder, "watch_folder.txt");
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(watchFile))) {
             bw.write(folder);
        } catch (IOException ex) {
            Logger.getLogger(ConfigManager.class.getName()).log(Level.SEVERE, null, ex);
        }
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
    
    public void appendToLogFile(String msg){
        
        FileWriter fWriter = null;
        try {
            if(!logFile.exists()){
                    logFile.createNewFile();
            }   
            
            fWriter = new FileWriter(logFile, true);
            BufferedWriter bWriter = new BufferedWriter(fWriter);
            
            bWriter.append(msg);
            bWriter.newLine();
            
            bWriter.close();
            
        } catch (IOException ex) {
            Logger.getLogger(ConfigManager.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                fWriter.close();
            } catch (IOException ex) {
                Logger.getLogger(ConfigManager.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
    }
}
