package com.whistleup.backend.service;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.whistleup.backend.entity.BuildingDetails;
import com.whistleup.backend.entity.Maintenance;
import com.whistleup.backend.entity.Profile;
import com.whistleup.backend.repository.BuildingDetailsRepository;
import com.whistleup.backend.repository.ProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
@RequiredArgsConstructor
public class InvoiceService {

    private static final String BASE_DIR = "invoices";

    private static final Font FONT_TITLE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
    private static final Font FONT_SUBTITLE = FontFactory.getFont(FontFactory.HELVETICA, 12);
    private static final Font FONT_LABEL = FontFactory.getFont(FontFactory.HELVETICA, 10);
    private static final Font FONT_BODY = FontFactory.getFont(FontFactory.HELVETICA, 12);
    private static final Font FONT_BODY_BOLD = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
    private static final Font FONT_PAID = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11);
    private static final Font FONT_AMOUNT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 26);
    private static final Font FONT_FOOTER = FontFactory.getFont(FontFactory.HELVETICA, 10);

    private final ProfileRepository profileRepository;
    private final BuildingDetailsRepository buildingDetailsRepository;

    public String generateInvoice(Maintenance m) {
        try {
            Files.createDirectories(Paths.get(BASE_DIR));

            String fileName = String.format(
                    "invoice_%s_%d_%02d.pdf",
                    m.getProfileId(),
                    m.getMaintenanceYear(),
                    m.getMaintenanceMonth());

            Path invoicePath = Paths.get(BASE_DIR, fileName);

            Profile profile = profileRepository.findById(m.getProfileId()).orElse(null);
            BuildingDetails building = resolveBuilding(m.getBuildingId());

            String buildingName = building != null ? safe(building.getBuildingName(), "Greenview Society")
                    : "Greenview Society";
            String residentName = profile != null ? safe(profile.getName(), "Resident") : "Resident";
            String flatNo = profile != null ? safe(profile.getFlatNo(), "N/A") : "N/A";
            String floor = profile != null ? safe(profile.getFloor(), "") : "";
            String periodLabel = monthLabel(m.getMaintenanceMonth()) + " " + m.getMaintenanceYear();
            String receiptId = "RCP-" + m.getMaintenanceYear() + "-" + String.format("%02d", m.getMaintenanceMonth())
                    + "-" + flatNo.toUpperCase(Locale.ENGLISH).replaceAll("\\s+", "");
            String floorText = floor.isBlank() ? "" : ", " + floor + " Floor";
            String residentLine = residentName + " | Flat " + flatNo + floorText;

            Map<String, BigDecimal> breakdown = buildBreakdownRows(m);
            BigDecimal waterAmount = amountOrZero(m.getWaterAmount());
            BigDecimal shareTotal = breakdown.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal totalPaid = amountOrZero(m.getAmount());
            String txnId = "TXN" + m.getMaintenanceYear() + String.format("%02d", m.getMaintenanceMonth()) + m.getId();

            Document document = new Document(PageSize.A4, 30, 30, 28, 28);
            PdfWriter.getInstance(document, new FileOutputStream(invoicePath.toFile()));
            document.open();

            PdfPTable headerTable = new PdfPTable(new float[] { 4f, 1f });
            headerTable.setWidthPercentage(100);

            Paragraph societyParagraph = new Paragraph(buildingName, FONT_TITLE);
            Paragraph subtitleParagraph = new Paragraph("Maintenance Receipt of " + periodLabel, FONT_SUBTITLE);
            subtitleParagraph.setSpacingBefore(3f);
            PdfPCell leftHeaderCell = cellNoBorder();
            leftHeaderCell.addElement(societyParagraph);
            leftHeaderCell.addElement(subtitleParagraph);
            headerTable.addCell(leftHeaderCell);

            PdfPCell paidCell = cellNoBorder();
            paidCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            Paragraph paidBadge = new Paragraph("Paid", FONT_PAID);
            paidBadge.setAlignment(Element.ALIGN_RIGHT);
            paidCell.addElement(paidBadge);
            headerTable.addCell(paidCell);

            document.add(headerTable);

            Paragraph receiptIdParagraph = new Paragraph("#" + receiptId, FONT_LABEL);
            receiptIdParagraph.setAlignment(Element.ALIGN_RIGHT);
            receiptIdParagraph.setSpacingAfter(16f);
            document.add(receiptIdParagraph);

            PdfPTable detailsTable = new PdfPTable(new float[] { 1f, 1f });
            detailsTable.setWidthPercentage(100);
            PdfPCell unitCell = cellNoBorder();
            unitCell.addElement(new Paragraph("Unit", FONT_LABEL));
            unitCell.addElement(new Paragraph(flatNo, FONT_BODY_BOLD));
            unitCell.addElement(new Paragraph(residentLine, FONT_BODY));
            detailsTable.addCell(unitCell);

            PdfPCell amountCell = cellNoBorder();
            amountCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            amountCell.addElement(alignedParagraph("Amount paid", FONT_LABEL, Element.ALIGN_RIGHT));
            amountCell.addElement(alignedParagraph(currency(totalPaid), FONT_AMOUNT, Element.ALIGN_RIGHT));
            detailsTable.addCell(amountCell);
            detailsTable.setSpacingAfter(14f);
            document.add(detailsTable);

            document.add(new Paragraph("Shared expense breakdown", FONT_LABEL));
            PdfPTable breakdownTable = new PdfPTable(new float[] { 4f, 1f });
            breakdownTable.setWidthPercentage(100);
            breakdownTable.setSpacingBefore(6f);
            for (Map.Entry<String, BigDecimal> row : breakdown.entrySet()) {
                breakdownTable.addCell(labelCell("• " + row.getKey()));
                breakdownTable.addCell(amountValueCell(currency(row.getValue())));
            }
            document.add(breakdownTable);

            document.add(spacer(8f));
            // document.add(new Paragraph("Bill summary", FONT_LABEL));
            // PdfPTable summaryTable = new PdfPTable(new float[] { 4f, 1f });
            // summaryTable.setWidthPercentage(100);
            // summaryTable.setSpacingBefore(6f);
            // summaryTable.addCell(labelCell("Your share of expenses"));
            // summaryTable.addCell(amountValueCell(currency(shareTotal)));
            // summaryTable.addCell(labelCell("Water charges"));
            // summaryTable.addCell(amountValueCell(currency(waterAmount)));
            // document.add(summaryTable);

            document.add(spacer(6f));
            PdfPTable totalTable = new PdfPTable(new float[] { 4f, 1f });
            totalTable.setWidthPercentage(100);
            totalTable.setSpacingBefore(2f);
            totalTable.addCell(labelCellBold("Total paid"));
            totalTable.addCell(amountValueCellBold(currency(totalPaid)));
            document.add(totalTable);

            document.add(spacer(12f));
            document.add(new Paragraph("Payment details", FONT_LABEL));
            PdfPTable paymentTable = new PdfPTable(new float[] { 1f, 1f });
            paymentTable.setWidthPercentage(100);
            paymentTable.setSpacingBefore(6f);
            PdfPCell paymentMethodCell = cellNoBorder();
            paymentMethodCell.addElement(new Paragraph("Method", FONT_LABEL));
            paymentMethodCell.addElement(new Paragraph("UPI", FONT_BODY));
            paymentTable.addCell(paymentMethodCell);
            PdfPCell transactionCell = cellNoBorder();
            transactionCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            transactionCell.addElement(alignedParagraph("Transaction ID", FONT_LABEL, Element.ALIGN_RIGHT));
            transactionCell.addElement(alignedParagraph(txnId, FONT_BODY, Element.ALIGN_RIGHT));
            paymentTable.addCell(transactionCell);
            document.add(paymentTable);

            document.add(spacer(42f));
            Paragraph footerTitle = new Paragraph(buildingName + " Association", FONT_BODY_BOLD);
            footerTitle.setAlignment(Element.ALIGN_CENTER);
            document.add(footerTitle);
            Paragraph footerInfo = new Paragraph(
                    "This is a computer-generated receipt and does not require a signature.", FONT_FOOTER);
            footerInfo.setAlignment(Element.ALIGN_CENTER);
            footerInfo.setSpacingBefore(4f);

            footerInfo = new Paragraph("Powered by Nestiti.", FONT_FOOTER);
            footerInfo.setAlignment(Element.ALIGN_CENTER);
            footerInfo.setSpacingBefore(4f);
            document.add(footerInfo);

            document.close();

            return invoicePath.toString();

        } catch (IOException e) {
            throw new RuntimeException("Failed to generate invoice", e);
        }
    }

    private BuildingDetails resolveBuilding(String buildingId) {
        if (buildingId == null || buildingId.isBlank())
            return null;
        try {
            return buildingDetailsRepository.findById(Long.parseLong(buildingId.trim())).orElse(null);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private Map<String, BigDecimal> buildBreakdownRows(Maintenance m) {
        Map<String, BigDecimal> rows = new LinkedHashMap<>();
        addRow(rows, "Watchman salary", m.getWatchmanSalary());
        addRow(rows, "Lift maintenance", m.getLiftMaintenance());
        addRow(rows, "Common area electricity", m.getElectricityCommon());
        addRow(rows, "Garbage collection", m.getGarbageCollection());
        addRow(rows, "Motor maintenance", m.getMotorPump());
        addRow(rows, "Miscellaneous", m.getMiscellaneous());
        if (m.getCustomExpenses() != null) {
            for (Map.Entry<String, BigDecimal> entry : m.getCustomExpenses().entrySet()) {
                addRow(rows, safe(entry.getKey(), "Other expense"), entry.getValue());
            }
        }
        return rows;
    }

    private void addRow(Map<String, BigDecimal> rows, String label, BigDecimal amount) {
        BigDecimal value = amountOrZero(amount);
        if (value.compareTo(BigDecimal.ZERO) > 0) {
            rows.put(label, value);
        }
    }

    private BigDecimal amountOrZero(BigDecimal amount) {
        return amount == null ? BigDecimal.ZERO : amount;
    }

    private String monthLabel(Integer month) {
        if (month == null || month < 1 || month > 12) {
            return "Unknown";
        }
        return Month.of(month).getDisplayName(TextStyle.FULL, Locale.ENGLISH);
    }

    private String currency(BigDecimal amount) {
        DecimalFormat format = new DecimalFormat("#,##,##0.##");
        return "₹" + format.format(amountOrZero(amount));
    }

    private String safe(String value, String fallback) {
        if (value == null)
            return fallback;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? fallback : trimmed;
    }

    private Paragraph alignedParagraph(String text, Font font, int alignment) {
        Paragraph paragraph = new Paragraph(text, font);
        paragraph.setAlignment(alignment);
        return paragraph;
    }

    private Paragraph spacer(float spacingAfter) {
        Paragraph paragraph = new Paragraph(" ");
        paragraph.setSpacingAfter(spacingAfter);
        return paragraph;
    }

    private PdfPCell cellNoBorder() {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPadding(0f);
        return cell;
    }

    private PdfPCell labelCell(String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, FONT_BODY));
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPaddingBottom(5f);
        return cell;
    }

    private PdfPCell amountValueCell(String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, FONT_BODY_BOLD));
        cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPaddingBottom(5f);
        return cell;
    }

    private PdfPCell labelCellBold(String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, FONT_BODY_BOLD));
        cell.setBorder(Rectangle.TOP);
        cell.setPaddingTop(8f);
        cell.setPaddingBottom(3f);
        return cell;
    }

    private PdfPCell amountValueCellBold(String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, FONT_AMOUNT));
        cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        cell.setBorder(Rectangle.TOP);
        cell.setPaddingTop(4f);
        cell.setPaddingBottom(0f);
        return cell;
    }
}
