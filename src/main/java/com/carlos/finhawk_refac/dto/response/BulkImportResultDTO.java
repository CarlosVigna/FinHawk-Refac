package com.carlos.finhawk_refac.dto.response;

import java.util.List;

public record BulkImportResultDTO(
        int categoriasCriadas,
        List<String> categoriasComFalha,
        int lancamentosCriados,
        int lancamentosIgnoradosPorJaExistir,
        List<String> lancamentosComFalha
) {}
