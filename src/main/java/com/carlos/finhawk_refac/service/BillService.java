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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import com.carlos.finhawk_refac.dto.response.DashboardSummaryDTO;
import com.carlos.finhawk_refac.dto.response.AccountSummaryDTO;

@Service
@Transactional(readOnly = true)
public class BillService {

    private final BillRepository billRepository;
    private final CategoryRepository categoryRepository;
    private final AccountRepository accountRepository;
    private final PlanLimitService planLimitService;

    public BillService(
            BillRepository billRepository,
            CategoryRepository categoryRepository,
            AccountRepository accountRepository,
            PlanLimitService planLimitService
    ) {
        this.billRepository = billRepository;
        this.categoryRepository = categoryRepository;
        this.accountRepository = accountRepository;
        this.planLimitService = planLimitService;
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
        planLimitService.checkBillLimit(currentUser);

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

        if (newStatus == StatusBill.PAID && bill.getPaidAt() == null) {
            bill.setPaidAt(LocalDateTime.now());
        }
        if (newStatus == StatusBill.RECEIVED && bill.getReceivedAt() == null) {
            bill.setReceivedAt(LocalDateTime.now());
        }

        bill.setStatus(newStatus);

        Bill updated = billRepository.save(bill);
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
    }
}