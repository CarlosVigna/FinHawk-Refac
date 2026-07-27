package com.carlos.finhawk_refac.controller;

import com.carlos.finhawk_refac.dto.request.BulkImportRequestDTO;
import com.carlos.finhawk_refac.dto.response.BulkImportResultDTO;
import com.carlos.finhawk_refac.service.AdminImportService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// TEMPORARIO -- usado uma unica vez pra carregar o historico categorizado
// manualmente de extratos bancarios (ver FINHAWK_IMPORT_EXTRATOS.md).
// Autenticacao normal (mesmo padrao dos outros controllers: o service
// resolve o usuario autenticado via SecurityContextHolder e confirma que a
// conta pertence a ele). Remover depois que o import for confirmado.
@RestController
@RequestMapping("/admin/import")
public class AdminImportController {

    private final AdminImportService adminImportService;

    public AdminImportController(AdminImportService adminImportService) {
        this.adminImportService = adminImportService;
    }

    @PostMapping("/bulk")
    public ResponseEntity<BulkImportResultDTO> bulkImport(@RequestBody BulkImportRequestDTO request) {
        BulkImportResultDTO result = adminImportService.bulkImport(request);
        return ResponseEntity.ok(result);
    }
}
