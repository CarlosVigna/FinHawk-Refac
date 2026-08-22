package com.carlos.finhawk_refac.controller;

import com.carlos.finhawk_refac.dto.request.WeeklyGoalRequestDTO;
import com.carlos.finhawk_refac.dto.response.WeeklyGoalResponseDTO;
import com.carlos.finhawk_refac.service.WeeklyGoalService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/weekly-goal")
public class WeeklyGoalController {

    private final WeeklyGoalService weeklyGoalService;

    public WeeklyGoalController(WeeklyGoalService weeklyGoalService) {
        this.weeklyGoalService = weeklyGoalService;
    }

    @PostMapping
    public ResponseEntity<WeeklyGoalResponseDTO> create(@Valid @RequestBody WeeklyGoalRequestDTO dto) {
        return ResponseEntity.ok(weeklyGoalService.create(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<WeeklyGoalResponseDTO> update(@PathVariable Long id, @Valid @RequestBody WeeklyGoalRequestDTO dto) {
        return ResponseEntity.ok(weeklyGoalService.update(id, dto));
    }

    @GetMapping("/account/{accountId}/current")
    public ResponseEntity<List<WeeklyGoalResponseDTO>> getCurrentWeek(@PathVariable Long accountId) {
        return ResponseEntity.ok(weeklyGoalService.getCurrentWeek(accountId));
    }

    public record CompletedRequest(boolean completed) {}

    @PatchMapping("/{id}/completed")
    public ResponseEntity<WeeklyGoalResponseDTO> setCompleted(@PathVariable Long id, @RequestBody CompletedRequest dto) {
        return ResponseEntity.ok(weeklyGoalService.setCompleted(id, dto.completed()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        weeklyGoalService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
