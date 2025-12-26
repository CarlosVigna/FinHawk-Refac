package com.carlos.finhawk_refac.controller;

import com.carlos.finhawk_refac.dto.request.BillRequestDTO;
import com.carlos.finhawk_refac.dto.response.BillResponseDTO;
import com.carlos.finhawk_refac.enums.StatusBill;
import com.carlos.finhawk_refac.service.BillService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/bill")
public class BillController {

    private final BillService billService;

    public BillController(BillService billService) {
        this.billService = billService;
    }

    @PostMapping
    public ResponseEntity<BillResponseDTO> create(@RequestBody BillRequestDTO dto) {
        System.out.println("🌐 Controller: CREATE chamado");
        BillResponseDTO newBill = billService.create(dto);
        return ResponseEntity.ok(newBill);
    }

    @PutMapping("/{id}")
    public ResponseEntity<BillResponseDTO> update(@PathVariable Long id, @RequestBody BillRequestDTO dto) {
        System.out.println("🌐 Controller: UPDATE chamado - ID: " + id);
        BillResponseDTO updated = billService.update(id, dto);
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/all")
    public ResponseEntity<List<BillResponseDTO>> getAll() {
        System.out.println("🌐 Controller: GET ALL chamado");
        List<BillResponseDTO> bills = billService.getAll();
        System.out.println("🌐 Controller: Retornando " + bills.size() + " bills do getAll()");
        return ResponseEntity.ok(bills);
    }

    @GetMapping("/account/{accountId}")
    public ResponseEntity<List<BillResponseDTO>> getAllByAccount(@PathVariable Long accountId) {

        List<BillResponseDTO> bills = billService.getAllByAccountId(accountId);


        return ResponseEntity.ok(bills);
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<BillResponseDTO>> getAllByStatus(@PathVariable StatusBill status) {
        System.out.println("🌐 Controller: GET BY STATUS chamado - status: " + status);
        List<BillResponseDTO> bills = billService.getAllByStatus(status);
        return ResponseEntity.ok(bills);
    }

    @GetMapping("/{id}")
    public ResponseEntity<BillResponseDTO> getById(@PathVariable Long id) {
        System.out.println("🌐 Controller: GET BY ID chamado - ID: " + id);
        BillResponseDTO bill = billService.getById(id);
        return ResponseEntity.ok(bill);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        System.out.println("🌐 Controller: DELETE chamado - ID: " + id);
        billService.delete(id);
        return ResponseEntity.noContent().build();
    }
}