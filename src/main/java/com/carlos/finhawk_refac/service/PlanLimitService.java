package com.carlos.finhawk_refac.service;

import com.carlos.finhawk_refac.entity.UserAccount;
import com.carlos.finhawk_refac.enums.PlanType;
import com.carlos.finhawk_refac.exception.PlanLimitException;
import com.carlos.finhawk_refac.repository.AccountRepository;
import com.carlos.finhawk_refac.repository.BillRepository;
import com.carlos.finhawk_refac.repository.ChecklistItemRepository;
import org.springframework.stereotype.Service;

@Service
public class PlanLimitService {

    private static final int FREE_MAX_ACCOUNTS  = 1;
    private static final int FREE_MAX_BILLS     = 100;
    private static final int FREE_MAX_CHECKLIST = 5;

    private final AccountRepository accountRepository;
    private final BillRepository billRepository;
    private final ChecklistItemRepository checklistItemRepository;

    public PlanLimitService(AccountRepository accountRepository,
                            BillRepository billRepository,
                            ChecklistItemRepository checklistItemRepository) {
        this.accountRepository = accountRepository;
        this.billRepository = billRepository;
        this.checklistItemRepository = checklistItemRepository;
    }

    public void checkAccountLimit(UserAccount user) {
        if (user.getPlan() == PlanType.PRO) return;
        long count = accountRepository.countByUserAccount(user);
        if (count >= FREE_MAX_ACCOUNTS) {
            throw new PlanLimitException("accounts",
                    "Plano gratuito permite apenas 1 conta financeira. Faça upgrade para o plano Pro.");
        }
    }

    public void checkBillLimit(UserAccount user) {
        if (user.getPlan() == PlanType.PRO) return;
        long count = billRepository.countByAccount_UserAccount_Id(user.getId());
        if (count >= FREE_MAX_BILLS) {
            throw new PlanLimitException("bills",
                    "Plano gratuito permite até 100 lançamentos. Faça upgrade para o plano Pro.");
        }
    }

    public void checkChecklistLimit(UserAccount user) {
        if (user.getPlan() == PlanType.PRO) return;
        long count = checklistItemRepository.countByAccount_UserAccount_IdAndActiveTrue(user.getId());
        if (count >= FREE_MAX_CHECKLIST) {
            throw new PlanLimitException("checklist",
                    "Plano gratuito permite até 5 itens no checklist. Faça upgrade para o plano Pro.");
        }
    }
}
