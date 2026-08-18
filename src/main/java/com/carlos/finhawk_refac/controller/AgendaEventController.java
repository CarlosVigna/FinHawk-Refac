package com.carlos.finhawk_refac.controller;

import com.carlos.finhawk_refac.dto.request.AgendaEventCompletionRequestDTO;
import com.carlos.finhawk_refac.dto.request.AgendaEventRequestDTO;
import com.carlos.finhawk_refac.dto.request.DayTypeOverrideRequestDTO;
import com.carlos.finhawk_refac.dto.response.AgendaEventCompletionResponseDTO;
import com.carlos.finhawk_refac.dto.response.AgendaEventResponseDTO;
import com.carlos.finhawk_refac.dto.response.DayTypeResponseDTO;
import com.carlos.finhawk_refac.enums.AgendaEventType;
import com.carlos.finhawk_refac.service.AgendaEventService;
import com.carlos.finhawk_refac.service.DayTypeService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/agenda")
public class AgendaEventController {

    private final AgendaEventService agendaEventService;
    private final DayTypeService dayTypeService;

    public AgendaEventController(AgendaEventService agendaEventService, DayTypeService dayTypeService) {
        this.agendaEventService = agendaEventService;
        this.dayTypeService = dayTypeService;
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

    // ===== Rollover de evento nao concluido (check-in dentro do app) =====

    @GetMapping("/account/{accountId}/rollover/pending")
    public ResponseEntity<List<AgendaEventResponseDTO>> getPendingRollovers(@PathVariable Long accountId) {
        return ResponseEntity.ok(agendaEventService.getPendingRollovers(accountId));
    }

    public record RolloverConfirmRequest(boolean done) {}

    @PostMapping("/{id}/rollover/confirm")
    public ResponseEntity<AgendaEventResponseDTO> confirmRollover(
            @PathVariable Long id,
            @RequestBody RolloverConfirmRequest dto) {
        return ResponseEntity.ok(agendaEventService.confirmRollover(id, dto.done()));
    }

    // ===== Tipo de dia manual (plantao/folga) =====

    @GetMapping("/account/{accountId}/day-type")
    public ResponseEntity<DayTypeResponseDTO> getDayType(
            @PathVariable Long accountId,
            @RequestParam LocalDate date) {
        return ResponseEntity.ok(dayTypeService.getEffectiveDayType(accountId, date));
    }

    @PutMapping("/account/{accountId}/day-type-override")
    public ResponseEntity<DayTypeResponseDTO> setDayTypeOverride(
            @PathVariable Long accountId,
            @RequestBody DayTypeOverrideRequestDTO dto) {
        return ResponseEntity.ok(dayTypeService.setOverride(accountId, dto));
    }
}
