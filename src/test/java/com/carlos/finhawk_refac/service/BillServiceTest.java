package com.carlos.finhawk_refac.service;

import com.carlos.finhawk_refac.dto.response.AccountSummaryDTO;
import com.carlos.finhawk_refac.dto.response.DashboardSummaryDTO;
import com.carlos.finhawk_refac.entity.Account;
import com.carlos.finhawk_refac.entity.Bill;
import com.carlos.finhawk_refac.entity.Category;
import com.carlos.finhawk_refac.entity.UserAccount;
import com.carlos.finhawk_refac.enums.CategoryType;
import com.carlos.finhawk_refac.enums.StatusBill;
import com.carlos.finhawk_refac.repository.AccountRepository;
import com.carlos.finhawk_refac.repository.BillRepository;
import com.carlos.finhawk_refac.repository.CategoryRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

// Cobre a agregacao financeira que alimenta o Dashboard (soma de lancamentos
// por status/tipo, saldo consolidado) -- o ponto mais critico de "calcular
// dinheiro errado silenciosamente" no sistema.
@ExtendWith(MockitoExtension.class)
class BillServiceTest {

    @Mock
    private BillRepository billRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private AccountRepository accountRepository;
    @Mock
    private AuditLogService auditLogService;

    private BillService billService;
    private UserAccount currentUser;
    private Account account;

    @BeforeEach
    void setUp() {
        billService = new BillService(billRepository, categoryRepository, accountRepository, auditLogService);

        currentUser = new UserAccount();
        currentUser.setId(1L);
        currentUser.setEmail("teste@finhawk.app");

        account = new Account();
        account.setId(10L);
        account.setName("Conta Teste");
        account.setUserAccount(currentUser);

        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(currentUser, null));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private Category category(CategoryType type) {
        Category c = new Category();
        c.setId(100L);
        c.setName(type == CategoryType.RECEIPT ? "Salário" : "Mercado");
        c.setType(type);
        c.setAccount(account);
        return c;
    }

    private Bill bill(StatusBill status, CategoryType type, String amount) {
        Bill b = new Bill();
        b.setStatus(status);
        b.setCategory(category(type));
        b.setAccount(account);
        b.setInstallmentAmount(amount == null ? null : new BigDecimal(amount));
        return b;
    }

    @Test
    void somaSimples_receitaMenosDespesa() {
        when(accountRepository.findAllByUserAccount(currentUser)).thenReturn(List.of(account));
        when(billRepository.findAllByAccount_Id(10L)).thenReturn(List.of(
                bill(StatusBill.RECEIVED, CategoryType.RECEIPT, "1000.00"),
                bill(StatusBill.PAID, CategoryType.PAYMENT, "300.00")
        ));

        DashboardSummaryDTO summary = billService.getConsolidatedSummary();

        assertThat(summary.patrimonioConsolidado()).isEqualByComparingTo("700.00");
        AccountSummaryDTO accSummary = summary.accounts().get(0);
        assertThat(accSummary.receitasRealizadas()).isEqualByComparingTo("1000.00");
        assertThat(accSummary.despesasRealizadas()).isEqualByComparingTo("300.00");
        assertThat(accSummary.saldoRealizado()).isEqualByComparingTo("700.00");
    }

    @Test
    void contaSemLancamentos_retornaZerado() {
        when(accountRepository.findAllByUserAccount(currentUser)).thenReturn(List.of(account));
        when(billRepository.findAllByAccount_Id(10L)).thenReturn(List.of());

        DashboardSummaryDTO summary = billService.getConsolidatedSummary();

        assertThat(summary.patrimonioConsolidado()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(summary.accounts().get(0).saldoRealizado()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void ignoraLancamentosPendentes_soContaPagoOuRecebido() {
        when(accountRepository.findAllByUserAccount(currentUser)).thenReturn(List.of(account));
        when(billRepository.findAllByAccount_Id(10L)).thenReturn(List.of(
                bill(StatusBill.PENDING, CategoryType.RECEIPT, "500.00"),
                bill(StatusBill.PENDING, CategoryType.PAYMENT, "200.00"),
                bill(StatusBill.RECEIVED, CategoryType.RECEIPT, "100.00")
        ));

        DashboardSummaryDTO summary = billService.getConsolidatedSummary();

        // pendentes nao entram na soma realizada -- so o RECEIVED de 100 conta
        assertThat(summary.patrimonioConsolidado()).isEqualByComparingTo("100.00");
    }

    @Test
    void precisaoDecimal_naoPerdeCentavos() {
        when(accountRepository.findAllByUserAccount(currentUser)).thenReturn(List.of(account));
        when(billRepository.findAllByAccount_Id(10L)).thenReturn(List.of(
                bill(StatusBill.RECEIVED, CategoryType.RECEIPT, "19.99"),
                bill(StatusBill.RECEIVED, CategoryType.RECEIPT, "0.01"),
                bill(StatusBill.PAID, CategoryType.PAYMENT, "0.33"),
                bill(StatusBill.PAID, CategoryType.PAYMENT, "0.33"),
                bill(StatusBill.PAID, CategoryType.PAYMENT, "0.34")
        ));

        DashboardSummaryDTO summary = billService.getConsolidatedSummary();

        AccountSummaryDTO accSummary = summary.accounts().get(0);
        assertThat(accSummary.receitasRealizadas()).isEqualByComparingTo("20.00");
        assertThat(accSummary.despesasRealizadas()).isEqualByComparingTo("1.00");
        assertThat(accSummary.saldoRealizado()).isEqualByComparingTo("19.00");
    }

    @Test
    void multiplasContas_somamNoPatrimonioConsolidado() {
        Account account2 = new Account();
        account2.setId(20L);
        account2.setName("Segunda Conta");
        account2.setUserAccount(currentUser);

        Category receita2 = new Category();
        receita2.setId(200L);
        receita2.setType(CategoryType.RECEIPT);
        receita2.setAccount(account2);

        Bill billConta2 = new Bill();
        billConta2.setStatus(StatusBill.RECEIVED);
        billConta2.setCategory(receita2);
        billConta2.setAccount(account2);
        billConta2.setInstallmentAmount(new BigDecimal("50.00"));

        when(accountRepository.findAllByUserAccount(currentUser)).thenReturn(List.of(account, account2));
        when(billRepository.findAllByAccount_Id(10L)).thenReturn(List.of(
                bill(StatusBill.RECEIVED, CategoryType.RECEIPT, "100.00")
        ));
        when(billRepository.findAllByAccount_Id(20L)).thenReturn(List.of(billConta2));

        DashboardSummaryDTO summary = billService.getConsolidatedSummary();

        assertThat(summary.accountCount()).isEqualTo(2);
        assertThat(summary.patrimonioConsolidado()).isEqualByComparingTo("150.00");
    }
}
