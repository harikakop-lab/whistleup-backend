package com.whistleup.backend.service;

import com.whistleup.backend.resource.CreateLedgerRequest;
import com.whistleup.backend.resource.LedgerResponse;
import com.whistleup.backend.resource.UpdateLedgerRequest;

public interface LedgerService {

    LedgerResponse createLedger(CreateLedgerRequest request);

    LedgerResponse getLedgerByYearAndMonth(int year, String month);

    LedgerResponse getLedgerByYearAndMonthAndBuilding(int year, String month, String buildingId);

    LedgerResponse updateLedger(Long ledgerId, UpdateLedgerRequest request);

    byte[] generateLedgerPdf(Long ledgerId);
}
