package com.whistleup.backend.service;

import com.lowagie.text.Document;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import com.whistleup.backend.entity.Ledger;
import com.whistleup.backend.entity.LedgerItem;
import com.whistleup.backend.repository.LedgerRepository;
import com.whistleup.backend.resource.*;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class LedgerServiceImpl implements LedgerService {

    private final LedgerRepository ledgerRepository;

    public LedgerServiceImpl(LedgerRepository ledgerRepository) {
        this.ledgerRepository = ledgerRepository;
    }

    @Override
    public LedgerResponse createLedger(CreateLedgerRequest request) {
        Ledger ledger = new Ledger();
        ledger.setYear(request.getYear());
        ledger.setMonth(request.getMonth());
        ledger.setTotalFlats(request.getTotalFlats());
        ledger.setCreatedAt(LocalDateTime.now());
        ledger.setBuildingId(request.getBuildingId());
        mapItems(request.getItems(), ledger);

        calculateTotals(ledger);

        return toResponse(ledgerRepository.save(ledger));
    }

    @Override
    public LedgerResponse getLedgerByYearAndMonth(int year, String month) {
        Ledger ledger = ledgerRepository
                .findByYearAndMonth(year, month)
                .orElseThrow();

        return toResponse(ledger);
    }

    @Override
    public LedgerResponse updateLedger(Long ledgerId, UpdateLedgerRequest request) {
        Ledger ledger = ledgerRepository.findById(ledgerId).orElseThrow();

        ledger.getItems().clear();
        mapItems(request.getItems(), ledger);
        calculateTotals(ledger);
        ledger.setUpdatedAt(LocalDateTime.now());
        ledger.setBuildingId(request.getBuildingId());

        return toResponse(ledger);
    }

    @Override
    public byte[] generateLedgerPdf(Long ledgerId) {
        Ledger ledger = ledgerRepository.findByIdWithItems(ledgerId).orElseThrow();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document();

        PdfWriter.getInstance(document, out);
        document.open();

        document.add(new Paragraph(ledger.getMonth() + " " + ledger.getYear()));
        document.add(new Paragraph(" "));

        ledger.getItems().forEach(item ->
                document.add(new Paragraph(item.getName() + " : ₹" + item.getAmount()))
        );

        document.add(new Paragraph(" "));
        document.add(new Paragraph("Total Amount: ₹" + ledger.getTotalAmount()));
        document.add(new Paragraph("Total Flats: " + ledger.getTotalFlats()));
        document.add(new Paragraph("Per Flat: ₹" + ledger.getPerFlatAmount()));

        document.close();
        return out.toByteArray();
    }

    private void mapItems(List<LedgerItemRequest> items, Ledger ledger) {
        for (LedgerItemRequest req : items) {
            LedgerItem item = new LedgerItem();
            item.setName(req.getName());
            item.setAmount(req.getAmount());
            item.setLedger(ledger);
            ledger.getItems().add(item);
        }
    }

    private void calculateTotals(Ledger ledger) {
        double total = ledger.getItems()
                .stream()
                .mapToDouble(LedgerItem::getAmount)
                .sum();

        ledger.setTotalAmount(total);
        ledger.setPerFlatAmount(
                ledger.getTotalFlats() == 0 ? 0 : total / ledger.getTotalFlats()
        );
    }

    private LedgerResponse toResponse(Ledger ledger) {
        List<LedgerItemResponse> items = ledger.getItems()
                .stream()
                .map(i -> new LedgerItemResponse(i.getId(), i.getName(), i.getAmount()))
                .toList();

        LedgerResponse ledgerResponse = new LedgerResponse(
                ledger.getId(),
                ledger.getYear(),
                ledger.getMonth(),
                ledger.getTotalAmount(),
                ledger.getTotalFlats(),
                ledger.getPerFlatAmount(),
                items
        );
        ledgerResponse.setBuildingId(ledger.getBuildingId());
        return ledgerResponse;
    }
}
