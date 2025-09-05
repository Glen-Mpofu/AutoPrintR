package autoprintr;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.printing.PDFPrintable;
import org.apache.pdfbox.printing.Scaling;

import javax.imageio.ImageIO;
import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;

public class PrintManager {

    private int copiesPerDocument;
    private final LogManager logManager;
    private final AutoPrintRUI ui;

    public PrintManager(int copies, LogManager logManager, AutoPrintRUI ui) {
        this.copiesPerDocument = copies;
        this.logManager = logManager;
        this.ui = ui;
    }

    public void setCopiesPerDocument(int copies) {
        this.copiesPerDocument = copies;
    }

    public void printFileIfNew(File file, Instant installInstant) throws Exception {
        BasicFileAttributes attrs = Files.readAttributes(file.toPath(), BasicFileAttributes.class);
        Instant created = attrs.creationTime().toInstant();
        Instant modified = attrs.lastModifiedTime().toInstant();

        if (!(created.isAfter(installInstant) || modified.isAfter(installInstant))) {
            ui.logMessage("Skipped (too old): " + file.getName());
            return;
        }

        String ext = getFileExtension(file);
        String symbol = file.getName().substring(0,2);
        //System.out.println(symbol);
        
        if(!ext.equals("tmp") && !symbol.equals("~$")){
            if (ext.equals("pdf")) {
                printPDF(file);
            } else if (ext.equals("doc") || ext.equals("docx") ||
                       ext.equals("xls") || ext.equals("xlsx") ||
                       ext.equals("ppt") || ext.equals("pptx")) {
                printOffice(file);
            } else if (ext.equals("txt")) {
                printTxt(file);
            } else if (ext.equals("jpg") || ext.equals("jpeg") ||
                       ext.equals("png") || ext.equals("bmp")) {
                printImage(file);
            }else {
                fallbackPrint(file);
            }
        }
    }

    private void printPDF(File file) throws IOException, PrinterException {
        try (PDDocument document = PDDocument.load(file)) {
            PrintService printer = PrintServiceLookup.lookupDefaultPrintService();
            if (printer == null) throw new IOException("No default printer found.");

            PrinterJob job = PrinterJob.getPrinterJob();
            job.setPrintService(printer);
            job.setPrintable(new PDFPrintable(document, Scaling.SHRINK_TO_FIT));

            for (int i = 0; i < copiesPerDocument; i++) {
                job.print();
            }

            logManager.logPrinted(file.getName());
            ui.logMessage("PDF printed: " + file.getName());
        }
    }

    private void printOffice(File file) throws IOException {
        String basePath = System.getProperty("user.dir");
        File script = new File(basePath + "/app/tools/print_office.ps1");
        
        if (!script.exists()) {
            fallbackPrint(file);
            throw new IOException("PowerShell script not found.");
        }

        for (int i = 0; i < copiesPerDocument; i++) {
            Runtime.getRuntime().exec("powershell.exe -ExecutionPolicy Bypass -File \"" +
                    script.getAbsolutePath() + "\" \"" + file.getAbsolutePath() + "\"");
        }
        logManager.logPrinted(file.getName());
        ui.logMessage("Office document printed: " + file.getName());
    }

    private void printTxt(File file) throws IOException {
        for (int i = 0; i < copiesPerDocument; i++) {
            Desktop.getDesktop().print(file);
        }
        logManager.logPrinted(file.getName());
        ui.logMessage("TXT file printed: " + file.getName());
    }

    private void printImage(File file) throws Exception {
        BufferedImage image = ImageIO.read(file);
        PrintService printer = PrintServiceLookup.lookupDefaultPrintService();
        if (printer == null) throw new IOException("No default printer found.");

        PrinterJob job = PrinterJob.getPrinterJob();
        job.setPrintService(printer);
        job.setPrintable((g, pf, pageIndex) -> {
            if (pageIndex > 0) return Printable.NO_SUCH_PAGE;
            g.drawImage(image, 100, 100, null);
            return Printable.PAGE_EXISTS;
        });

        for (int i = 0; i < copiesPerDocument; i++) {
            job.print();
        }
        logManager.logPrinted(file.getName());
        ui.logMessage("Image printed: " + file.getName());
    }

    private void fallbackPrint(File file) throws IOException {
        for (int i = 0; i < copiesPerDocument; i++) {
            Desktop.getDesktop().print(file);
        }
        logManager.logPrinted(file.getName());
        ui.logMessage("Fallback printer activated: " + file.getName());
    }

    private static String getFileExtension(File file) {
        String name = file.getName();
        int lastDot = name.lastIndexOf('.');
        return lastDot == -1 ? "" : name.substring(lastDot + 1).toLowerCase();
    }
}
