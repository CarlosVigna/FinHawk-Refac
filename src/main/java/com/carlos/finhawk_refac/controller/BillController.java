package com.carlos.finhawk_refac.controller;

import com.carlos.finhawk_refac.dto.request.BillRequestDTO;
import com.carlos.finhawk_refac.dto.response.BillResponseDTO;
import com.carlos.finhawk_refac.service.BillService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/bill")
public class BillController {

    public final BillService billService;

    public BillController(BillService billService) {
        this.billService = billService;
    }

    @PostMapping
    public ResponseEntity<BillResponseDTO> create (@RequestBody BillRequestDTO dto) {
        BillResponseDTO newBill = billService.create(dto);
        return ResponseEntity.ok(newBill);

    }

    @PutMapping("/{id}")
    public ResponseEntity<BillResponseDTO> update (@PathVariable Long id, @RequestBody BillRequestDTO dto){
        BillResponseDTO updated = billService.update(id, dto);
        return ResponseEntity.ok(updated);
    }

    @GetMapping("{id}")
    public ResponseEntity<BillResponseDTO> getById (@PathVariable Long id){
        BillResponseDTO bill = billService.getById(id);
        return ResponseEntity.ok(bill);
    }

    @GetMapping
    public ResponseEntity<List<BillResponseDTO>> getAll (){
        List<BillResponseDTO> bills = billService.getAll();
        return ResponseEntity.ok(bills);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id){
        billService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

