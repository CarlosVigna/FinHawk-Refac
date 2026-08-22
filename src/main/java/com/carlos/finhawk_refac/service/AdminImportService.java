package com.carlos.finhawk_refac.service;

import com.carlos.finhawk_refac.dto.request.BulkImportRequestDTO;
import com.carlos.finhawk_refac.dto.request.ResetAndReimportRequestDTO;
import com.carlos.finhawk_refac.dto.response.BillBackupDTO;
import com.carlos.finhawk_refac.dto.response.CategoryResponseDTO;
import com.carlos.finhawk_refac.dto.response.ResetAndReimportResultDTO;
import com.carlos.finhawk_refac.entity.Account;
import com.carlos.finhawk_refac.entity.Bill;
import com.carlos.finhawk_refac.entity.Category;
import com.carlos.finhawk_refac.entity.UserAccount;
import com.carlos.finhawk_refac.enums.CategoryType;
import com.carlos.finhawk_refac.enums.Periodicity;
import com.carlos.finhawk_refac.enums.StatusBill;
import com.carlos.finhawk_refac.repository.AccountRepository;
import com.carlos.finhawk_refac.repository.BillRepository;
import com.carlos.finhawk_refac.repository.CategoryRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

// Endpoint/service TEMPORARIO -- usado uma unica vez pra zerar e
// reimportar os lancamentos da conta 1 com dados corretos (ver
// FINHAWK_import_ago2026 -- zerar/reimportar/ajustar saldo). Remover
// junto com AdminImportController depois que o import for confirmado.
//
// O JSON de import (finhawk_import_ago2026.json) vem embutido no
// classpath (import-ago2026.json) em vez de vir no corpo do request --
// evita o usuario precisar colar 78 lancamentos manualmente numa chamada
// autenticada pelo console do navegador.
@Service
@Transactional(readOnly = true)
public class AdminImportService {

    private static final String IMPORT_RESOURCE = "import-ago2026.json";

    private final AccountRepository accountRepository;
    private final CategoryRepository categoryRepository;
    private final BillRepository billRepository;
    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;

    public AdminImportService(AccountRepository accountRepository,
                               CategoryRepository categoryRepository,
                               BillRepository billRepository,
                               AuditLogService auditLogService) {
        this.accountRepository = accountRepository;
        this.categoryRepository = categoryRepository;
        this.billRepository = billRepository;
        this.auditLogService = auditLogService;
        this.objectMapper = new ObjectMapper();
    }

