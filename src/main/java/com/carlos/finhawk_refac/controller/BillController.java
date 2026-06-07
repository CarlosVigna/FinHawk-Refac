package com.carlos.finhawk_refac.controller;

import com.carlos.finhawk_refac.dto.response.BillResponseDTO;
import com.carlos.finhawk_refac.dto.request.BillRequestDTO;
import com.carlos.finhawk_refac.enums.StatusBill;
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

    @GetMapping("/all")
    public ResponseEntity<List<BillResponseDTO>> getAll() {
        List<BillResponseDTO> bills = billService.getAll();
        return ResponseEntity.ok(bills);
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

    @GetMapping("/status/{status}")
    public ResponseEntity<List<BillResponseDTO>> getAllByStatus(@PathVariable StatusBill status) {
        List<BillResponseDTO> bills = billService.getAllByStatus(status);
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

    @GetMapping("/export/account/{accountId}")
    public ResponseEntity<byte[]> exportAccountCsv(@PathVariable Long accountId) {
        byte[] csv = billService.exportAccountCsv(accountId);
        String filename = String.format("finhawk_account_%d.csv", accountId);
        return ResponseEntity.ok()
                .header("Content-Type", "text/csv; charset=UTF-8")
                .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                .body(csv);
    }
}