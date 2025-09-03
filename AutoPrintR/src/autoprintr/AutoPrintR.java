package autoprintr;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.awt.print.Printable;
import java.awt.print.PrinterJob;
import java.io.*;
import java.net.ServerSocket;
import java.net.URL;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.logging.*;
import javax.imageio.ImageIO;
import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import javax.swing.*;
import javax.swing.text.DefaultCaret;


//add a log file. change location of the file from documents to ProgramData
public class AutoPrintR implements ActionListener {
    
    //panels and frames
    private JFrame gui;
    private JPanel mainPnl;
    
    //log txt files
    private static JTextArea msgTxt;
    private static JTextArea descTxt;
    private JButton chooseFolderBtn;
    
    //locations and files
    private static String watchFolder;
    private static String printedFilesLogFilePath;
    private static String printerFolder;
    private static String portFolder;
    private static File defaultNumCopiesFolder = new File(getAppBasePath(),"default_num_copies.txt");    
    private static final String INSTALL_INFO_FILE = "installation_date.txt";
    
    private TrayIcon trayIcon;    
    
    //port information
    private static ServerSocket lockSocket;
    private static int PORT_NUMBER; 
    
    //copies information
    private JComboBox copiesListBox;
    private static int copiesPerDocument = 3;    
    
    private static String progDataFolder = System.getenv("ProgramData");
    
