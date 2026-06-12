package com.carlos.finhawk_refac.controller;

import com.carlos.finhawk_refac.dto.request.ChecklistCompletionRequestDTO;
import com.carlos.finhawk_refac.dto.request.ChecklistItemRequestDTO;
import com.carlos.finhawk_refac.dto.response.ChecklistCompletionResponseDTO;
import com.carlos.finhawk_refac.dto.response.ChecklistItemResponseDTO;
import com.carlos.finhawk_refac.dto.response.ChecklistSuggestionDTO;
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

    @GetMapping("/{id}/suggestion")
    public ResponseEntity<ChecklistSuggestionDTO> getSuggestion(@PathVariable Long id) {
        return ResponseEntity.ok(checklistItemService.getSuggestion(id));
    }

    // ===== Conclusão mensal =====

    @GetMapping("/account/{accountId}/completions")
    public ResponseEntity<List<Long>> getCompletions(
            @PathVariable Long accountId,
            @RequestParam String month) {
        return ResponseEntity.ok(checklistItemService.getCompletedItemIds(accountId, month));
    }

    @PostMapping("/{id}/completion")
    public ResponseEntity<ChecklistCompletionResponseDTO> markComplete(
            @PathVariable Long id,
            @RequestBody ChecklistCompletionRequestDTO dto) {
        var result = checklistItemService.markComplete(id, dto.month());
        return result.created()
                ? ResponseEntity.status(201).body(result.dto())
                : ResponseEntity.ok(result.dto());
    }

    @DeleteMapping("/{id}/completion/{month}")
    public ResponseEntity<Void> unmarkComplete(
            @PathVariable Long id,
            @PathVariable String month) {
        checklistItemService.unmarkComplete(id, month);
        return ResponseEntity.noContent().build();
    }
}
