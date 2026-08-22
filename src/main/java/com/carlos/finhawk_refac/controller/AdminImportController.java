package com.carlos.finhawk_refac.controller;

import com.carlos.finhawk_refac.dto.request.ResetAndReimportRequestDTO;
import com.carlos.finhawk_refac.dto.response.BillBackupDTO;
import com.carlos.finhawk_refac.dto.response.CategoryResponseDTO;
import com.carlos.finhawk_refac.dto.response.ResetAndReimportResultDTO;
import com.carlos.finhawk_refac.service.AdminImportService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// TEMPORARIO -- zerar e reimportar os lancamentos da conta 1 com dados
// corretos de finhawk_import_ago2026.json e ajustar o saldo pra bater com
// o extrato bancario real. Autenticacao normal (o service resolve o
// usuario via SecurityContextHolder e confirma dono da conta). Os dois GETs
// sao so leitura, seguros de chamar quantas vezes quiser -- so o POST muda
// dado, e faz isso numa unica transacao (tudo ou nada). Remover junto com
// AdminImportService depois que o import for confirmado.
@RestController
@RequestMapping("/admin/import")
public class AdminImportController {

    private final AdminImportService adminImportService;

    public AdminImportController(AdminImportService adminImportService) {
        this.adminImportService = adminImportService;
    }

    @GetMapping("/backup")
    public ResponseEntity<List<BillBackupDTO>> backup(@RequestParam Long accountId) {
        return ResponseEntity.ok(adminImportService.exportBackup(accountId));
    }

    @GetMapping("/categories")
    public ResponseEntity<List<CategoryResponseDTO>> categories(@RequestParam Long accountId) {
        return ResponseEntity.ok(adminImportService.listCategories(accountId));
    }

    @PostMapping("/reset-and-reimport")
    public ResponseEntity<ResetAndReimportResultDTO> resetAndReimport(@RequestBody ResetAndReimportRequestDTO request) {
        return ResponseEntity.ok(adminImportService.resetAndReimport(request));
    }
}
