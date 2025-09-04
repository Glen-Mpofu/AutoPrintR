package autoprintr;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.ServerSocket;
import java.time.Instant;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;

public class AutoPrintR {

    private final ConfigManager configManager;
    private final LogManager logManager;
    private final AutoPrintRUI ui;
    private final FileWatcherService fileWatcherService;
    private final PrintManager printManager;

    private final Instant installInstant;

    private static int PORT_NUMBER; 
    private static ServerSocket lockSocket;
    private static String portFolder;
    
    public AutoPrintR() {
        // Setup config + logs
        configManager = new ConfigManager();
        logManager = new LogManager();

        // Load settings
        int copies = configManager.getCopiesPerDocument();
        String watchFolder = configManager.getWatchFolder();

        // UI
        ui = new AutoPrintRUI(this, copies, watchFolder);

        // Printing manager
        printManager = new PrintManager(copies, logManager, ui);
        
        // Installation timestamp
        installInstant = configManager.getOrCreateInstallInstant();
    
        // Folder watcher
        fileWatcherService = new FileWatcherService(watchFolder, (File f) -> {
            try {
                printManager.printFileIfNew(f, installInstant);
            } catch (Exception e) {
                logManager.logError("Error printing file " + f.getName(), e);
                ui.logMessage(e.getMessage());
            }
        });

    }

    public void start() {
        ui.show();
        fileWatcherService.startWatching();
        ui.logMessage("Watching: " + configManager.getWatchFolder() + " for any new files");
    }

    public void updateCopies(int copies) {
        configManager.setCopiesPerDocument(copies);
        printManager.setCopiesPerDocument(copies);
        ui.logMessage("Copies per document updated to: " + copies);
    }

    public void updateWatchFolder(String folder) {
        configManager.setWatchFolder(folder);
        fileWatcherService.setWatchFolder(folder);
        ui.logMessage("New Watch folder: " + folder);
    }

    public static void main(String[] args) {
        savePortNumber();
        if(checkIfRunning() == false){
            new AutoPrintR().start();
        }else{
            JOptionPane.showMessageDialog(null, "AutoPrintR is already running. Check the 'System Tray' ", 
                    "Already Running", 
                    JOptionPane.INFORMATION_MESSAGE);
            System.exit(0);
        }
        
            
    }
    
    private static boolean checkIfRunning() {
        try {            
            readPortNumber();
            // Use a hard-coded port number (should be unique to your app)
            lockSocket = new ServerSocket(PORT_NUMBER);
            return false; // No other instance running
        } catch (IOException e) {
            // Port is already in use
            return true;
        }
    }
    
    public static void readPortNumber() {
        try {
            // reading the portnumber from the port_number.txt file
            FileReader fr = new FileReader(portFolder);
            BufferedReader br = new BufferedReader(fr);
            PORT_NUMBER = Integer.parseInt(br.readLine());
            br.close();        
            fr.close();
        } catch (IOException ex) {
            Logger.getLogger(AutoPrintR.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public static void savePortNumber(){    
        // It saves the port number inside the port_number.txt file
        try {
            String localAppData  = System.getenv("LOCALAPPDATA");
            
            File localAPFolder = new File(localAppData,"AutoPrintR");
            if (!localAPFolder.exists()) {
                localAPFolder.mkdir();
            }
            
            File portFile = new File(localAPFolder+"/", "port_number.txt");            
            
            if(!portFile.exists()) portFile.createNewFile();
            
            portFolder = portFile.getAbsolutePath();
            
            FileWriter fw = new FileWriter(portFile);
            BufferedWriter bw = new BufferedWriter(fw);
            
            bw.write(String.valueOf(65432));
            
            bw.close();
            fw.close();
        } catch (IOException ex) {
            Logger.getLogger(AutoPrintR.class.getName()).log(Level.SEVERE, null, ex);
        }        
    }
}
