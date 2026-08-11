package com.carlos.finhawk_refac.service;

import com.carlos.finhawk_refac.dto.request.BillRequestDTO;
import com.carlos.finhawk_refac.dto.response.BillResponseDTO;
import com.carlos.finhawk_refac.dto.response.CategoryResponseDTO;
import com.carlos.finhawk_refac.entity.Account;
import com.carlos.finhawk_refac.entity.Bill;
import com.carlos.finhawk_refac.entity.Category;
import com.carlos.finhawk_refac.entity.UserAccount;
import com.carlos.finhawk_refac.enums.StatusBill;
import com.carlos.finhawk_refac.repository.AccountRepository;
import com.carlos.finhawk_refac.repository.BillRepository;
import com.carlos.finhawk_refac.repository.CategoryRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import com.carlos.finhawk_refac.dto.response.DashboardSummaryDTO;
import com.carlos.finhawk_refac.dto.response.AccountSummaryDTO;

@Service
@Transactional(readOnly = true)
public class BillService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final BillRepository billRepository;
    private final CategoryRepository categoryRepository;
    private final AccountRepository accountRepository;
    private final AuditLogService auditLogService;
    private final WhatsAppNotificationService whatsAppNotificationService;

    @Value("${whatsapp.large-amount-threshold:1000}")
    private BigDecimal largeAmountThreshold;

    public BillService(
            BillRepository billRepository,
            CategoryRepository categoryRepository,
            AccountRepository accountRepository,
            AuditLogService auditLogService,
            WhatsAppNotificationService whatsAppNotificationService
    ) {
        this.billRepository = billRepository;
        this.categoryRepository = categoryRepository;
        this.accountRepository = accountRepository;
        this.auditLogService = auditLogService;
        this.whatsAppNotificationService = whatsAppNotificationService;
    }

    private static String formatCurrency(BigDecimal value) {
        NumberFormat fmt = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
        return fmt.format(value != null ? value : BigDecimal.ZERO);
    }

    // Notificacoes de ciclo de vida de Bill (criado/pago/editado/excluido) --
    // nunca disparam durante operacoes em lote (ver BulkOperationContext).

    private void notifyCreated(Bill bill) {
        if (BulkOperationContext.isActive()) {
            return;
        }
        whatsAppNotificationService.sendMessage(
                "🆕 Novo lançamento: " + bill.getDescription()
                        + " — " + formatCurrency(bill.getInstallmentAmount())
                        + " (vence " + bill.getMaturity().format(DATE_FMT) + ").");

        if (bill.getInstallmentAmount() != null
                && largeAmountThreshold != null
                && bill.getInstallmentAmount().compareTo(largeAmountThreshold) > 0) {
            whatsAppNotificationService.sendMessage(
                    "⚠️ Lançamento de valor alto: " + bill.getDescription()
                            + " — " + formatCurrency(bill.getInstallmentAmount()));
        }
    }

    private void notifyStatusChanged(Bill bill, StatusBill oldStatus) {
        if (BulkOperationContext.isActive()) {
            return;
        }
        if (bill.getStatus() == oldStatus) {
            return;
        }
        if (bill.getStatus() == StatusBill.PAID) {
            whatsAppNotificationService.sendMessage(
                    "✅ Pago: " + bill.getDescription() + " — " + formatCurrency(bill.getInstallmentAmount()));
        } else if (bill.getStatus() == StatusBill.RECEIVED) {
            whatsAppNotificationService.sendMessage(
                    "✅ Recebido: " + bill.getDescription() + " — " + formatCurrency(bill.getInstallmentAmount()));
        }
    }

    private void notifyEdited(Bill bill, BigDecimal oldAmount, LocalDate oldMaturity) {
        if (BulkOperationContext.isActive()) {
            return;
        }
        boolean amountChanged = !Objects.equals(oldAmount, bill.getInstallmentAmount())
                && (oldAmount == null || bill.getInstallmentAmount() == null
                        || oldAmount.compareTo(bill.getInstallmentAmount()) != 0);
        boolean maturityChanged = !Objects.equals(oldMaturity, bill.getMaturity());

        if (!amountChanged && !maturityChanged) {
            return;
        }

        StringBuilder sb = new StringBuilder("✏️ Editado: ").append(bill.getDescription());
        if (amountChanged) {
            sb.append(" — valor mudou de ").append(formatCurrency(oldAmount))
                    .append(" pra ").append(formatCurrency(bill.getInstallmentAmount()));
        }
        if (maturityChanged) {
            sb.append(amountChanged ? "; " : " — ")
                    .append("vencimento mudou de ").append(oldMaturity != null ? oldMaturity.format(DATE_FMT) : "-")
                    .append(" pra ").append(bill.getMaturity() != null ? bill.getMaturity().format(DATE_FMT) : "-");
        }
        whatsAppNotificationService.sendMessage(sb.toString());
    }

    private void notifyDeleted(Bill bill) {
        if (BulkOperationContext.isActive()) {
            return;
        }
        whatsAppNotificationService.sendMessage(
                "🗑️ Excluído: " + bill.getDescription() + " — " + formatCurrency(bill.getInstallmentAmount()));
    }

    private UserAccount getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !(authentication.getPrincipal() instanceof UserAccount)) {
            throw new RuntimeException("Unauthenticated user");
        }

        return (UserAccount) authentication.getPrincipal();
    }

    private BillResponseDTO toResponseDTO(Bill bill) {
        return new BillResponseDTO(
                bill.getId(),
                bill.getDescription(),
                bill.getEmission(),
                bill.getMaturity(),
                bill.getInstallmentAmount(),
                bill.getInstallmentCount(),
                bill.getCurrentInstallment(),
                bill.getStatus(),
                bill.getPeriodicity(),
                bill.getAccount().getId(),
                new CategoryResponseDTO(
                        bill.getCategory().getId(),
                        bill.getCategory().getName(),
                        bill.getCategory().getType().name()
                ),
                bill.getCreatedAt(),
                bill.getUpdatedAt(),
                bill.getPaidAt(),
                bill.getReceivedAt()
        );
    }

    @Transactional
    public BillResponseDTO create(BillRequestDTO dto) {
        UserAccount currentUser = getAuthenticatedUser();

        Account account = accountRepository.findById(dto.accountId())
                .orElseThrow(() -> new RuntimeException("Account not found"));

        if (!account.getUserAccount().getId().equals(currentUser.getId())) {
            throw new RuntimeException("You are not allowed to use this account");
        }

        Category category = categoryRepository.findById(dto.categoryId())
                .orElseThrow(() -> new RuntimeException("Category not found"));

        if (!category.getAccount().getId().equals(account.getId())) {
            throw new RuntimeException("Category must belong to the same account");
        }

        if (category.getDeletedAt() != null) {
            throw new RuntimeException("Category not found");
        }

        int totalInstallments = (dto.installmentCount() != null && dto.installmentCount() > 0)
                ? dto.installmentCount()
                : 1;

        List<Bill> billsToSave = new ArrayList<>();

        for (int i = 0; i < totalInstallments; i++) {
            Bill bill = new Bill();

            bill.setDescription(dto.description());
            bill.setEmission(dto.emission());
            bill.setInstallmentAmount(dto.installmentAmount() != null ? dto.installmentAmount() : BigDecimal.ZERO);
            bill.setInstallmentCount(totalInstallments);
            bill.setPeriodicity(dto.periodicity());
            bill.setCategory(category);
            bill.setAccount(account);
            bill.setCurrentInstallment(i + 1);

            LocalDate maturityDate = dto.maturity();

            if (i > 0) {
                switch (dto.periodicity()) {
                    case MONTHLY -> maturityDate = maturityDate.plusMonths(i);
                    case BIMONTHLY -> maturityDate = maturityDate.plusMonths(i * 2L);
                    case QUARTERLY -> maturityDate = maturityDate.plusMonths(i * 3L);
                    case SEMIANNUAL -> maturityDate = maturityDate.plusMonths(i * 6L);
                    case ANNUAL -> maturityDate = maturityDate.plusYears(i);
                    default -> maturityDate = maturityDate.plusMonths(i);
                }
            }

            bill.setMaturity(maturityDate);

            if (i == 0) {
                StatusBill initialStatus = dto.status() != null ? dto.status() : StatusBill.PENDING;
                bill.setStatus(initialStatus);
                if (initialStatus == StatusBill.PAID) {
                    bill.setPaidAt(LocalDateTime.now());
                } else if (initialStatus == StatusBill.RECEIVED) {
                    bill.setReceivedAt(LocalDateTime.now());
                }
            } else {
                bill.setStatus(StatusBill.PENDING);
            }

            billsToSave.add(bill);
        }

        List<Bill> savedBills = billRepository.saveAll(billsToSave);

        for (Bill saved : savedBills) {
            auditLogService.record(currentUser, AuditLogService.CREATE, "Bill", saved.getId(), saved.getDescription());
        }

        // Uma unica notificacao por chamada a create(), mesmo quando gera
        // varias parcelas de uma vez -- evita enxurrada de mensagens pra
        // compras parceladas sem deixar de avisar da criacao.
        notifyCreated(savedBills.get(0));

        return toResponseDTO(savedBills.get(0));
    }

    @Transactional
    public BillResponseDTO update(Long id, BillRequestDTO dto) {
        UserAccount currentUser = getAuthenticatedUser();

        Bill bill = billRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Bill not found"));

        if (!bill.getAccount().getUserAccount().getId().equals(currentUser.getId())) {
            throw new RuntimeException("You are not allowed to update this bill");
        }

        Category category = categoryRepository.findById(dto.categoryId())
                .orElseThrow(() -> new RuntimeException("Category not found"));

        if (!category.getAccount().getId().equals(bill.getAccount().getId())) {
            throw new RuntimeException("Category must belong to the same account");
        }

        if (category.getDeletedAt() != null) {
            throw new RuntimeException("Category not found");
        }

        StatusBill oldStatus = bill.getStatus();
        BigDecimal oldAmount = bill.getInstallmentAmount();
        LocalDate oldMaturity = bill.getMaturity();

        bill.setDescription(dto.description());
        bill.setEmission(dto.emission());
        bill.setMaturity(dto.maturity());
        bill.setPeriodicity(dto.periodicity());
        bill.setCategory(category);

        if (dto.status() != null) {
            StatusBill newStatus = dto.status();
            // if transitioning to PAID/RECEIVED set timestamps if not present
            if (newStatus == StatusBill.PAID && bill.getPaidAt() == null) {
                bill.setPaidAt(LocalDateTime.now());
            }
            if (newStatus == StatusBill.RECEIVED && bill.getReceivedAt() == null) {
                bill.setReceivedAt(LocalDateTime.now());
            }
            bill.setStatus(newStatus);
        }

        if (dto.installmentAmount() != null) {
            bill.setInstallmentAmount(dto.installmentAmount());
        }

        if (dto.installmentCount() != null) {
            bill.setInstallmentCount(dto.installmentCount());
        }

        Bill updated = billRepository.save(bill);

        auditLogService.record(currentUser, AuditLogService.UPDATE, "Bill", updated.getId(), updated.getDescription());

        notifyStatusChanged(updated, oldStatus);
        notifyEdited(updated, oldAmount, oldMaturity);

        return toResponseDTO(updated);
    }

    public DashboardSummaryDTO getConsolidatedSummary() {
        UserAccount currentUser = getAuthenticatedUser();
        List<Account> accounts = accountRepository.findAllByUserAccount(currentUser);

        BigDecimal patrimonio = BigDecimal.ZERO;
        List<AccountSummaryDTO> summaries = new ArrayList<>();

        for (Account account : accounts) {
            List<Bill> bills = billRepository.findAllByAccount_Id(account.getId());

            BigDecimal receitasRealizadas = bills.stream()
                    .filter(b -> b.getStatus() == StatusBill.RECEIVED)
                    .filter(b -> b.getCategory().getType().name().equals("RECEIPT"))
                    .map(b -> b.getInstallmentAmount() != null ? b.getInstallmentAmount() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal despesasRealizadas = bills.stream()
                    .filter(b -> b.getStatus() == StatusBill.PAID)
                    .filter(b -> b.getCategory().getType().name().equals("PAYMENT"))
                    .map(b -> b.getInstallmentAmount() != null ? b.getInstallmentAmount() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal saldoRealizado = receitasRealizadas.subtract(despesasRealizadas);
            patrimonio = patrimonio.add(saldoRealizado);

            summaries.add(new AccountSummaryDTO(
                    account.getId(),
                    account.getName(),
                    receitasRealizadas,
                    despesasRealizadas,
                    saldoRealizado
            ));
        }

        return new DashboardSummaryDTO(
                patrimonio,
                accounts.size(),
                summaries
        );
    }

    public BillResponseDTO getById(Long id) {
        UserAccount currentUser = getAuthenticatedUser();

        Bill bill = billRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Bill not found"));

        if (!bill.getAccount().getUserAccount().getId().equals(currentUser.getId())) {
            throw new RuntimeException("Access denied");
        }

        return toResponseDTO(bill);
    }

    public List<BillResponseDTO> getAllByAccountId(Long accountId) {
        UserAccount currentUser = getAuthenticatedUser();

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Account not found"));

        if (!account.getUserAccount().getId().equals(currentUser.getId())) {
            throw new RuntimeException("Access denied");
        }

        List<Bill> bills = billRepository.findAllByAccount_Id(accountId);

        return bills.stream()
                .map(this::toResponseDTO)
                .toList();
    }

    public List<BillResponseDTO> getAllByAccountIdAndPeriod(Long accountId, LocalDate start, LocalDate end) {
        UserAccount currentUser = getAuthenticatedUser();

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Account not found"));

        if (!account.getUserAccount().getId().equals(currentUser.getId())) {
            throw new RuntimeException("Access denied");
        }

        return billRepository.findAllByAccount_IdAndMaturityBetween(accountId, start, end).stream()
                .map(this::toResponseDTO)
                .toList();
    }

    @Transactional
    public BillResponseDTO updateStatus(Long id, StatusBill newStatus) {
        UserAccount currentUser = getAuthenticatedUser();

        Bill bill = billRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Bill not found"));

        if (!bill.getAccount().getUserAccount().getId().equals(currentUser.getId())) {
            throw new RuntimeException("You are not allowed to update this bill");
        }

        StatusBill oldStatus = bill.getStatus();

        if (newStatus == StatusBill.PAID && bill.getPaidAt() == null) {
            bill.setPaidAt(LocalDateTime.now());
        }
        if (newStatus == StatusBill.RECEIVED && bill.getReceivedAt() == null) {
            bill.setReceivedAt(LocalDateTime.now());
        }

        bill.setStatus(newStatus);

        Bill updated = billRepository.save(bill);

        auditLogService.record(currentUser, AuditLogService.UPDATE, "Bill", updated.getId(),
                updated.getDescription() + " -> status " + newStatus);

        notifyStatusChanged(updated, oldStatus);

        return toResponseDTO(updated);
    }

    @Transactional
    public void delete(Long id) {
        UserAccount currentUser = getAuthenticatedUser();

        Bill bill = billRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Bill not found"));

        if (!bill.getAccount().getUserAccount().getId().equals(currentUser.getId())) {
            throw new RuntimeException("You are not allowed to delete this bill");
        }

        billRepository.delete(bill);

        auditLogService.record(currentUser, AuditLogService.DELETE, "Bill", bill.getId(), bill.getDescription());

        notifyDeleted(bill);
    }
}