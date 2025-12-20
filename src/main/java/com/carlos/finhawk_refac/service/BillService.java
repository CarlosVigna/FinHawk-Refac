package com.carlos.finhawk_refac.service;

import com.carlos.finhawk_refac.dto.request.BillRequestDTO;
import com.carlos.finhawk_refac.dto.response.BillResponseDTO;
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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class BillService {

    private final BillRepository billRepository;
    private final CategoryRepository categoryRepository;
    private final AccountRepository accountRepository;

    public BillService(
            BillRepository billRepository,
            CategoryRepository categoryRepository,
            AccountRepository accountRepository
    ) {
        this.billRepository = billRepository;
        this.categoryRepository = categoryRepository;
        this.accountRepository = accountRepository;
    }

    /* ===================== AUTH ===================== */

    private UserAccount getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !(authentication.getPrincipal() instanceof UserAccount)) {
            throw new RuntimeException("Unauthenticated user");
        }

        return (UserAccount) authentication.getPrincipal();
    }

    /* ===================== MAPPER ===================== */

    private BillResponseDTO toResponseDTO(Bill bill) {
        return new BillResponseDTO(
                bill.getId(),
                bill.getDescription(),
                bill.getEmission(),
                bill.getMaturity(),
                bill.getTotalAmount(),
                bill.getInstallmentAmount(),
                bill.getInstallmentCount(),
                bill.getCurrentInstallment(),
                bill.getPeriodicity(),
                bill.getStatus(),
                bill.getCategory().getName()
        );
    }

    /* ===================== CREATE ===================== */

    public BillResponseDTO create(BillRequestDTO dto) {
        UserAccount currentUser = getAuthenticatedUser();

        Account account = accountRepository.findAllByUserAccount(currentUser)
                .stream()
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Account not found"));

        Category category = categoryRepository.findById(dto.categoryId())
                .orElseThrow(() -> new RuntimeException("Category not found"));

        if (!category.getAccount().getId().equals(account.getId())) {
            throw new RuntimeException("Category must belong to the same account");
        }

        Bill bill = new Bill();
        bill.setDescription(dto.description());
        bill.setEmission(dto.emission());
        bill.setMaturity(dto.maturity());
        bill.setTotalAmount(dto.totalAmount());
        bill.setInstallmentCount(dto.installmentCount() != null ? dto.installmentCount() : 1);
        bill.setCurrentInstallment(1);
        bill.setPeriodicity(dto.periodicity());
        bill.setStatus(dto.status() != null ? dto.status() : StatusBill.PENDING);
        bill.setCategory(category);
        bill.setAccount(account);

        calcularParcelas(bill);

        Bill saved = billRepository.save(bill);
        return toResponseDTO(saved);
    }

    /* ===================== UPDATE ===================== */

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

        bill.setDescription(dto.description());
        bill.setEmission(dto.emission());
        bill.setMaturity(dto.maturity());
        bill.setPeriodicity(dto.periodicity());
        bill.setCategory(category);

        if (dto.status() != null) {
            bill.setStatus(dto.status());
        }

        if (dto.totalAmount() != null) {
            bill.setTotalAmount(dto.totalAmount());
        }

        if (dto.installmentCount() != null) {
            bill.setInstallmentCount(dto.installmentCount());
        }

        calcularParcelas(bill);

        Bill updated = billRepository.save(bill);
        return toResponseDTO(updated);
    }

    /* ===================== GETS ===================== */

    public List<BillResponseDTO> getAll() {
        UserAccount currentUser = getAuthenticatedUser();

        return accountRepository.findAllByUserAccount(currentUser).stream()
                .flatMap(account -> billRepository.findAllByAccount(account).stream())
                .map(this::toResponseDTO)
                .toList();
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

        return billRepository.findAllByAccount(account).stream()
                .map(this::toResponseDTO)
                .toList();
    }

    public List<BillResponseDTO> getAllByStatus(StatusBill status) {
        UserAccount currentUser = getAuthenticatedUser();

        return accountRepository.findAllByUserAccount(currentUser).stream()
                .flatMap(account -> billRepository.findAllByAccount(account).stream())
                .filter(bill -> bill.getStatus() == status)
                .map(this::toResponseDTO)
                .toList();
    }

    /* ===================== DELETE ===================== */

    public void delete(Long id) {
        UserAccount currentUser = getAuthenticatedUser();

        Bill bill = billRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Bill not found"));

        if (!bill.getAccount().getUserAccount().getId().equals(currentUser.getId())) {
            throw new RuntimeException("You are not allowed to delete this bill");
        }

        billRepository.delete(bill);
    }

    /* ===================== AUX ===================== */

    private void calcularParcelas(Bill bill) {
        if (bill.getInstallmentCount() != null && bill.getInstallmentCount() > 1) {
            bill.setInstallmentAmount(
                    bill.getTotalAmount().divide(
                            BigDecimal.valueOf(bill.getInstallmentCount()),
                            2,
                            RoundingMode.HALF_UP
                    )
            );
        } else {
            bill.setInstallmentCount(1);
            bill.setInstallmentAmount(bill.getTotalAmount());
        }
    }
}