package com.carlos.finhawk_refac.service;

import com.carlos.finhawk_refac.dto.request.BulkImportRequestDTO;
import com.carlos.finhawk_refac.dto.response.BulkImportResultDTO;
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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Endpoint/service TEMPORARIO -- usado uma unica vez pra carregar o
// historico categorizado manualmente de extratos bancarios (ver
// FINHAWK_IMPORT_EXTRATOS.md). Remover junto com AdminImportController
// depois que o import for confirmado.
@Service
@Transactional(readOnly = true)
public class AdminImportService {

    private final AccountRepository accountRepository;
    private final CategoryRepository categoryRepository;
    private final BillRepository billRepository;
    private final AuditLogService auditLogService;

    public AdminImportService(AccountRepository accountRepository,
                               CategoryRepository categoryRepository,
                               BillRepository billRepository,
                               AuditLogService auditLogService) {
        this.accountRepository = accountRepository;
        this.categoryRepository = categoryRepository;
        this.billRepository = billRepository;
        this.auditLogService = auditLogService;
    }

    private UserAccount getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !(authentication.getPrincipal() instanceof UserAccount)) {
            throw new RuntimeException("Unauthenticated user");
        }

        return (UserAccount) authentication.getPrincipal();
    }

    @Transactional
    public BulkImportResultDTO bulkImport(BulkImportRequestDTO request) {
        UserAccount currentUser = getAuthenticatedUser();

        Account account = accountRepository.findById(request.accountId())
                .orElseThrow(() -> new RuntimeException("Account not found"));

        if (!account.getUserAccount().getId().equals(currentUser.getId())) {
            throw new RuntimeException("You are not allowed to import into this account");
        }

        // Pre-carrega categorias ja existentes na conta -- torna o import
        // idempotente (rodar de novo por engano nao duplica categoria).
        Map<String, Category> categoriasPorNome = new HashMap<>();
        categoryRepository.findAllByAccountAndDeletedAtIsNull(account)
                .forEach(c -> categoriasPorNome.put(c.getName(), c));

        int categoriasCriadas = 0;
        List<String> categoriasComFalha = new ArrayList<>();

        for (BulkImportRequestDTO.CategoryImportItem item : request.categories()) {
            if (categoriasPorNome.containsKey(item.name())) {
                continue;
            }
            try {
                Category category = new Category();
                category.setName(item.name());
                category.setType(CategoryType.valueOf(item.type().toUpperCase()));
                category.setAccount(account);

                Category saved = categoryRepository.save(category);
                categoriasPorNome.put(saved.getName(), saved);
                categoriasCriadas++;

                auditLogService.record(currentUser, AuditLogService.CREATE, "Category", saved.getId(),
                        saved.getName() + " (import em massa)");
            } catch (Exception e) {
                categoriasComFalha.add(item.name() + ": " + e.getMessage());
            }
        }

        int lancamentosCriados = 0;
        int lancamentosIgnorados = 0;
        List<String> lancamentosComFalha = new ArrayList<>();

        for (BulkImportRequestDTO.BillImportItem item : request.bills()) {
            String rotulo = item.description() + " (" + item.date() + ")";
            try {
                Category category = categoriasPorNome.get(item.categoryName());
                if (category == null) {
                    lancamentosComFalha.add(rotulo + ": categoria \"" + item.categoryName() + "\" nao encontrada");
                    continue;
                }

                boolean jaExiste = billRepository.existsByAccount_IdAndDescriptionAndMaturityAndInstallmentAmount(
                        account.getId(), item.description(), item.date(), item.amount());
                if (jaExiste) {
                    lancamentosIgnorados++;
                    continue;
                }

                StatusBill status = StatusBill.valueOf(item.status().toUpperCase());

                Bill bill = new Bill();
                bill.setDescription(item.description());
                bill.setEmission(item.date());
                bill.setMaturity(item.date());
                bill.setInstallmentAmount(item.amount());
                bill.setInstallmentCount(1);
                bill.setCurrentInstallment(1);
                bill.setPeriodicity(Periodicity.MONTHLY);
                bill.setStatus(status);
                bill.setCategory(category);
                bill.setAccount(account);

                // Lancamento historico ja liquidado -- paidAt/receivedAt
                // usa a data real da transacao, nao "agora" (diferente do
                // fluxo normal de BillService.create()/updateStatus(),
                // que carimba o momento da acao do usuario).
                LocalDateTime dataHistorica = item.date().atStartOfDay();
                if (status == StatusBill.PAID) {
                    bill.setPaidAt(dataHistorica);
                } else if (status == StatusBill.RECEIVED) {
                    bill.setReceivedAt(dataHistorica);
                }

                Bill saved = billRepository.save(bill);
                lancamentosCriados++;

                auditLogService.record(currentUser, AuditLogService.CREATE, "Bill", saved.getId(),
                        saved.getDescription() + " (import em massa)");
            } catch (Exception e) {
                lancamentosComFalha.add(rotulo + ": " + e.getMessage());
            }
        }

        return new BulkImportResultDTO(
                categoriasCriadas,
                categoriasComFalha,
                lancamentosCriados,
                lancamentosIgnorados,
                lancamentosComFalha
        );
    }
}
