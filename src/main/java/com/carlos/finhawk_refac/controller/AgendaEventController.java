package com.carlos.finhawk_refac.controller;

import com.carlos.finhawk_refac.dto.request.AgendaEventCompletionRequestDTO;
import com.carlos.finhawk_refac.dto.request.AgendaEventRequestDTO;
import com.carlos.finhawk_refac.dto.response.AgendaEventCompletionResponseDTO;
import com.carlos.finhawk_refac.dto.response.AgendaEventResponseDTO;
import com.carlos.finhawk_refac.enums.AgendaEventType;
import com.carlos.finhawk_refac.service.AgendaEventService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/agenda")
public class AgendaEventController {

    private final AgendaEventService agendaEventService;

    public AgendaEventController(AgendaEventService agendaEventService) {
        this.agendaEventService = agendaEventService;
    }

    @PostMapping
    public ResponseEntity<AgendaEventResponseDTO> create(@Valid @RequestBody AgendaEventRequestDTO dto) {
        return ResponseEntity.ok(agendaEventService.create(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AgendaEventResponseDTO> update(@PathVariable Long id,
                                                          @Valid @RequestBody AgendaEventRequestDTO dto) {
        return ResponseEntity.ok(agendaEventService.update(id, dto));
    }

    @GetMapping("/account/{accountId}")
    public ResponseEntity<List<AgendaEventResponseDTO>> getAllByAccount(
            @PathVariable Long accountId,
            @RequestParam(required = false) AgendaEventType type) {
        return ResponseEntity.ok(agendaEventService.getAllByAccountId(accountId, type));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        agendaEventService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // ===== Conclusão diária (hábitos) =====

    @GetMapping("/account/{accountId}/completions")
    public ResponseEntity<List<AgendaEventCompletionResponseDTO>> getCompletions(
            @PathVariable Long accountId,
            @RequestParam LocalDate date) {
        return ResponseEntity.ok(agendaEventService.getCompletionsByDate(accountId, date));
    }

    @PostMapping("/{id}/completion")
    public ResponseEntity<AgendaEventCompletionResponseDTO> markCompletion(
            @PathVariable Long id,
            @RequestBody AgendaEventCompletionRequestDTO dto) {
        var result = agendaEventService.markCompletion(id, dto);
        return result.created()
                ? ResponseEntity.status(201).body(result.dto())
                : ResponseEntity.ok(result.dto());
    }

    @DeleteMapping("/{id}/completion/{date}")
    public ResponseEntity<Void> unmarkCompletion(
            @PathVariable Long id,
            @PathVariable LocalDate date) {
        agendaEventService.unmarkCompletion(id, date);
        return ResponseEntity.noContent().build();
    }
}