    //this is our user interface
    public AutoPrintR() {
        
        //Gui frame the user interacts with
        gui = new JFrame("AutoPrintR");
        gui.setSize(450, 480);
        gui.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        
        gui.setLocationRelativeTo(null);
        gui.setResizable(false);

        //initialising the tray icon. Basically the icon that will show up next to the app when it get's added to the system tray
        try {
            URL iconURL = getClass().getResource("/resources/AutoPrintR Logo Design.png");
            if (iconURL != null) {
                Image icon = Toolkit.getDefaultToolkit().getImage(iconURL);
                gui.setIconImage(icon);
            } else {
                System.err.println("Icon not found.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        //message that confirms the app was minimised to the tray
        gui.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if(gui.isVisible() == false){
                    gui.setVisible(true);
                }
                
                if (SystemTray.isSupported() && trayIcon != null) {
                    trayIcon.displayMessage("AutoPrintR",
                            "App is minimized to tray. Right-click to open or exit.",
                            TrayIcon.MessageType.INFO);
                }
            }
        });        
        
        //GUI Label
        JLabel heading = new JLabel("Automate Your Prints", JLabel.CENTER);
        heading.setFont(new Font("SansSerif", Font.BOLD, 18));
        heading.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));

        //main panel that holds all our UI Items
        mainPnl = new JPanel(new BorderLayout());

        //log text area
        msgTxt = new JTextArea(15, 30);
        msgTxt.setEditable(false);
        msgTxt.setLineWrap(true);
        msgTxt.setWrapStyleWord(true);
        JScrollPane scrollPane = new JScrollPane(msgTxt);
        DefaultCaret caret = (DefaultCaret) msgTxt.getCaret();
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);

        //description text area with our app's description
        descTxt = new JTextArea(4, 30);
        descTxt.setText("AutoPrintR automatically prints files copied/moved/edited/saved to a folder of thy choice.");
        descTxt.setEditable(false);
        descTxt.setWrapStyleWord(true);
        descTxt.setLineWrap(true);

        //button for changing the Watch folder Path
        chooseFolderBtn = new JButton("Choose/Change Folder");
        chooseFolderBtn.addActionListener(this);

        //center panethat holds the button and description textarea
        JPanel centerPnl = new JPanel();
        centerPnl.add(scrollPane);
        centerPnl.add(chooseFolderBtn);
        centerPnl.add(descTxt); 

        //COPIES PER DOCUMENT
        JPanel bottonPanel = new JPanel(new FlowLayout());
        
        //cobo box showing the number of copies per document a user can choose from
        Object copies[] = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
        copiesListBox = new JComboBox(copies);
        copiesListBox.setSelectedItem(copiesPerDocument);
        
        //action listener for tracking the change of the number of copies per document
        copiesListBox.addActionListener(this);
        
        JLabel copiesLabel = new JLabel("Copies Per Document: ");
        bottonPanel.add(copiesLabel);
        bottonPanel.add(copiesListBox);
        
        mainPnl.add(heading, BorderLayout.NORTH);
        mainPnl.add(centerPnl, BorderLayout.CENTER);
        mainPnl.add(bottonPanel, BorderLayout.PAGE_END);
        gui.add(mainPnl);
        
        if(gui.isVisible() == false){
            gui.setVisible(true);
        }
        
        setupTrayIcon();
    }
    
    //Ensures one instance of the app is runnung by checking if the port is available 
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
        // reading the portnumber from the port_number.txt file
        try {
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
    
    public static void main(String[] args) throws Exception { 
        //Calling the savePortNumber method 
        savePortNumber();
        
        if(!defaultNumCopiesFolder.exists()){            
            saveDefaultCopiesNumber();
        }
        readDefaultCopiesNumber();
        
        //checking whether or not the applictaion is already running
        if (checkIfRunning()) {
            JOptionPane.showMessageDialog(null, "AutoPrintR is already running. Check the 'System Tray' ", 
                    "Already Running", 
                    JOptionPane.INFORMATION_MESSAGE);
            System.exit(0);
        }
        //Initializing the UI
        new AutoPrintR();
        
        createLogFolderDirectory();
        
        //gets the installation date and converts it to an Instant
        String installStr = installationDate().trim();
        LocalDateTime installTime = LocalDateTime.parse(installStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        Instant installInstant = installTime.atZone(ZoneId.systemDefault()).toInstant();

        //Choosing the watch folder which is the folder that contains the files.
        chooseFolderToWatch();
        
        //Loading the files that have been printed
        Set<String> printedFiles = loadPrintedFiles();
        File dir = new File(watchFolder);
        File[] files = dir.listFiles();

        if (files != null) { 
            //Checks if  the current file to be printed is inside the set or not. if it is , it gets ignored as it has been printed before.
            for (File file : files) {
                String name = file.getName().trim().toLowerCase();
                if (file.isFile() && isPrintable(file) && !printedFiles.contains(name)) {
                    try {
                        printFileIfNew(file, installInstant);
                        printedFiles.add(name);
                    } catch (Exception e) {
                        msgTxt.append("Failed to print existing file: " + file.getName() + "\n");
                    }
                }
            }
        }
        
        //gets the folder the user choose as the watch folder and initializes a Watch Event over it to track any changes like a file being added/pasted or modified in the folder
        Path folder = Paths.get(watchFolder);        
        
        WatchService watchService = FileSystems.getDefault().newWatchService();
        
        folder.register(watchService, StandardWatchEventKinds.ENTRY_CREATE);
        msgTxt.append("Watching for new files in: " + watchFolder + "\n");

        while (true) {
            WatchKey key = watchService.take();
            for (WatchEvent<?> event : key.pollEvents()) {
                if (event.kind() == StandardWatchEventKinds.ENTRY_CREATE) {
                    Path newPath = folder.resolve((Path) event.context());
                    File file = newPath.toFile();
                    
                    Thread.sleep(1000);
                    
                    String name = file.getName().trim().toLowerCase();

                    if (file.isFile() && isPrintable(file) && !printedFiles.contains(name)) {
                        try {
                            printFileIfNew(file, installInstant);
                            printedFiles.add(name); 
                        } catch (Exception e) {
                            msgTxt.append("Failed to print new file: " + file.getName() + "\n");
                        }
                    }
                }
            }
            if (!key.reset()) break;
        }
    }

    private void setupTrayIcon() {
        if (!SystemTray.isSupported()) {
            msgTxt.append("System tray is not supported on this platform.\n");
            return;
        }

        SystemTray tray = SystemTray.getSystemTray();
        Image image = Toolkit.getDefaultToolkit().getImage(getClass().getResource("/resources/AutoPrintR Logo Design.png"));

        PopupMenu popup = new PopupMenu();

        MenuItem showItem = new MenuItem("Show");
        showItem.addActionListener(e -> {
            if(gui.isVisible() == false){
                gui.setVisible(true);
            }
            gui.setExtendedState(JFrame.NORMAL);
            gui.toFront();
        });

        MenuItem exitItem = new MenuItem("Exit");
        exitItem.addActionListener(e -> {
            tray.remove(trayIcon);
            System.exit(0);
        });

        popup.add(showItem);
        popup.add(exitItem);

        trayIcon = new TrayIcon(image, "AutoPrintR", popup);
        trayIcon.setImageAutoSize(true);

        trayIcon.addActionListener(e -> {
            if(gui.isVisible() == false){
                gui.setVisible(true);
            }
            gui.setExtendedState(JFrame.NORMAL);
            gui.toFront();
        });

        try {
            tray.add(trayIcon);
        } catch (AWTException e) {
            msgTxt.append("Failed to add to system tray.\n");
        }
    }
    
    private static void printFileIfNew(File file, Instant installInstant) throws InterruptedException, IOException {
        //Prints out files if they were created/modified after the installation date based on the extensions
        BasicFileAttributes attrs = Files.readAttributes(file.toPath(), BasicFileAttributes.class);
            Instant created = attrs.creationTime().toInstant();
            Instant modified = attrs.lastModifiedTime().toInstant();

        if (created.isAfter(installInstant) || modified.isAfter(installInstant)) {
                String ext = getFileExtension(file);
            try {
                switch (ext) {
                    case "pdf":
                        //PDF Printing using SumatraPDF an app for PDF Printing
                        printWithSumatra(file);                                              
                        
                        break;
                    case "doc": case "docx": case "xls": case "xlsx": case "ppt": case "pptx":
                        //Printing Office Files using a Powershell script. Note the computer should have MS Office installed for this to work
                        //Reason for using the powershell script instead of the normal desktop print is to reduce the screen pop ups when printing
                        printWithOffice(file);                        
                        break;
                    case "txt":      
                        //TXT File printing using the computers default printer and Desktop.print
                        for (int i = 0; i < copiesPerDocument; i++) {
                            Desktop.getDesktop().print(file);
                            System.out.println("Txt file logging :"+file.getName());
                        }        
                        appendToLogFile(file.getName());
                        msgTxt.append("TXT File Printed: " + file.getName() + "\n");
                        break;
                    case "jpg": case "jpeg": case "png": case "bmp":
                        //image printing 
                        printImage(file);
                        break;
                    default:     
                        //fallback printing if any of the above are not the specified extension types
                        for (int i = 0; i < copiesPerDocument; i++) {
                            Desktop.getDesktop().print(file);
                            System.out.println("Default Printer logging:"+file.getName() );
                        }          
                        appendToLogFile(file.getName());
                        msgTxt.append("✅ File Printed: " + file.getName() + "\n");
                    }
                } catch (Exception e) {
                    msgTxt.append("❌ Error printing: " + file.getName() + "\n");
                    msgTxt.append(e.getMessage() + "\n");
                }finally{                  
                    Thread.sleep(5000);
                }
            } else {
                msgTxt.append("Skipped (too old): " + file.getName() + "\n");
            }
    }
    
    private static void fallBackPrinter(File file) throws IOException{
        //fallback printing if any of the above specifically the PDF and word scripts
        for (int i = 0; i < copiesPerDocument; i++) {
            Desktop.getDesktop().print(file);
            System.out.println("Default Printer logging:"+file.getName() );
        }          
        appendToLogFile(file.getName());
        msgTxt.append("✅ File Printed -" + file.getName() + "- Using Default Printer. See Previous Error Below: \n");       
    }
    
    //Prints using the SumatraPDF app for silent PDF printing. This is used so the desktop doesn't run into errors if a user's computer has no PDF Reader
    private static void printWithSumatra(File file) throws IOException {
     
        String basePath = getAppBasePath();
        //msgTxt.append(basePath);
        File sumatra = new File(basePath + "/app/tools/SumatraPDF.exe"); //uncomment when building
        //File sumatra = new File(basePath + "/dist/tools/SumatraPDF.exe");
        if (!sumatra.exists()) {                      
           fallBackPrinter(file);  
           throw new IOException("SumatraPDF not found at " + sumatra.getAbsolutePath() + "\n");
        }
        
        //C:\Users\Reception\OneDrive - Tshwane University of Technology\Desktop\Tshepo\AutoPrintR\AutoPrintR\dist\tools\SumatraPDF.exe
        for (int i = 0; i < copiesPerDocument; i++) {
            Runtime.getRuntime().exec("\"" + sumatra.getAbsolutePath() + "\" -print-to-default \"" + file.getAbsolutePath() + "\"");      
            System.out.println("Pdf Logging: "+file.getAbsolutePath());  
        }    
        appendToLogFile(file.getName());
        msgTxt.append("✅ PDF Printed: " + file.getName() + "\n");                
    }
    
    //Office Silent printing using a PowerShell Script.
    private static void printWithOffice(File file) throws IOException {
        int counter=0;
        String basePath = getAppBasePath();
        File script = new File(basePath + "/app/tools/print_office.ps1"); //built app file installation path
        //File script = new File(basePath + "/dist/tools/print_office.ps1"); //netbeans project location
        
        //msgTxt.append(script.getAbsolutePath());
        if (!script.exists()) {                           
                fallBackPrinter(file);                            
            
            throw new IOException("PowerShell script not found. \n");
        }else{
            while(counter < copiesPerDocument){
                Runtime.getRuntime().exec("powershell.exe -ExecutionPolicy Bypass -File \"" + script.getAbsolutePath() + "\" \"" + file.getAbsolutePath() + "\"");
                //Runtime.getRuntime().exec("powershell.exe -ExecutionPolicy RemoteSigned -File \"" + script.getAbsolutePath() + "\" \"" + file.getAbsolutePath() + "\"");
                System.out.println("Office Logging: " + file.getName());
                counter++;
                
                appendToLogFile(file.getName());
            }
            msgTxt.append("✅ Office Document Printed: " + file.getName() + "\n");
        } 
    }
    
    //Image Printing using the default Printer and PrintService. Bypasses all the pop ups and information required about images.
    private static void printImage(File file) throws Exception {
        BufferedImage image = ImageIO.read(file);
        PrintService defaultPrinter = PrintServiceLookup.lookupDefaultPrintService();
        if (defaultPrinter == null) throw new IOException("No default printer found. \n");

        PrinterJob job = PrinterJob.getPrinterJob();
        
        job.setPrintService(defaultPrinter);
        job.setPrintable((g, pf, pageIndex) -> {
            if (pageIndex > 0) return Printable.NO_SUCH_PAGE;
            g.drawImage(image, 100, 100, null);
            return Printable.PAGE_EXISTS;
        });
        
        int count=0;
        do{
            job.print();
            count++;
            System.out.println("Image Logging: " + file.getName());
        }
        while(count<copiesPerDocument);
        appendToLogFile(file.getName());
        msgTxt.append("✅ Image printed: " + file.getName() + "\n");
    }
    
    private static void createLogFolderDirectory() {
        //Creates the app folder under documents to store the log files 
        try {
            
            File baseFolder = new File(progDataFolder, "AutoPrintR");
            if (!baseFolder.exists()) baseFolder.mkdirs();

            File logFile = new File(baseFolder, "printed_files.txt");
            File folderFile = new File(baseFolder, "printer_folder_directory.txt");
            if (!logFile.exists()) logFile.createNewFile();
            if (!folderFile.exists()) folderFile.createNewFile();
            
            printedFilesLogFilePath = logFile.getAbsolutePath();
            printerFolder = folderFile.getAbsolutePath();
        } catch (IOException e) {
            Logger.getLogger(AutoPrintR.class.getName()).log(Level.SEVERE, null, e);
        }
    }

    //writing the installation date into a file
    private static String installationDate() throws IOException {
        
        File file = new File(progDataFolder + File.separator + "AutoPrintR" + File.separator + INSTALL_INFO_FILE);
        if (!file.exists()) {
            String now = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            try (BufferedWriter w = new BufferedWriter(new FileWriter(file))) {
                w.write(now);
            }
            return now;
        }
        return Files.readAllLines(file.toPath()).get(0);
    }

    //method for choosing the watch folder
    private static void chooseFolderToWatch() {
        String saved = readDirectory();
        if (saved != null && !saved.trim().isEmpty()) {
            watchFolder = saved.trim();
            return;
        }

        boolean chosen = false;
        while (!chosen) {
            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("Select Folder to Watch");
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                File selected = chooser.getSelectedFile();
                int confirm = JOptionPane.showConfirmDialog(null, "Use this folder?\n" + selected.getAbsolutePath());
                if (confirm == 0) {
                    watchFolder = selected.getAbsolutePath();
                    saveDirectory(watchFolder);
                    chosen = true;
                }
            } else {
                System.exit(0);
            }
        }
    }

    //saving the Watch folder location
    private static void saveDirectory(String dir) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(printerFolder))) {
            bw.write(dir.trim());
        } catch (IOException e) {
            Logger.getLogger(AutoPrintR.class.getName()).log(Level.SEVERE, null, e);
        }
    }

    //reading the Watch Folder location
    private static String readDirectory() {
        try (BufferedReader br = new BufferedReader(new FileReader(printerFolder))) {
            return br.readLine();
        } catch (IOException e) {
            return null;
        }
    }

    //checking whether or not a file is printable based on the file extension
    private static boolean isPrintable(File file) {
        String name = file.getName().toLowerCase();
        return !name.startsWith("~$") && Set.of("pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "jpg", "jpeg", "png", "bmp").contains(getFileExtension(file));
    }

    //getting a file's extension to check it's printing viability
    private static String getFileExtension(File file) {
        String name = file.getName();
        int lastIndex = name.lastIndexOf('.');
        if (lastIndex == -1) return "";
        
        return name.substring(lastIndex + 1);
    }

    private static Set<String> loadPrintedFiles() {         
        // Loads the printed file names from the log file called printed_files.txt located in the documents folder. 
        Set<String> printed = new HashSet<>();
        try (BufferedReader br = new BufferedReader(new FileReader(printedFilesLogFilePath))) {
            String line;
            while ((line = br.readLine()) != null) printed.add(line.trim().toLowerCase());
        } catch (IOException ignored) {}
        return printed;
    }

    private static void appendToLogFile(String fileName) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(printedFilesLogFilePath, true))) {
            writer.write(fileName);
            writer.newLine();
        } catch (IOException ignored) {}
    }
    
    //changing the watch folder button code
    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == chooseFolderBtn) {          

            boolean chosen = false;
            while (!chosen) {
                JFileChooser chooser = new JFileChooser();
                chooser.setDialogTitle("Select Folder to Watch");
                chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                    File selected = chooser.getSelectedFile();
                    int confirm = JOptionPane.showConfirmDialog(null, "Use this folder?\n" + selected.getAbsolutePath());
                    if (confirm == 0) {
                        watchFolder = selected.getAbsolutePath();
                        msgTxt.append("Changed Watch folder to: " + watchFolder + "\n");
                        saveDirectory(watchFolder);
                        chosen = true;
                    }
                } else {
                    System.exit(0);
                }
            }
        }
        else if(e.getSource() == copiesListBox){
            //setting the copies per document variable for looping purposes
            copiesPerDocument = (int)copiesListBox.getSelectedItem();
            System.out.println(copiesPerDocument);
            
            //saving the new default number of copies
            saveDefaultCopiesNumber();
        }
    }
    
    //default copies storage and retrieval
    private static void saveDefaultCopiesNumber(){
        FileWriter fw = null;
        try {
            if(!defaultNumCopiesFolder.exists()) defaultNumCopiesFolder.createNewFile();
            
            fw = new FileWriter(defaultNumCopiesFolder);
            BufferedWriter bw = new BufferedWriter(fw);
            
            bw.write(String.valueOf(copiesPerDocument));
            
            bw.close();
            fw.close();
        } catch (IOException ex) {
            Logger.getLogger(AutoPrintR.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                fw.close();
            } catch (IOException ex) {
                Logger.getLogger(AutoPrintR.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    
    //reeading the default copies number everytime the app is re run
    private static void readDefaultCopiesNumber(){        
        FileReader fr = null;
        try {
            fr = new FileReader(defaultNumCopiesFolder);
            BufferedReader br = new BufferedReader(fr);
            
            copiesPerDocument = Integer.parseInt(br.readLine());
            
            br.close();
            fr.close();
        } catch (IOException ex) {
            Logger.getLogger(AutoPrintR.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                fr.close();
            } catch (IOException ex) {
                Logger.getLogger(AutoPrintR.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    
    //getting the installation path of the APP 
    private static String getAppBasePath() {
        try {
            return new File(AutoPrintR.class.getProtectionDomain().getCodeSource().getLocation().toURI())
                       .getParentFile().getAbsolutePath();
        } catch (Exception e) {
            return System.getProperty("user.dir"); // fallback
        }
    }
}
