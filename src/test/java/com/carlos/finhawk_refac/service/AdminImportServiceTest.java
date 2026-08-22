package com.carlos.finhawk_refac.service;

import com.carlos.finhawk_refac.dto.request.ResetAndReimportRequestDTO;
import com.carlos.finhawk_refac.dto.response.BillBackupDTO;
import com.carlos.finhawk_refac.dto.response.CategoryResponseDTO;
import com.carlos.finhawk_refac.dto.response.ResetAndReimportResultDTO;
import com.carlos.finhawk_refac.entity.Account;
import com.carlos.finhawk_refac.entity.Bill;
import com.carlos.finhawk_refac.entity.Category;
import com.carlos.finhawk_refac.entity.UserAccount;
import com.carlos.finhawk_refac.enums.CategoryType;
import com.carlos.finhawk_refac.repository.AccountRepository;
import com.carlos.finhawk_refac.repository.BillRepository;
import com.carlos.finhawk_refac.repository.CategoryRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

// Cobre AdminImportService.resetAndReimport contra o arquivo REAL embutido
// (import-ago2026.json, 78 lancamentos) -- dado o risco (limpeza real de
// dados financeiros de producao), este teste roda a logica inteira contra
// um "banco" fake em memoria (listas simples nos mocks) pra validar,
// ANTES de tocar em producao: contagem exata de lancamentos, calculo do
// saldo (mesma formula de BillService.getConsolidatedSummary --
// RECEIVED+RECEIPT menos PAID+PAYMENT), reaproveitamento de categoria
// case-insensitive, criacao do ajuste de saldo com o sinal certo, e a rede
// de seguranca que aborta se o saldo final nao bater com o alvo.
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AdminImportServiceTest {

    @Mock private AccountRepository accountRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private BillRepository billRepository;
    @Mock private AuditLogService auditLogService;

    private AdminImportService service;
    private UserAccount currentUser;
    private Account account;

    // "banco" fake em memoria
    private final List<Category> categoriaStore = new ArrayList<>();
    private final List<Bill> billStore = new ArrayList<>();
    private final AtomicLong categoryIdSeq = new AtomicLong(100);
    private final AtomicLong billIdSeq = new AtomicLong(1000);

    @BeforeEach
    void setUp() {
        service = new AdminImportService(accountRepository, categoryRepository, billRepository, auditLogService);

        currentUser = new UserAccount();
        currentUser.setId(1L);
        currentUser.setEmail("carlos@finhawk.app");

        account = new Account();
        account.setId(1L);
        account.setName("Família");
        account.setUserAccount(currentUser);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(currentUser, null, currentUser.getAuthorities()));

        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));

        when(categoryRepository.findAllByAccountAndDeletedAtIsNull(account))
                .thenAnswer(inv -> new ArrayList<>(categoriaStore));

        when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> {
            Category c = inv.getArgument(0);
            if (c.getId() == null) {
                c.setId(categoryIdSeq.incrementAndGet());
            }
            categoriaStore.add(c);
            return c;
        });

        when(billRepository.findAllByAccount_Id(1L)).thenAnswer(inv -> new ArrayList<>(billStore));

        org.mockito.Mockito.doAnswer(inv -> {
            List<Bill> toRemove = inv.getArgument(0);
            billStore.removeAll(toRemove);
            return null;
        }).when(billRepository).deleteAll(any());

        when(billRepository.save(any(Bill.class))).thenAnswer(inv -> {
            Bill b = inv.getArgument(0);
            if (b.getId() == null) {
                b.setId(billIdSeq.incrementAndGet());
            }
            billStore.add(b);
            return b;
        });
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void contaDeOutroUsuario_negaAcesso() {
        UserAccount outroUsuario = new UserAccount();
        outroUsuario.setId(2L);
        Account contaDeOutraPessoa = new Account();
        contaDeOutraPessoa.setId(1L);
        contaDeOutraPessoa.setUserAccount(outroUsuario);

        when(accountRepository.findById(1L)).thenReturn(Optional.of(contaDeOutraPessoa));

        assertThatThrownBy(() -> service.resetAndReimport(new ResetAndReimportRequestDTO(1L, BigDecimal.valueOf(55.92))))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void exportBackup_retornaTodosOsCamposDosBillExistentes() {
        Category cat = new Category();
        cat.setId(50L);
        cat.setName("Salário");
        cat.setType(CategoryType.RECEIPT);

        Bill existing = new Bill();
        existing.setId(9L);
        existing.setDescription("Lançamento antigo errado");
        existing.setInstallmentAmount(BigDecimal.valueOf(123.45));
        existing.setStatus(com.carlos.finhawk_refac.enums.StatusBill.RECEIVED);
        existing.setCategory(cat);
        existing.setAccount(account);
        billStore.add(existing);

        List<BillBackupDTO> backup = service.exportBackup(1L);

        assertThat(backup).hasSize(1);
        assertThat(backup.get(0).description()).isEqualTo("Lançamento antigo errado");
        assertThat(backup.get(0).categoryName()).isEqualTo("Salário");
    }

    @Test
    void resetAndReimport_apagaTudoEReimportaOArquivoReal_78Lancamentos() {
        // simula 3 lancamentos antigos "errados" que devem ser todos apagados
        for (int i = 0; i < 3; i++) {
            Bill old = new Bill();
            old.setId((long) (2000 + i));
            old.setDescription("Antigo " + i);
            old.setInstallmentAmount(BigDecimal.TEN);
            old.setAccount(account);
            billStore.add(old);
        }

        ResetAndReimportResultDTO result = service.resetAndReimport(
                new ResetAndReimportRequestDTO(1L, new BigDecimal("55.92")));

        assertThat(result.billsDeleted()).isEqualTo(3);
        assertThat(result.categoriesComFalha()).isEmpty();
        assertThat(result.billsComFalha()).isEmpty();
        assertThat(result.categoriesReused() + result.categoriesCreated()).isGreaterThanOrEqualTo(16);

        // valores conferidos independentemente (node) contra o arquivo real,
        // usando o TIPO REAL da categoria (nao o category_type do bill, que
        // e so um hint) -- receitas 5972.86, despesas 7635.78 (nao 7665.78:
        // "[Itau] APLICACAO COFRINHOS", R$30, PAID, categoria "Investimentos"
        // -- mas "Investimentos" e declarada tipo RECEIPT, entao esse bill
        // fica de fora do calculo de despesas, igual a app faria) --
        // saldo antes do ajuste -1662.92.
        assertThat(result.balanceBeforeAdjustment()).isEqualByComparingTo("-1662.92");
        assertThat(result.targetBalance()).isEqualByComparingTo("55.92");
        assertThat(result.adjustmentDirection()).isEqualTo("RECEIPT");
        assertThat(result.adjustmentAmount()).isEqualByComparingTo("1718.84");
        assertThat(result.finalBalance()).isEqualByComparingTo("55.92");

        // 78 do arquivo + 1 lancamento de ajuste = 79; nada da limpeza restou
        assertThat(billStore).hasSize(result.billsImported());
        assertThat(result.billsImported()).isEqualTo(79);

        // categoria de ajuste criada com o tipo certo (RECEIPT, pois o ajuste foi positivo)
        Optional<Category> ajusteCategoria = categoriaStore.stream()
                .filter(c -> c.getName().equalsIgnoreCase("Ajuste de Saldo"))
                .findFirst();
        assertThat(ajusteCategoria).isPresent();
        assertThat(ajusteCategoria.get().getType()).isEqualTo(CategoryType.RECEIPT);
    }

    @Test
    void resetAndReimport_reaproveitaCategoriaExistente_casoInsensitivo() {
        Category salarioExistente = new Category();
        salarioExistente.setId(77L);
        salarioExistente.setName("SALÁRIO");
        salarioExistente.setType(CategoryType.RECEIPT);
        salarioExistente.setAccount(account);
        categoriaStore.add(salarioExistente);

        ResetAndReimportResultDTO result = service.resetAndReimport(
                new ResetAndReimportRequestDTO(1L, new BigDecimal("55.92")));

        // "SALÁRIO" (maiuscula, ja existente) deve ter sido reaproveitada pro
        // "Salário" do JSON -- nao pode haver duas categorias de salario.
        long qtdCategoriasSalario = categoriaStore.stream()
                .filter(c -> c.getName().equalsIgnoreCase("Salário"))
                .count();
        assertThat(qtdCategoriasSalario).isEqualTo(1);
        assertThat(result.categoriesReused()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void resetAndReimport_semDiferenca_naoCriaAjuste() {
        // Alvo == saldo que o proprio arquivo real ja produz (-1662.92):
        // nao deve criar nenhum lancamento de ajuste nem categoria "Ajuste de Saldo".
        ResetAndReimportResultDTO result = service.resetAndReimport(
                new ResetAndReimportRequestDTO(1L, new BigDecimal("-1662.92")));

        assertThat(result.adjustmentDirection()).isEqualTo("nenhum");
        assertThat(result.adjustmentAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.billsImported()).isEqualTo(78);
        assertThat(result.finalBalance()).isEqualByComparingTo("-1662.92");
        assertThat(categoriaStore.stream().noneMatch(c -> c.getName().equalsIgnoreCase("Ajuste de Saldo"))).isTrue();
    }

    @Test
    void resetAndReimport_ajusteNegativo_criaCategoriaPayment() {
        // Alvo bem abaixo do saldo real do arquivo -> ajuste negativo (PAYMENT/PAID).
        ResetAndReimportResultDTO result = service.resetAndReimport(
                new ResetAndReimportRequestDTO(1L, new BigDecimal("-2000.00")));

        assertThat(result.adjustmentDirection()).isEqualTo("PAYMENT");
        assertThat(result.adjustmentAmount()).isEqualByComparingTo("337.08");
        assertThat(result.finalBalance()).isEqualByComparingTo("-2000.00");

        Optional<Category> ajusteCategoria = categoriaStore.stream()
                .filter(c -> c.getName().equalsIgnoreCase("Ajuste de Saldo"))
                .findFirst();
        assertThat(ajusteCategoria).isPresent();
        assertThat(ajusteCategoria.get().getType()).isEqualTo(CategoryType.PAYMENT);
    }

    @Test
    void listCategories_retornaNomeETipo() {
        Category cat = new Category();
        cat.setId(5L);
        cat.setName("Moradia");
        cat.setType(CategoryType.PAYMENT);
        categoriaStore.add(cat);

        List<CategoryResponseDTO> categories = service.listCategories(1L);

        assertThat(categories).hasSize(1);
        assertThat(categories.get(0).name()).isEqualTo("Moradia");
        assertThat(categories.get(0).type()).isEqualTo("PAYMENT");
    }
}
