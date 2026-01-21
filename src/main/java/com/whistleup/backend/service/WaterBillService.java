package com.whistleup.backend.service;

import com.whistleup.backend.entity.WaterBill;
import com.whistleup.backend.entity.WaterReading;
import com.whistleup.backend.repository.WaterBillRepository;
import com.whistleup.backend.resource.WaterBillRequest;
import com.whistleup.backend.resource.WaterReadingRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WaterBillService {

    private final WaterBillRepository waterBillRepository;

    public void createWaterBill(WaterBillRequest request) {

        WaterBill bill = new WaterBill();
        bill.setBuildingId(request.getBuildingId());
        bill.setYear(request.getYear());
        bill.setMonth(request.getMonth());
        bill.setLiftCurrentBill(request.getLiftCurrentBill());
        bill.setCommonCurrentBill(request.getCommonCurrentBill());
        bill.setGarbage(request.getGarbage());
        bill.setWatchmanSalary(request.getWatchmanSalary());
        bill.setMiscellaneousExpenses(request.getMiscellaneousExpenses());
        bill.setLiftMotorMaintenance(request.getLiftMotorMaintenance());
        bill.setOtherExpenses(request.getOtherExpenses());

        List<WaterReading> readings = request.getWaterReadings()
                .stream()
                .map(r -> mapToEntity(r, bill))
                .toList();

        bill.setWaterReadings(readings);

        waterBillRepository.save(bill);
    }

    private WaterReading mapToEntity(
            WaterReadingRequest request,
            WaterBill bill
    ) {
        WaterReading reading = new WaterReading();
        reading.setFlatNumber(request.getFlatNumber());
        reading.setMeterReading(request.getMeterReading());
        reading.setAmount(request.getAmount());
        reading.setWaterBill(bill);
        return reading;
    }
}
