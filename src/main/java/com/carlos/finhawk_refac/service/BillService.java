package com.carlos.finhawk_refac.service;

import com.carlos.finhawk_refac.dto.request.BillRequestDTO;
import com.carlos.finhawk_refac.dto.response.BillResponseDTO;
import com.carlos.finhawk_refac.dto.response.CategoryResponseDTO;
import com.carlos.finhawk_refac.entity.Account;
import com.carlos.finhawk_refac.entity.Bill;
import com.carlos.finhawk_refac.entity.Category;
import com.carlos.finhawk_refac.entity.UserAccount;
import com.carlos.finhawk_refac.enums.Periodicity;
import com.carlos.finhawk_refac.enums.StatusBill;
import com.carlos.finhawk_refac.repository.AccountRepository;
import com.carlos.finhawk_refac.repository.BillRepository;
import com.carlos.finhawk_refac.repository.CategoryRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
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
                bill.getInstallmentAmount(),
                bill.getInstallmentCount(),
                bill.getCurrentInstallment(),
                bill.getStatus(),
                bill.getAccount().getId(),
                new CategoryResponseDTO(
                        bill.getCategory().getId(),
                        bill.getCategory().getName(),
                        bill.getCategory().getType().name()
                )
        );
    }

    /* ===================== CREATE ===================== */
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

        // Define quantas parcelas serão criadas (mínimo 1)
        int totalInstallments = (dto.installmentCount() != null && dto.installmentCount() > 0) ? dto.installmentCount() : 1;

        List<Bill> billsToSave = new ArrayList<>();

        for (int i = 0; i < totalInstallments; i++) {
            Bill bill = new Bill();

            // Copia os dados básicos
            bill.setDescription(dto.description());
            bill.setEmission(dto.emission());
            bill.setInstallmentAmount(dto.installmentAmount() != null ? dto.installmentAmount() : BigDecimal.ZERO);
            bill.setInstallmentCount(totalInstallments);
            bill.setPeriodicity(dto.periodicity());
            bill.setCategory(category);
            bill.setAccount(account);

            // Define o número da parcela atual (i começa em 0, então somamos 1)
            bill.setCurrentInstallment(i + 1);

            // === LÓGICA DE DATA DE VENCIMENTO ===
            LocalDate maturityDate = dto.maturity();

            // Se não for a primeira parcela, calcula a nova data
            if (i > 0) {
                switch (dto.periodicity()) {
                    case MONTHLY:
                        maturityDate = maturityDate.plusMonths(i);
                        break;
                    case BIMONTHLY:
                        maturityDate = maturityDate.plusMonths(i * 2L);
                        break;
                    case QUARTERLY:
                        maturityDate = maturityDate.plusMonths(i * 3L);
                        break;
                    case SEMIANNUAL:
                        maturityDate = maturityDate.plusMonths(i * 6L);
                        break;
                    case ANNUAL:
                        maturityDate = maturityDate.plusYears(i);
                        break;
                    // Caso padrão (se adicionar outros no futuro) cai aqui
                    default:
                        maturityDate = maturityDate.plusMonths(i);
                        break;
                }
            }
            bill.setMaturity(maturityDate);

            // === LÓGICA DE STATUS ===
            // A primeira parcela pega o status que veio do front (Ex: PAGO ou PENDENTE).
            // As parcelas futuras (2, 3...) nascem sempre como PENDENTE,
            // a não ser que você queira que todas nasçam como pagas (se for o caso, remova o if).
            if (i == 0) {
                bill.setStatus(dto.status() != null ? dto.status() : StatusBill.PENDING);
            } else {
                bill.setStatus(StatusBill.PENDING);
            }

            billsToSave.add(bill);
        }

        // Salva todas as parcelas no banco de uma vez
        List<Bill> savedBills = billRepository.saveAll(billsToSave);

        // Retorna o DTO da primeira parcela criada para o front-end exibir feedback
        return toResponseDTO(savedBills.get(0));
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

        if (dto.installmentAmount() != null) {
            bill.setInstallmentAmount(dto.installmentAmount());
        }

        if (dto.installmentCount() != null) {
            bill.setInstallmentCount(dto.installmentCount());
        }

        Bill updated = billRepository.save(bill);
        return toResponseDTO(updated);
    }

    /* ===================== GETS ===================== */
    public List<BillResponseDTO> getAll() {
        UserAccount currentUser = getAuthenticatedUser();

        return accountRepository.findAllByUserAccount(currentUser).stream()
                .flatMap(account -> billRepository.findAllByAccount_Id(account.getId()).stream())
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

        List<Bill> bills = billRepository.findAllByAccount_Id(accountId);

        return bills.stream()
                .map(this::toResponseDTO)
                .toList();
    }

    public List<BillResponseDTO> getAllByStatus(StatusBill status) {
        UserAccount currentUser = getAuthenticatedUser();

        return accountRepository.findAllByUserAccount(currentUser).stream()
                .flatMap(account -> billRepository.findAllByAccount_Id(account.getId()).stream())
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
}