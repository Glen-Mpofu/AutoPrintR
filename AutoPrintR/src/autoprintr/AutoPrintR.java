/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Main.java to edit this template
 */
package autoprintr;

import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.Font;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.text.DefaultCaret;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class AutoPrintR implements ActionListener{
    //GUI Components
    private JFrame gui;
    private JPanel mainPnl;

    private static JTextArea msgTxt;
    private static JTextArea descTxt;

    private JButton chooseFolderBtn;

    public AutoPrintR(){
        gui = new JFrame("AutoPrintR");
        gui.setSize(450,480);
        gui.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        gui.setLocationRelativeTo(null);
        gui.setResizable(false);
        
        URL iconURL = getClass().getResource("/resources/AutoPrintR Logo Design.png");
        if (iconURL != null) {
            Image icon = Toolkit.getDefaultToolkit().getImage(iconURL);
            gui.setIconImage(icon); // setIconImage replaces default icon :contentReference[oaicite:1]{index=1}
        } else {
            System.err.println("Icon not found at /resources/myIcon.png");
        }
        // Create heading label
        JLabel heading = new JLabel("Automate Your Prints", JLabel.CENTER);
        heading.setFont(new Font("SansSerif", Font.BOLD, 18));
        heading.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        // Optionally: heading.setOpaque(true); heading.setBackground(Color.LIGHT_GRAY);

        mainPnl = new JPanel();
        mainPnl.setLayout(new BorderLayout());
        
        msgTxt = new JTextArea(15, 30);
        msgTxt.setEditable(false);
        msgTxt.setLineWrap(true);
        msgTxt.setWrapStyleWord(true);

        descTxt = new JTextArea(10, 30);
        descTxt.append("AutoPrintR is designed to automatically print files that are added/moved/copied to a folder of your selection");
        descTxt.setEditable(false);
        descTxt.setWrapStyleWord(true);
        descTxt.setLineWrap(true);
        
        JScrollPane scrollPane = new JScrollPane(msgTxt);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        
        // Force auto-scroll when appending
        DefaultCaret caret = (DefaultCaret) msgTxt.getCaret();
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
        
        chooseFolderBtn = new JButton("Choose/Change Folder");
        chooseFolderBtn.addActionListener(this);
        
        JPanel centerPnl = new JPanel();
        centerPnl.setOpaque(false);
        centerPnl.add(scrollPane);
        centerPnl.add(chooseFolderBtn);
        
        JPanel innerPnl = new JPanel();
        innerPnl.add(descTxt);
        
        centerPnl.add(innerPnl);
        // Assemble panels
        mainPnl.add(heading, BorderLayout.NORTH);
        mainPnl.add(centerPnl, BorderLayout.CENTER);
        
        gui.add(mainPnl);
       
        gui.setVisible(true);
    }

    //private static final String folderPath = "C:\\Users\\Tshepo Mpofu\\Desktop\\i.am.mgt\\School\\WIL\\Files for Printing";
    private static String folderPath;
    private static String logFilePath;
    private static String printerFolder;
    private static final String INSTALL_INFO_FILE = "installaltion_date.txt";

    public static void main(String[] args) throws Exception {
        new AutoPrintR();

        createDirectory();
        //method that allows the user to choose a path that will contain all the files to be printed
        chooseFolderToWatch();

        File dir = new File(folderPath);

        //a set that will contan the names of the files that already have been printed
        Set<String> printedFiles = loadPrintedFiles();

        //array of files in the directory.
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {

                // normalize name
                String normalized = file.getName().trim().toLowerCase();

                //if the file in the directory is indeed a file and is printable meaning is the file's extension is allowed for printing and the log file doesn't contain the file name then print the file
                if (file.isFile() && isPrintable(file) && !printedFiles.contains(normalized)) {
                    try {
                        printFile(file);

                        //adding the printed file name to the set
                        printedFiles.add(normalized);

                        // adding the file name to the log(list of printed files)
                        appendToLogFile(normalized);
                        //System.out.println("File Successfully Printed: " + file.getName());

                        //msgTxt.append("File Successfully Printed: " + file.getName() + "\n");

                    } catch (Exception e) {
                        System.out.println("Failed to print existing file: " + file.getName());
                        msgTxt.append("Failed to print existing file: " + file.getName() + "\n");

                        e.printStackTrace();
                    }
                }
            }
        }

        //this section of the code is responsible for watching the folder with the files for changes and printing new files as they are added
        Path folder = Paths.get(folderPath);
        WatchService watchService = FileSystems.getDefault().newWatchService();
        folder.register(watchService, StandardWatchEventKinds.ENTRY_CREATE);

        System.out.println("Watching for new files in: " + folderPath);
        msgTxt.append("Watching for new files in: " + folderPath + "\n");

        while (true) {
            WatchKey key = watchService.take();

            for (WatchEvent<?> event : key.pollEvents()) {
                WatchEvent.Kind<?> kind = event.kind();

                if (kind == StandardWatchEventKinds.ENTRY_CREATE) {
                    Path newFilePath = folder.resolve((Path) event.context());
                    File newFile = newFilePath.toFile();
                    Thread.sleep(1000); // Let file finish writing

                    String normalized = newFile.getName().trim().toLowerCase();

                    if (newFile.isFile() && isPrintable(newFile) && !printedFiles.contains(normalized)) {
                        try {
                            printFile(newFile);
                            printedFiles.add(normalized);
                            appendToLogFile(normalized);                            
                        } catch (Exception e) {
                            System.out.println("Failed to print new file: " + newFile.getName());
                            msgTxt.append("Failed to print new file: " + newFile.getName() + "\n");
                            e.printStackTrace();
                        }
                    }
                }
            }

            if (!key.reset()) {
                System.out.println("Watch key no longer valid.");
                msgTxt.append("Watch key no longer valid.\n");
                break;
            }
        }
    }

    private static void createDirectory(){
        try {
            String userDocs = new JFileChooser().getFileSystemView().getDefaultDirectory().getAbsolutePath();
            File autoPrintRFolder = new File(userDocs, "AutoPrintR");
            if (!autoPrintRFolder.exists()) {
                autoPrintRFolder.mkdirs();
            }
            File logFile = new File(autoPrintRFolder, "printed_files.txt");
            File directoryFile = new File(autoPrintRFolder, "printer_folder_directory.txt");

            if(!logFile.exists()){
                logFile.createNewFile();            
            }

            if(!directoryFile.exists()){
                directoryFile.createNewFile();
            }

            logFilePath = logFile.getAbsolutePath();
            printerFolder = directoryFile.getAbsolutePath();
        } catch (IOException ex) {
            Logger.getLogger(AutoPrintR.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private static void chooseFolderToWatch(){ 
        //this keeps asking the user to choose the directory when they say no
        boolean controller = false;

        String savedDir = readDirectory();
        if(savedDir != null){
            folderPath = savedDir.trim();
        }
        else{
            while(controller == false){
                JFileChooser chooser = new JFileChooser();
                chooser.setDialogTitle("Select Folder to Watch and Print From");
                chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                chooser.setAcceptAllFileFilterUsed(false);

                File selectedDirectory = null;
                if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                    selectedDirectory = chooser.getSelectedFile();
                } else {
                    System.out.println("No folder selected. Exiting.");
                    return;
                }

                String path = selectedDirectory.getAbsolutePath();
                System.out.println("Monitoring folder: " + path);
                msgTxt.append("Monitoring folder: " + path + "\n");
                int confirmation = JOptionPane.showConfirmDialog(null, "Are you sure you want to use: "+ path  +" as your printer folder directory?");
                if(confirmation == 0){
                    folderPath = selectedDirectory.getAbsolutePath();
                    saveDirectory(folderPath);
                    controller = true;
                }      
                else if(confirmation == 1){
                    JOptionPane.showMessageDialog(null, "Please select another folder.");
                }
                else{
                    System.exit(0);
                }
            }
        }
    }

    private static void saveDirectory(String dir){
        FileWriter fr = null;
        try {
            fr = new FileWriter(printerFolder);
            BufferedWriter br = new BufferedWriter(fr);
            br.write(dir.trim());
            br.close();
        } catch (IOException ex) {
            Logger.getLogger(AutoPrintR.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                if (fr != null) fr.close();
            } catch (IOException ex) {
                Logger.getLogger(AutoPrintR.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    private static String readDirectory(){
        String dir = "";
        FileReader fr = null;
        try {
            fr = new FileReader(printerFolder);
            BufferedReader br = new BufferedReader(fr);
            dir = br.readLine();
            br.close();
        } catch (IOException ex) {
            Logger.getLogger(AutoPrintR.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                if (fr != null) fr.close();
            } catch (IOException ex) {
                Logger.getLogger(AutoPrintR.class.getName()).log(Level.SEVERE, null, ex);
            }
        }        
        return dir;
    }

    // Main method to print each file
    private static void printFile(File file) throws Exception {
        String appInstall = installationDate();
        LocalDateTime installLdt = LocalDateTime.parse(appInstall, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        Instant installInstant = installLdt.atZone(ZoneId.systemDefault()).toInstant();

        // Retrieve file creation date
        String creationDateStr = getCreationDate(file);
        Instant fileCreationInstant = Instant.parse(creationDateStr);

        // Retrieve file modification date
        String modificationDateStr = getModificationDate(file);
        Instant fileModificationInstant = Instant.parse(modificationDateStr);

        // Determine if the file was created or modified after the application installation
        if (fileCreationInstant.isAfter(installInstant) || fileModificationInstant.isAfter(installInstant)) {
            Desktop.getDesktop().print(file);
            System.out.println("File Successfully Printed: " + file.getName());
            msgTxt.append("File Successfully Printed: " + file.getName() + "\n");
        }

        Thread.sleep(5000); // Wait a bit between print jobs
    }

    // Extracts file extension
    private static String getFileExtension(File file) {
        String name = file.getName();
        int dotIndex = name.lastIndexOf(".");
        return dotIndex != -1 ? name.substring(dotIndex + 1).toLowerCase() : "";
    }

    // Only allow known printable files
    private static boolean isPrintable(File file) {
        String ext = getFileExtension(file);
        return !file.getName().startsWith("~$") && (
            ext.equals("pdf") || isOfficeDocument(ext) ||
            ext.equals("txt") || ext.equals("jpg") || ext.equals("jpeg") ||
            ext.equals("png") || ext.equals("bmp")
        );
    }

    private static boolean isOfficeDocument(String ext) {
        return ext.equals("doc") || ext.equals("docx") ||
               ext.equals("xls") || ext.equals("xlsx") ||
               ext.equals("ppt") || ext.equals("pptx");
    }

    //method for reading the log file for printed file names
    private static Set<String> loadPrintedFiles() {
        Set<String> printed = new HashSet<>();
        File logFile = new File(logFilePath);
        if (logFile.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(logFile))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    printed.add(line.trim().toLowerCase());
                }
            } catch (IOException e) {
                System.out.println("Failed to read printed log.");
                msgTxt.append("Failed to read printed log.\n");
            }
        }
        return printed;
    }

    //method for writing a file name into the log file
    private static void appendToLogFile(String fileName) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(logFilePath, true))) {
            writer.write(fileName.trim().toLowerCase());
            writer.newLine();
        } catch (IOException e) {
            System.out.println("Failed to update log.");
            msgTxt.append("Failed to update log.\n");
        }
    }  

    @Override
    public void actionPerformed(ActionEvent e) {
        if(e.getSource() == chooseFolderBtn){            
        //this keeps asking the user to choose the directory when they say no
        boolean controller = false;
            while(controller == false){
                JFileChooser chooser = new JFileChooser();
                chooser.setDialogTitle("Select Folder to Watch and Print From");
                chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                chooser.setAcceptAllFileFilterUsed(false);

                File selectedDirectory = null;
                if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                    selectedDirectory = chooser.getSelectedFile();
                } else {
                    System.out.println("No folder selected. Exiting.");
                    return;
                }

                String path = selectedDirectory.getAbsolutePath();
                System.out.println("Monitoring folder: " + path);
                msgTxt.append("Monitoring folder: " + path + "\n");
                int confirmation = JOptionPane.showConfirmDialog(null, "Are you sure you want to use: "+ path  +" as your printer folder directory?");
                if(confirmation == 0){
                    folderPath = selectedDirectory.getAbsolutePath();
                    saveDirectory(folderPath);
                    controller = true;
                }      
                else if(confirmation == 1){
                    JOptionPane.showMessageDialog(null, "Please select another folder.");
                }
                else{
                    System.exit(0);
                }
 
            }
        }
    
    }
    
    private static String installationDate() throws IOException{
        String installDate;
        
        String userDocs = new JFileChooser().getFileSystemView().getDefaultDirectory().getAbsolutePath();
        
        File installFile = new File(userDocs+"\\AutoPrintR\\"+INSTALL_INFO_FILE);
        
        if(!installFile.exists()){
            installFile.createNewFile();
        }
        
        if (readInstallDate(installFile) == null) {
            installDate = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            
            FileWriter fr = new FileWriter(installFile);
            BufferedWriter wr = new BufferedWriter(fr);
            
            wr.write(installDate);
            
            //System.out.println("First install. Date recorded: " + installDate);
            wr.close();
            
        } else {
            installDate = new String(Files.readAllBytes(installFile.toPath()));
            //System.out.println("Installation date: " + readInstallDate(installFile));
        }
        //System.out.println(installFile.getAbsolutePath());
        return installDate;
    }
    
    private static String readInstallDate(File file){
        String installDate = "";
        try {
                        
            FileReader fr = new FileReader(file);
            BufferedReader br = new BufferedReader(fr);
        
            installDate = br.readLine();            
            //System.out.println("Saved: " + installDate);
            
            br.close();
            
        } catch (IOException ex) {
            Logger.getLogger(AutoPrintR.class.getName()).log(Level.SEVERE, null, ex);
        }
        return installDate;
    }
    
    private static String getCreationDate(File file) throws IOException{
        String creationDate;
        BasicFileAttributes attrs = Files.readAttributes(file.getAbsoluteFile().toPath(), BasicFileAttributes.class);
        FileTime lastCreationTime = attrs.creationTime();
        
        creationDate = lastCreationTime.toString();
        //System.out.println(lastAccessTime);
        return creationDate;
    }
    
    private static String getModificationDate(File file) throws IOException{
        String modificationDate;
        BasicFileAttributes attrs = Files.readAttributes(file.getAbsoluteFile().toPath(), BasicFileAttributes.class);
        FileTime lastModifTime = attrs.lastModifiedTime();
        
        modificationDate = lastModifTime.toString();
        //System.out.println(lastAccessTime);
        return modificationDate;
    }

}


//take the day the app was installed and use it as the start date for printing files. Only the files pasted/moved etc from the installation date shall be printed