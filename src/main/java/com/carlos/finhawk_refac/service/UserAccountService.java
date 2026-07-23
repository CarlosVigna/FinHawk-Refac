package com.carlos.finhawk_refac.service;

import com.carlos.finhawk_refac.dto.request.UserAccountRequestDTO;
import com.carlos.finhawk_refac.dto.request.UserDataImportDTO;
import com.carlos.finhawk_refac.dto.response.AccountResponseDTO;
import com.carlos.finhawk_refac.dto.response.BillResponseDTO;
import com.carlos.finhawk_refac.dto.response.CategoryResponseDTO;
import com.carlos.finhawk_refac.dto.response.ChecklistItemResponseDTO;
import com.carlos.finhawk_refac.dto.response.UserAccountResponseDTO;
import com.carlos.finhawk_refac.dto.response.UserDataExportDTO;
import com.carlos.finhawk_refac.entity.Account;
import com.carlos.finhawk_refac.entity.Bill;
import com.carlos.finhawk_refac.entity.Category;
import com.carlos.finhawk_refac.entity.ChecklistItem;
import com.carlos.finhawk_refac.entity.UserAccount;
import com.carlos.finhawk_refac.enums.CategoryType;
import com.carlos.finhawk_refac.enums.Periodicity;
import com.carlos.finhawk_refac.enums.StatusBill;
import com.carlos.finhawk_refac.enums.UserRole;
import com.carlos.finhawk_refac.repository.AccountRepository;
import com.carlos.finhawk_refac.repository.BillRepository;
import com.carlos.finhawk_refac.repository.CategoryRepository;
import com.carlos.finhawk_refac.repository.ChecklistItemRepository;
import com.carlos.finhawk_refac.repository.UserAccountRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class UserAccountService {

    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final AccountRepository accountRepository;
    private final CategoryRepository categoryRepository;
    private final BillRepository billRepository;
    private final ChecklistItemRepository checklistItemRepository;

    public UserAccountService(UserAccountRepository userAccountRepository,
                               PasswordEncoder passwordEncoder,
                               AccountRepository accountRepository,
                               CategoryRepository categoryRepository,
                               BillRepository billRepository,
                               ChecklistItemRepository checklistItemRepository) {
        this.userAccountRepository = userAccountRepository;
        this.passwordEncoder = passwordEncoder;
        this.accountRepository = accountRepository;
        this.categoryRepository = categoryRepository;
        this.billRepository = billRepository;
        this.checklistItemRepository = checklistItemRepository;
    }

    private UserAccount getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserAccount)) {
            throw new RuntimeException("Unauthenticated user");
        }
        return (UserAccount) authentication.getPrincipal();
    }

    public UserAccountResponseDTO update(Long id, UserAccountRequestDTO userDto) {
        UserAccount currentUser = getAuthenticatedUser();
        if (!currentUser.getId().equals(id) && currentUser.getRole() != UserRole.ADMIN) {
            throw new RuntimeException("Access not allowed");
        }

        UserAccount oldUser = userAccountRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (userDto.name() != null) oldUser.setName(userDto.name());

        if (userDto.email() != null && !userDto.email().equalsIgnoreCase(oldUser.getEmail())) {
            if (userAccountRepository.findByEmail(userDto.email()).isPresent()) {
                throw new RuntimeException("Email already registered");
            }
            oldUser.setEmail(userDto.email());
        }

        if (userDto.password() != null && !userDto.password().isBlank()) {
            oldUser.setPassword(passwordEncoder.encode(userDto.password()));
        }

        UserAccount updated = userAccountRepository.save(oldUser);

        return new UserAccountResponseDTO(
                updated.getId(),
                updated.getName(),
                updated.getEmail(),
                updated.getRole().name()
        );
    }

    @Transactional(readOnly = true)
    public UserDataExportDTO exportUserData() {
        UserAccount currentUser = getAuthenticatedUser();

        List<Account> accounts = accountRepository.findAllByUserAccount(currentUser);

        List<AccountResponseDTO> accountDTOs = accounts.stream()
                .map(a -> new AccountResponseDTO(a.getId(), a.getName(), a.getPhotoUrl()))
                .toList();

        List<UserDataExportDTO.CategoryExportDTO> categoryDTOs = accounts.stream()
                .flatMap(a -> categoryRepository.findAllByAccountAndDeletedAtIsNull(a).stream()
                        .map(c -> new UserDataExportDTO.CategoryExportDTO(
                                c.getId(), c.getName(), c.getType().name(), a.getId())))
                .toList();

        List<BillResponseDTO> billDTOs = accounts.stream()
                .flatMap(a -> billRepository.findAllByAccount_Id(a.getId()).stream()
                        .map(b -> new BillResponseDTO(
                                b.getId(),
                                b.getDescription(),
                                b.getEmission(),
                                b.getMaturity(),
                                b.getInstallmentAmount(),
                                b.getInstallmentCount(),
                                b.getCurrentInstallment(),
                                b.getStatus(),
                                b.getPeriodicity(),
                                b.getAccount().getId(),
                                new CategoryResponseDTO(
                                        b.getCategory().getId(),
                                        b.getCategory().getName(),
                                        b.getCategory().getType().name()
                                ),
                                b.getCreatedAt(),
                                b.getUpdatedAt(),
                                b.getPaidAt(),
                                b.getReceivedAt()
                        )))
                .toList();

        List<ChecklistItemResponseDTO> checklistDTOs = accounts.stream()
                .flatMap(a -> checklistItemRepository.findAllByAccount_Id(a.getId()).stream()
                        .map(ci -> new ChecklistItemResponseDTO(
                                ci.getId(),
                                ci.getDescription(),
                                ci.getDueDay(),
                                ci.getActive(),
                                ci.getAccount().getId(),
                                ci.getApproximateValue()
                        )))
                .toList();

        return new UserDataExportDTO(
                LocalDateTime.now(),
                "1.0",
                new UserDataExportDTO.UserExportDTO(
                        currentUser.getId(),
                        currentUser.getName(),
                        currentUser.getEmail()
                ),
                accountDTOs,
                categoryDTOs,
                billDTOs,
                checklistDTOs
        );
    }

    @Transactional
    public void importUserData(UserDataImportDTO dto) {
        if (!"1.0".equals(dto.version())) {
            throw new RuntimeException("Backup version invalid. Expected: 1.0");
        }

        UserAccount currentUser = getAuthenticatedUser();

        Map<Long, Account> accountMap = new HashMap<>();
        if (dto.accounts() != null) {
            for (UserDataImportDTO.AccountImportData a : dto.accounts()) {
                if (a.name() == null) continue;
                Account account = accountRepository
                        .findByUserAccountAndName(currentUser, a.name())
                        .orElseGet(() -> {
                            Account newAccount = new Account();
                            newAccount.setName(a.name());
                            newAccount.setPhotoUrl(a.photoUrl());
                            newAccount.setUserAccount(currentUser);
                            return accountRepository.save(newAccount);
                        });
                if (a.id() != null) {
                    accountMap.put(a.id(), account);
                }
            }
        }

        Map<Long, Category> categoryMap = new HashMap<>();
        if (dto.categories() != null) {
            for (UserDataImportDTO.CategoryImportData c : dto.categories()) {
                if (c.accountId() == null || !accountMap.containsKey(c.accountId())) continue;
                if (c.name() == null) continue;
                Account ownerAccount = accountMap.get(c.accountId());

                Category category = categoryRepository
                        .findByAccountAndNameAndDeletedAtIsNull(ownerAccount, c.name())
                        .orElseGet(() -> {
                            Category newCategory = new Category();
                            newCategory.setName(c.name());
                            newCategory.setType(CategoryType.valueOf(c.type()));
                            newCategory.setAccount(ownerAccount);
                            return categoryRepository.save(newCategory);
                        });
                if (c.id() != null) {
                    categoryMap.put(c.id(), category);
                }
            }
        }

        if (dto.bills() != null) {
            for (UserDataImportDTO.BillImportData b : dto.bills()) {
                if (b.accountId() == null || !accountMap.containsKey(b.accountId())) continue;
                if (b.category() == null || b.category().id() == null) continue;
                if (!categoryMap.containsKey(b.category().id())) continue;

                Account ownerAccount = accountMap.get(b.accountId());
                Category category = categoryMap.get(b.category().id());

                boolean alreadyExists = billRepository
                        .existsByAccount_IdAndDescriptionAndMaturityAndInstallmentAmount(
                                ownerAccount.getId(),
                                b.description(),
                                b.maturity(),
                                b.installmentAmount()
                        );
                if (alreadyExists) continue;

                Bill newBill = new Bill();
                newBill.setDescription(b.description());
                newBill.setEmission(b.emission());
                newBill.setMaturity(b.maturity());
                newBill.setInstallmentAmount(b.installmentAmount());
                newBill.setInstallmentCount(b.installmentCount() != null ? b.installmentCount() : 1);
                newBill.setCurrentInstallment(b.currentInstallment() != null ? b.currentInstallment() : 1);
                newBill.setStatus(b.status() != null ? b.status() : StatusBill.PENDING);
                newBill.setPeriodicity(b.periodicity() != null
                        ? Periodicity.valueOf(b.periodicity())
                        : Periodicity.MONTHLY);
                newBill.setAccount(ownerAccount);
                newBill.setCategory(category);
                newBill.setPaidAt(b.paidAt());
                newBill.setReceivedAt(b.receivedAt());
                billRepository.save(newBill);
            }
        }

        if (dto.checklistItems() != null) {
            for (UserDataImportDTO.ChecklistItemImportData ci : dto.checklistItems()) {
                if (ci.accountId() == null || !accountMap.containsKey(ci.accountId())) continue;
                if (ci.description() == null) continue;
                Account ownerAccount = accountMap.get(ci.accountId());

                boolean alreadyExists = checklistItemRepository
                        .existsByAccount_IdAndDescriptionAndActiveTrue(
                                ownerAccount.getId(), ci.description());
                if (alreadyExists) continue;

                ChecklistItem newItem = new ChecklistItem();
                newItem.setDescription(ci.description());
                newItem.setDueDay(ci.dueDay());
                newItem.setActive(ci.active() != null ? ci.active() : Boolean.TRUE);
                newItem.setAccount(ownerAccount);
                checklistItemRepository.save(newItem);
            }
        }
    }

    // ATENCAO: sem checagem de dono/autenticacao de proposito -- so deve ser
    // chamado com um id que ja veio de uma fonte confiavel e pre-validada
    // (hoje: PasswordResetService, apos validar token de reset expirado/
    // uso unico). NUNCA exponha isso direto num controller recebendo id do
    // path/body sem antes confirmar contra o usuario autenticado.
    public void updatePassword(Long id, String newPassword) {
        UserAccount user = userAccountRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setPassword(passwordEncoder.encode(newPassword));
        userAccountRepository.save(user);
    }

    public void deleteCurrentUser(UserAccount user) {
        userAccountRepository.delete(user);
    }

}
