package com.whistleup.backend.service;

import com.lowagie.text.Document;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import com.whistleup.backend.entity.Maintenance;
import org.springframework.stereotype.Service;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Month;

@Service
public class InvoiceService {

    private static final String BASE_DIR = "invoices";

    public String generateInvoice(Maintenance m) {

        try {
            Files.createDirectories(Paths.get(BASE_DIR));

            String fileName = String.format(
                    "invoice_%s_%d_%02d.pdf",
                    m.getProfileId(),
                    m.getMaintenanceYear(),
                    m.getMaintenanceMonth()
            );

            Path invoicePath = Paths.get(BASE_DIR, fileName);

            Document document = new Document();
            PdfWriter.getInstance(document, new FileOutputStream(invoicePath.toFile()));

            document.open();

            document.add(new Paragraph("Maintenance Invoice"));
            document.add(new Paragraph(" "));
            document.add(new Paragraph("Username   : " + m.getProfileId()));
            document.add(new Paragraph("Month      : " + Month.of(m.getMaintenanceMonth())));
            document.add(new Paragraph("Year       : " + m.getMaintenanceYear()));
            document.add(new Paragraph("Amount     : ₹ " + m.getAmount()));
            document.add(new Paragraph("Paid Date  : " + m.getPaidDate()));
            document.add(new Paragraph("Status     : PAID"));

            document.close();

            return invoicePath.toString();

        } catch (IOException e) {
            throw new RuntimeException("Failed to generate invoice", e);
        }
    }
}
