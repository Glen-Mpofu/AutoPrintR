package autoprintr;

import javax.swing.*;
import javax.swing.text.DefaultCaret;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.net.URL;

public class AutoPrintRUI implements ActionListener {
    private final AutoPrintR app;
    private final JFrame gui;
    private final JTextArea msgTxt;
    private final JComboBox<Integer> copiesListBox;
    private final JButton chooseFolderBtn;
    private TrayIcon trayIcon;

    public AutoPrintRUI(AutoPrintR app, int defaultCopies, String defaultWatchFolder) {
        this.app = app;

        gui = new JFrame("AutoPrintR");
        gui.setSize(450, 480);
        gui.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        gui.setLocationRelativeTo(null);
        gui.setResizable(false);

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
        
        // Heading
        JLabel heading = new JLabel("Automate Your Prints", JLabel.CENTER);
        heading.setFont(new Font("SansSerif", Font.BOLD, 18));
        heading.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));

        // Message log
        msgTxt = new JTextArea(15, 30);
        msgTxt.setEditable(false);
        msgTxt.setLineWrap(true);
        msgTxt.setWrapStyleWord(true);
        JScrollPane scrollPane = new JScrollPane(msgTxt);
        DefaultCaret caret = (DefaultCaret) msgTxt.getCaret();
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);

        // Folder chooser
        chooseFolderBtn = new JButton("Choose/Change Folder");
        chooseFolderBtn.addActionListener(this);

        JTextArea descTxt = new JTextArea(4, 30);
        descTxt.setText("AutoPrintR automatically prints files copied/moved/edited/saved to a folder of your choice.");
        descTxt.setEditable(false);
        descTxt.setWrapStyleWord(true);
        descTxt.setLineWrap(true);

        JPanel centerPnl = new JPanel();
        centerPnl.add(scrollPane);
        centerPnl.add(chooseFolderBtn);
        centerPnl.add(descTxt);

        // Copies dropdown
        JPanel bottomPanel = new JPanel(new FlowLayout());
        Integer[] copies = {1,2,3,4,5,6,7,8,9,10};
        copiesListBox = new JComboBox<>(copies);
        copiesListBox.setSelectedItem(defaultCopies);
        copiesListBox.addActionListener(this);

        bottomPanel.add(new JLabel("Copies Per Document: "));
        bottomPanel.add(copiesListBox);

        //UI Icon
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
        
        gui.add(heading, BorderLayout.NORTH);
        gui.add(centerPnl, BorderLayout.CENTER);
        gui.add(bottomPanel, BorderLayout.SOUTH);

        setupTrayIcon();
    }

    public void show() {
        gui.setVisible(true);
    }

    public void logMessage(String msg) {
        msgTxt.append(msg + "\n");
    }

    private void setupTrayIcon() {
        if (!SystemTray.isSupported()) return;
        SystemTray tray = SystemTray.getSystemTray();
        Image image = Toolkit.getDefaultToolkit().getImage(getClass().getResource("/resources/AutoPrintR Logo Design.png"));

        PopupMenu popup = new PopupMenu();
        MenuItem showItem = new MenuItem("Show");
        showItem.addActionListener(e -> show());
        MenuItem exitItem = new MenuItem("Exit");
        exitItem.addActionListener(e -> {
            tray.remove(trayIcon);
            System.exit(0);
        });
        popup.add(showItem);
        popup.add(exitItem);

        trayIcon = new TrayIcon(image, "AutoPrintR", popup);
        trayIcon.setImageAutoSize(true);
        trayIcon.addActionListener(e -> show());

        try {
            tray.add(trayIcon);
        } catch (AWTException ignored) {}
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == chooseFolderBtn) {
            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("Select Folder to Watch");
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            if (chooser.showOpenDialog(gui) == JFileChooser.APPROVE_OPTION) {
                File selected = chooser.getSelectedFile();
                int confirm = JOptionPane.showConfirmDialog(gui, "Use this folder?\n" + selected.getAbsolutePath());
                if (confirm == 0) {
                    app.updateWatchFolder(selected.getAbsolutePath());
                }
            }
        } else if (e.getSource() == copiesListBox) {
            int copies = (int) copiesListBox.getSelectedItem();
            app.updateCopies(copies);
        }
    }
}
