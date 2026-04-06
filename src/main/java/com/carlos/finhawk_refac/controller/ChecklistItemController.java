package com.carlos.finhawk_refac.controller;

import com.carlos.finhawk_refac.dto.request.ChecklistItemRequestDTO;
import com.carlos.finhawk_refac.dto.response.ChecklistItemResponseDTO;
import com.carlos.finhawk_refac.service.ChecklistItemService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/checklist")
public class ChecklistItemController {

    private final ChecklistItemService checklistItemService;

    public ChecklistItemController(ChecklistItemService checklistItemService) {
        this.checklistItemService = checklistItemService;
    }

    @PostMapping
    public ResponseEntity<ChecklistItemResponseDTO> create(@RequestBody ChecklistItemRequestDTO dto) {
        return ResponseEntity.ok(checklistItemService.create(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ChecklistItemResponseDTO> update(@PathVariable Long id,
                                                           @RequestBody ChecklistItemRequestDTO dto) {
        return ResponseEntity.ok(checklistItemService.update(id, dto));
    }

    @GetMapping("/account/{accountId}")
    public ResponseEntity<List<ChecklistItemResponseDTO>> getAllByAccount(@PathVariable Long accountId) {
        return ResponseEntity.ok(checklistItemService.getAllByAccountId(accountId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        checklistItemService.delete(id);
        return ResponseEntity.noContent().build();
    }
}