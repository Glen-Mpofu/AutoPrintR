package autoprintr;

import java.io.*;

public class LogManager {
    private File logFile;

    public LogManager() {
        File appFolder = new File(System.getenv("ProgramData"), "AutoPrintR");
        if (!appFolder.exists()) appFolder.mkdirs();
        logFile = new File(appFolder, "printed_files.txt");
        if (!logFile.exists()) {
            try { logFile.createNewFile(); } catch (IOException ignored) {}
        }
    }

    public void logPrinted(String fileName) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(logFile, true))) {
            writer.write(fileName);
            writer.newLine();
        } catch (IOException ignored) {}
    }
    
    public File getLogFile() {
        return logFile;
    }
    
}
