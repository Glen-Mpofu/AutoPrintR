package silentprintr;

import javax.imageio.ImageIO;
import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import javax.swing.*;
import javax.swing.text.DefaultCaret;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.awt.print.Printable;
import java.awt.print.PrinterJob;
import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class SilentPrintR implements ActionListener {

    private JFrame gui;
    private JPanel mainPnl;
    private static JTextArea logArea;
    private static JTextArea descTxt;
    private JButton chooseFolderBtn;

    private static String folderPath;
    private static String printerFolder;
    private static final String INSTALL_INFO_FILE = "installation_date.txt";
    private static final Set<String> SUPPORTED = Set.of("pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "jpg", "jpeg", "png", "bmp");

    public SilentPrintR() {
        gui = new JFrame("SilentPrintR");
        gui.setSize(500, 450);
        gui.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        gui.setLocationRelativeTo(null);
        gui.setResizable(false);

        JLabel heading = new JLabel("Silent Auto Printer", JLabel.CENTER);
        heading.setFont(new Font("SansSerif", Font.BOLD, 18));
        heading.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));

        mainPnl = new JPanel(new BorderLayout());

        logArea = new JTextArea(15, 30);
        logArea.setEditable(false);
        logArea.setLineWrap(true);
        logArea.setWrapStyleWord(true);
        JScrollPane scrollPane = new JScrollPane(logArea);
        DefaultCaret caret = (DefaultCaret) logArea.getCaret();
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);

        descTxt = new JTextArea(4, 30);
        descTxt.setText("Watches a folder and silently prints supported files automatically.");
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
        new SilentPrintR();
        createDirectory();
        chooseFolderToWatch();

        File dir = new File(folderPath);
        Path folder = Paths.get(folderPath);
        WatchService watchService = FileSystems.getDefault().newWatchService();
        folder.register(watchService, StandardWatchEventKinds.ENTRY_CREATE);
        log("Watching for new files in: " + folderPath);

        while (true) {
            WatchKey key = watchService.take();
            for (WatchEvent<?> event : key.pollEvents()) {
                if (event.kind() == StandardWatchEventKinds.ENTRY_CREATE) {
                    Path newPath = folder.resolve((Path) event.context());
                    File file = newPath.toFile();
                    Thread.sleep(1000);
                    if (file.isFile() && isPrintable(file)) {
                        new Thread(() -> printSilently(file)).start();
                    }
                }
            }
            if (!key.reset()) break;
        }
    }

    private static void printSilently(File file) {
        String ext = getFileExtension(file);
        try {
            switch (ext) {
                case "pdf":
                    printWithSumatra(file);
                    break;
                case "doc": case "docx": case "xls": case "xlsx": case "ppt": case "pptx":
                    printWithOffice(file);
                    break;
                case "txt":
                    printText(file);
                    break;
                case "jpg": case "jpeg": case "png": case "bmp":
                    printImage(file);
                    break;
                default:
                    log("Unsupported extension: " + ext);
            }
        } catch (Exception e) {
            log("❌ Error printing: " + file.getName());
            log(e.getMessage());
        }
    }

    private static void printWithSumatra(File file) throws IOException {
        File sumatra = new File("tools/SumatraPDF.exe");
        if (!sumatra.exists()) throw new IOException("SumatraPDF not found.");
        Runtime.getRuntime().exec("\"" + sumatra.getAbsolutePath() + "\" -print-to-default \"" + file.getAbsolutePath() + "\"");
        log("✅ PDF sent to printer: " + file.getName());
    }

    private static void printWithOffice(File file) throws IOException {
        File script = new File("tools/print_office.ps1");
        if (!script.exists()) throw new IOException("PowerShell script not found.");
        Runtime.getRuntime().exec("powershell.exe -ExecutionPolicy Bypass -File \"" + script.getAbsolutePath() + "\" \"" + file.getAbsolutePath() + "\"");
        log("✅ Office document sent to printer: " + file.getName());
    }

    private static void printText(File file) throws IOException {
        Runtime.getRuntime().exec("notepad.exe /p \"" + file.getAbsolutePath() + "\"");
        log("✅ TXT sent to printer: " + file.getName());
    }

    private static void printImage(File file) throws Exception {
        BufferedImage image = ImageIO.read(file);
        PrintService defaultPrinter = PrintServiceLookup.lookupDefaultPrintService();
        if (defaultPrinter == null) throw new IOException("No default printer found.");
        PrinterJob job = PrinterJob.getPrinterJob();
        job.setPrintService(defaultPrinter);
        job.setPrintable((g, pf, pageIndex) -> {
            if (pageIndex > 0) return Printable.NO_SUCH_PAGE;
            g.drawImage(image, 100, 100, null);
            return Printable.PAGE_EXISTS;
        });
        job.print();
        log("✅ Image printed: " + file.getName());
    }

    private static boolean isPrintable(File file) {
        return SUPPORTED.contains(getFileExtension(file));
    }

    private static String getFileExtension(File file) {
        int i = file.getName().lastIndexOf(".");
        return i >= 0 ? file.getName().substring(i + 1).toLowerCase() : "";
    }

    private static void createDirectory() {
        try {
            String userDocs = new JFileChooser().getFileSystemView().getDefaultDirectory().getAbsolutePath();
            File baseFolder = new File(userDocs, "SilentPrintR");
            if (!baseFolder.exists()) baseFolder.mkdirs();
            File folderFile = new File(baseFolder, "printer_folder_directory.txt");
            if (!folderFile.exists()) folderFile.createNewFile();
            printerFolder = folderFile.getAbsolutePath();
        } catch (IOException e) {
            log("❌ Failed to create settings directory.");
        }
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
            log("❌ Could not save folder path.");
        }
    }

    private static String readDirectory() {
        try (BufferedReader br = new BufferedReader(new FileReader(printerFolder))) {
            return br.readLine();
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == chooseFolderBtn) chooseFolderToWatch();
    }

    private static void log(String msg) {
        SwingUtilities.invokeLater(() -> logArea.append(msg + "\n"));
    }
}
