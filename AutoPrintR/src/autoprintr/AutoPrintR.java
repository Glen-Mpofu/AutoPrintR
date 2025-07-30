package autoprintr;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.URL;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.logging.*;
import javax.swing.*;
import javax.swing.text.DefaultCaret;

public class AutoPrintR implements ActionListener {
    private JFrame gui;
    private JPanel mainPnl;
    private static JTextArea msgTxt;
    private static JTextArea descTxt;
    private JButton chooseFolderBtn;

    private static String folderPath;
    private static String logFilePath;
    private static String printerFolder;
    private static final String INSTALL_INFO_FILE = "installation_date.txt";

    public AutoPrintR() {
        gui = new JFrame("AutoPrintR");
        gui.setSize(450, 480);
        gui.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        gui.setLocationRelativeTo(null);
        gui.setResizable(false);

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

        JLabel heading = new JLabel("Automate Your Prints", JLabel.CENTER);
        heading.setFont(new Font("SansSerif", Font.BOLD, 18));
        heading.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));

        mainPnl = new JPanel(new BorderLayout());

        msgTxt = new JTextArea(15, 30);
        msgTxt.setEditable(false);
        msgTxt.setLineWrap(true);
        msgTxt.setWrapStyleWord(true);
        JScrollPane scrollPane = new JScrollPane(msgTxt);
        DefaultCaret caret = (DefaultCaret) msgTxt.getCaret();
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);

        descTxt = new JTextArea(4, 30);
        descTxt.setText("AutoPrintR automatically prints files copied to your selected folder.");
        descTxt.setEditable(false);
        descTxt.setWrapStyleWord(true);
        descTxt.setLineWrap(true);

        chooseFolderBtn = new JButton("Choose/Change Folder");
        chooseFolderBtn.addActionListener(this);

        JPanel centerPnl = new JPanel();
        centerPnl.add(scrollPane);
        centerPnl.add(chooseFolderBtn);
        centerPnl.add(descTxt);

        mainPnl.add(heading, BorderLayout.NORTH);
        mainPnl.add(centerPnl, BorderLayout.CENTER);
        gui.add(mainPnl);
        gui.setVisible(true);
    }

    public static void main(String[] args) throws Exception {
        new AutoPrintR();
        createDirectory();
        String installStr = installationDate().trim();
        LocalDateTime installTime = LocalDateTime.parse(installStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        Instant installInstant = installTime.atZone(ZoneId.systemDefault()).toInstant();

        chooseFolderToWatch();

        Set<String> printedFiles = loadPrintedFiles();
        File dir = new File(folderPath);
        File[] files = dir.listFiles();

        if (files != null) {
            for (File file : files) {
                String name = file.getName().trim().toLowerCase();
                if (file.isFile() && isPrintable(file) && !printedFiles.contains(name)) {
                    try {
                        printFileIfNew(file, installInstant);
                        printedFiles.add(name);
                        appendToLogFile(name);
                    } catch (Exception e) {
                        msgTxt.append("Failed to print existing file: " + file.getName() + "\n");
                    }
                }
            }
        }

        Path folder = Paths.get(folderPath);
        WatchService watchService = FileSystems.getDefault().newWatchService();
        folder.register(watchService, StandardWatchEventKinds.ENTRY_CREATE);
        msgTxt.append("Watching for new files in: " + folderPath + "\n");

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
                            appendToLogFile(name);
                        } catch (Exception e) {
                            msgTxt.append("Failed to print new file: " + file.getName() + "\n");
                        }
                    }
                }
            }
            if (!key.reset()) break;
        }
    }

    private static void printFileIfNew(File file, Instant installInstant) throws Exception {
        try {
            BasicFileAttributes attrs = Files.readAttributes(file.toPath(), BasicFileAttributes.class);
            Instant created = attrs.creationTime().toInstant();
            Instant modified = attrs.lastModifiedTime().toInstant();

            if (created.isAfter(installInstant) || modified.isAfter(installInstant)) {
                Desktop.getDesktop().print(file);
                msgTxt.append("Printed: " + file.getName() + "\n");
                Thread.sleep(5000);
            } else {
                msgTxt.append("Skipped (too old): " + file.getName() + "\n");
            }
        } catch (Exception e) {
            msgTxt.append("Error checking date, printing anyway: " + file.getName() + "\n");
            Desktop.getDesktop().print(file);
        }
    }

    private static void createDirectory() {
        try {
            String userDocs = new JFileChooser().getFileSystemView().getDefaultDirectory().getAbsolutePath();
            File baseFolder = new File(userDocs, "AutoPrintR");
            if (!baseFolder.exists()) baseFolder.mkdirs();

            File logFile = new File(baseFolder, "printed_files.txt");
            File folderFile = new File(baseFolder, "printer_folder_directory.txt");
            if (!logFile.exists()) logFile.createNewFile();
            if (!folderFile.exists()) folderFile.createNewFile();

            logFilePath = logFile.getAbsolutePath();
            printerFolder = folderFile.getAbsolutePath();
        } catch (IOException e) {
            Logger.getLogger(AutoPrintR.class.getName()).log(Level.SEVERE, null, e);
        }
    }

    private static String installationDate() throws IOException {
        String userDocs = new JFileChooser().getFileSystemView().getDefaultDirectory().getAbsolutePath();
        File file = new File(userDocs + File.separator + "AutoPrintR" + File.separator + INSTALL_INFO_FILE);
        if (!file.exists()) {
            String now = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            try (BufferedWriter w = new BufferedWriter(new FileWriter(file))) {
                w.write(now);
            }
            return now;
        }
        return Files.readAllLines(file.toPath()).get(0);
    }

    private static void chooseFolderToWatch() {
        String saved = readDirectory();
        if (saved != null && !saved.trim().isEmpty()) {
            folderPath = saved.trim();
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
                    folderPath = selected.getAbsolutePath();
                    saveDirectory(folderPath);
                    chosen = true;
                }
            } else {
                System.exit(0);
            }
        }
    }

    private static void saveDirectory(String dir) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(printerFolder))) {
            bw.write(dir.trim());
        } catch (IOException e) {
            Logger.getLogger(AutoPrintR.class.getName()).log(Level.SEVERE, null, e);
        }
    }

    private static String readDirectory() {
        try (BufferedReader br = new BufferedReader(new FileReader(printerFolder))) {
            return br.readLine();
        } catch (IOException e) {
            return null;
        }
    }

    private static boolean isPrintable(File file) {
        String name = file.getName().toLowerCase();
        return !name.startsWith("~$") && Set.of("pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "jpg", "jpeg", "png", "bmp").contains(getFileExtension(file));
    }

    private static String getFileExtension(File file) {
        int i = file.getName().lastIndexOf(".");
        return i >= 0 ? file.getName().substring(i + 1).toLowerCase() : "";
    }

    private static Set<String> loadPrintedFiles() {
        Set<String> printed = new HashSet<>();
        try (BufferedReader br = new BufferedReader(new FileReader(logFilePath))) {
            String line;
            while ((line = br.readLine()) != null) printed.add(line.trim().toLowerCase());
        } catch (IOException ignored) {}
        return printed;
    }

    private static void appendToLogFile(String fileName) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(logFilePath, true))) {
            writer.write(fileName);
            writer.newLine();
        } catch (IOException ignored) {}
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == chooseFolderBtn) chooseFolderToWatch();
    }
}
