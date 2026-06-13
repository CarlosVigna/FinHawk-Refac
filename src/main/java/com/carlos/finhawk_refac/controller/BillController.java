package com.carlos.finhawk_refac.controller;

import com.carlos.finhawk_refac.dto.response.BillResponseDTO;
import com.carlos.finhawk_refac.dto.request.BillRequestDTO;
import com.carlos.finhawk_refac.dto.request.BillStatusUpdateRequest;
import com.carlos.finhawk_refac.service.BillService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import com.carlos.finhawk_refac.dto.response.DashboardSummaryDTO;

@RestController
@RequestMapping("/bill")
public class BillController {

    private final BillService billService;

    public BillController(BillService billService) {
        this.billService = billService;
    }

    @PostMapping
    public ResponseEntity<BillResponseDTO> create(@RequestBody BillRequestDTO dto) {
        BillResponseDTO newBill = billService.create(dto);
        return ResponseEntity.ok(newBill);
    }

    @PutMapping("/{id}")
    public ResponseEntity<BillResponseDTO> update(@PathVariable Long id, @RequestBody BillRequestDTO dto) {
        BillResponseDTO updated = billService.update(id, dto);
        return ResponseEntity.ok(updated);
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<BillResponseDTO> updateStatus(@PathVariable Long id, @RequestBody BillStatusUpdateRequest dto) {
        BillResponseDTO updated = billService.updateStatus(id, dto.status());
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/account/{accountId}")
    public ResponseEntity<List<BillResponseDTO>> getAllByAccount(@PathVariable Long accountId) {
        List<BillResponseDTO> bills = billService.getAllByAccountId(accountId);
        return ResponseEntity.ok(bills);
    }

    @GetMapping("/dashboard/consolidated")
    public ResponseEntity<DashboardSummaryDTO> getConsolidated() {
        DashboardSummaryDTO dto = billService.getConsolidatedSummary();
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/account/{accountId}/period")
    public ResponseEntity<List<BillResponseDTO>> getAllByAccountAndPeriod(
            @PathVariable Long accountId,
            @RequestParam LocalDate start,
            @RequestParam LocalDate end
    ) {
        List<BillResponseDTO> bills = billService.getAllByAccountIdAndPeriod(accountId, start, end);
        return ResponseEntity.ok(bills);
    }

    @GetMapping("/{id}")
    public ResponseEntity<BillResponseDTO> getById(@PathVariable Long id) {
        BillResponseDTO bill = billService.getById(id);
        return ResponseEntity.ok(bill);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        billService.delete(id);
        return ResponseEntity.noContent().build();
    }

}