    private UserAccount getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !(authentication.getPrincipal() instanceof UserAccount)) {
            throw new RuntimeException("Unauthenticated user");
        }

        return (UserAccount) authentication.getPrincipal();
    }

    private Account requireOwnedAccount(Long accountId, UserAccount currentUser) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Account not found"));

        if (!account.getUserAccount().getId().equals(currentUser.getId())) {
            throw new RuntimeException("You are not allowed to use this account");
        }

        return account;
    }

    // ===== Parte 1: backup de seguranca (so leitura, seguro chamar quantas vezes quiser) =====

    public List<BillBackupDTO> exportBackup(Long accountId) {
        UserAccount currentUser = getAuthenticatedUser();
        requireOwnedAccount(accountId, currentUser);

        return billRepository.findAllByAccount_Id(accountId).stream()
                .map(b -> new BillBackupDTO(
                        b.getId(),
                        b.getDescription(),
                        b.getEmission(),
                        b.getMaturity(),
                        b.getInstallmentAmount(),
                        b.getInstallmentCount(),
                        b.getCurrentInstallment(),
                        b.getPeriodicity() != null ? b.getPeriodicity().name() : null,
                        b.getStatus() != null ? b.getStatus().name() : null,
                        b.getCategory() != null ? b.getCategory().getName() : null,
                        b.getCategory() != null && b.getCategory().getType() != null ? b.getCategory().getType().name() : null,
                        b.getAccount().getId(),
                        b.getCreatedAt(),
                        b.getUpdatedAt(),
                        b.getPaidAt(),
                        b.getReceivedAt()
                ))
                .toList();
    }

    // ===== Parte 2: categorias existentes (so leitura) =====

    public List<CategoryResponseDTO> listCategories(Long accountId) {
        UserAccount currentUser = getAuthenticatedUser();
        Account account = requireOwnedAccount(accountId, currentUser);

        return categoryRepository.findAllByAccountAndDeletedAtIsNull(account).stream()
                .map(c -> new CategoryResponseDTO(c.getId(), c.getName(), c.getType() != null ? c.getType().name() : null))
                .toList();
    }

    // ===== Partes 3+4+6: apaga tudo, reimporta, ajusta saldo -- tudo numa =====
    // ===== unica transacao: se qualquer etapa falhar (categoria invalida, =====
    // ===== verificacao final do saldo nao bater), a exclusao E o reimport =====
    // ===== inteiros sao revertidos, os lancamentos originais nunca ficam =====
    // ===== deletados sem o reimport ter dado certo. =====

    @Transactional
    public ResetAndReimportResultDTO resetAndReimport(ResetAndReimportRequestDTO request) {
        UserAccount currentUser = getAuthenticatedUser();
        Account account = requireOwnedAccount(request.accountId(), currentUser);

        if (request.targetBalance() == null) {
            throw new RuntimeException("O saldo alvo (targetBalance) é obrigatório.");
        }

        BulkImportRequestDTO payload = loadBundledImportPayload();

        // ----- Parte 3: apaga TODOS os bill da conta (e so isso -- nao toca em category/agenda/habito/meta) -----
        List<Bill> existing = billRepository.findAllByAccount_Id(account.getId());
        int billsDeleted = existing.size();
        billRepository.deleteAll(existing);
        auditLogService.record(currentUser, AuditLogService.DELETE, "Bill",
                account.getId(), "Limpeza em massa antes de reimportar (" + billsDeleted + " lancamentos apagados)");

        // ----- Parte 2: mapa de categorias case-insensitive, pra reaproveitar em vez de duplicar -----
        Map<String, Category> categoriasPorNomeLower = new HashMap<>();
        categoryRepository.findAllByAccountAndDeletedAtIsNull(account)
                .forEach(c -> categoriasPorNomeLower.put(c.getName().toLowerCase(Locale.ROOT), c));

        int categoriasReaproveitadas = 0;
        int categoriasCriadas = 0;
        List<String> categoriasComFalha = new ArrayList<>();

        for (BulkImportRequestDTO.CategoryImportItem item : payload.categories()) {
            String key = item.name().toLowerCase(Locale.ROOT);
            if (categoriasPorNomeLower.containsKey(key)) {
                categoriasReaproveitadas++;
                continue;
            }
            try {
                Category category = new Category();
                category.setName(item.name());
                category.setType(CategoryType.valueOf(item.type().toUpperCase(Locale.ROOT)));
                category.setAccount(account);

                Category saved = categoryRepository.save(category);
                categoriasPorNomeLower.put(key, saved);
                categoriasCriadas++;

                auditLogService.record(currentUser, AuditLogService.CREATE, "Category", saved.getId(),
                        saved.getName() + " (reimport ago/2026)");
            } catch (Exception e) {
                categoriasComFalha.add(item.name() + ": " + e.getMessage());
            }
        }

        // ----- Parte 4: reimporta os lancamentos, acumulando receitas/despesas -----
        // pra calcular o saldo do mesmo jeito que BillService.getConsolidatedSummary()
        // (RECEIVED+RECEIPT menos PAID+PAYMENT) sem precisar reconsultar o banco.
        int billsImported = 0;
        List<String> billsComFalha = new ArrayList<>();
        BigDecimal receitas = BigDecimal.ZERO;
        BigDecimal despesas = BigDecimal.ZERO;

        for (BulkImportRequestDTO.BillImportItem item : payload.bills()) {
            String rotulo = item.description() + " (" + item.date() + ")";
            try {
                Category category = categoriasPorNomeLower.get(item.categoryName().toLowerCase(Locale.ROOT));
                if (category == null) {
                    billsComFalha.add(rotulo + ": categoria \"" + item.categoryName() + "\" não encontrada");
                    continue;
                }

                StatusBill status = StatusBill.valueOf(item.status().toUpperCase(Locale.ROOT));
                LocalDate date = LocalDate.parse(item.date());

                Bill bill = new Bill();
                bill.setDescription(item.description());
                bill.setEmission(date);
                bill.setMaturity(date);
                bill.setInstallmentAmount(item.amount());
                bill.setInstallmentCount(1);
                bill.setCurrentInstallment(1);
                bill.setPeriodicity(Periodicity.MONTHLY);
                bill.setStatus(status);
                bill.setCategory(category);
                bill.setAccount(account);

                // Lancamento historico ja liquidado -- paidAt/receivedAt usa a
                // data real da transacao, nao "agora".
                LocalDateTime dataHistorica = date.atStartOfDay();
                if (status == StatusBill.PAID) {
                    bill.setPaidAt(dataHistorica);
                } else if (status == StatusBill.RECEIVED) {
                    bill.setReceivedAt(dataHistorica);
                }

                Bill saved = billRepository.save(bill);
                billsImported++;

                if (status == StatusBill.RECEIVED && category.getType() == CategoryType.RECEIPT) {
                    receitas = receitas.add(item.amount());
                } else if (status == StatusBill.PAID && category.getType() == CategoryType.PAYMENT) {
                    despesas = despesas.add(item.amount());
                }

                auditLogService.record(currentUser, AuditLogService.CREATE, "Bill", saved.getId(),
                        saved.getDescription() + " (reimport ago/2026)");
            } catch (Exception e) {
                billsComFalha.add(rotulo + ": " + e.getMessage());
            }
        }

        BigDecimal balanceBeforeAdjustment = receitas.subtract(despesas);

        // ----- Parte 6: ajuste de saldo pra bater com o extrato bancario real -----
        BigDecimal diff = request.targetBalance().subtract(balanceBeforeAdjustment);
        BigDecimal adjustmentAmount = BigDecimal.ZERO;
        String adjustmentDirection = "nenhum";

        if (diff.compareTo(BigDecimal.ZERO) != 0) {
            boolean isReceipt = diff.signum() > 0;
            adjustmentAmount = diff.abs();
            adjustmentDirection = isReceipt ? "RECEIPT" : "PAYMENT";

            String adjustmentCategoryName = "Ajuste de Saldo";
            CategoryType adjustmentType = isReceipt ? CategoryType.RECEIPT : CategoryType.PAYMENT;

            Category adjustmentCategory = categoriasPorNomeLower.get(adjustmentCategoryName.toLowerCase(Locale.ROOT));
            if (adjustmentCategory == null || adjustmentCategory.getType() != adjustmentType) {
                Category novaCategoria = new Category();
                novaCategoria.setName(adjustmentCategoryName);
                novaCategoria.setType(adjustmentType);
                novaCategoria.setAccount(account);
                adjustmentCategory = categoryRepository.save(novaCategoria);
                categoriasPorNomeLower.put(adjustmentCategoryName.toLowerCase(Locale.ROOT), adjustmentCategory);
                categoriasCriadas++;
                auditLogService.record(currentUser, AuditLogService.CREATE, "Category", adjustmentCategory.getId(),
                        adjustmentCategoryName + " (reconciliação de saldo)");
            }

            LocalDate today = LocalDate.now();

            Bill adjustmentBill = new Bill();
            adjustmentBill.setDescription("Ajuste de saldo - reconciliação com extrato bancário");
            adjustmentBill.setEmission(today);
            adjustmentBill.setMaturity(today);
            adjustmentBill.setInstallmentAmount(adjustmentAmount);
            adjustmentBill.setInstallmentCount(1);
            adjustmentBill.setCurrentInstallment(1);
            adjustmentBill.setPeriodicity(Periodicity.MONTHLY);
            adjustmentBill.setCategory(adjustmentCategory);
            adjustmentBill.setAccount(account);

            LocalDateTime now = LocalDateTime.now();
            if (isReceipt) {
                adjustmentBill.setStatus(StatusBill.RECEIVED);
                adjustmentBill.setReceivedAt(now);
                receitas = receitas.add(adjustmentAmount);
            } else {
                adjustmentBill.setStatus(StatusBill.PAID);
                adjustmentBill.setPaidAt(now);
                despesas = despesas.add(adjustmentAmount);
            }

            Bill savedAdjustment = billRepository.save(adjustmentBill);
            billsImported++;

            auditLogService.record(currentUser, AuditLogService.CREATE, "Bill", savedAdjustment.getId(),
                    savedAdjustment.getDescription());
        }

        BigDecimal finalBalance = receitas.subtract(despesas);

        // Rede de seguranca: se por algum motivo a conta nao bateu exatamente
        // (nao deveria acontecer -- e so subtracao/soma do mesmo BigDecimal),
        // aborta a transacao inteira em vez de deixar o saldo errado.
        if (finalBalance.compareTo(request.targetBalance()) != 0) {
            throw new RuntimeException("Saldo final (" + finalBalance + ") não bateu com o alvo ("
                    + request.targetBalance() + ") -- abortando, nada foi salvo.");
        }

        return new ResetAndReimportResultDTO(
                billsDeleted,
                categoriasReaproveitadas,
                categoriasCriadas,
                categoriasComFalha,
                billsImported,
                billsComFalha,
                balanceBeforeAdjustment,
                request.targetBalance(),
                adjustmentAmount,
                adjustmentDirection,
                finalBalance
        );
    }

    private BulkImportRequestDTO loadBundledImportPayload() {
        try (InputStream in = new ClassPathResource(IMPORT_RESOURCE).getInputStream()) {
            return objectMapper.readValue(in, BulkImportRequestDTO.class);
        } catch (Exception e) {
            throw new RuntimeException("Falha ao ler o arquivo de import embutido (" + IMPORT_RESOURCE + "): " + e.getMessage(), e);
        }
    }
}
