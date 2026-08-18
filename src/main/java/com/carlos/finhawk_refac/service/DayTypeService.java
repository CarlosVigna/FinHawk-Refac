package com.carlos.finhawk_refac.service;

import com.carlos.finhawk_refac.dto.request.DayTypeOverrideRequestDTO;
import com.carlos.finhawk_refac.dto.response.DayTypeResponseDTO;
import com.carlos.finhawk_refac.entity.Account;
import com.carlos.finhawk_refac.entity.AgendaEvent;
import com.carlos.finhawk_refac.entity.DayTypeOverride;
import com.carlos.finhawk_refac.entity.UserAccount;
import com.carlos.finhawk_refac.enums.DayType;
import com.carlos.finhawk_refac.enums.RecurrenceFrequency;
import com.carlos.finhawk_refac.repository.AccountRepository;
import com.carlos.finhawk_refac.repository.DayTypeOverrideRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

// Calcula os tipos de dia (plantao/folga/entrega/fim de semana) aplicaveis a
// uma data, e gerencia a escolha manual (DayTypeOverride) que tem
// prioridade sobre o calculo automatico pra dimensao plantao/folga.
//
// O ciclo de plantao/folga e fixo pra sempre, combinado com o usuario:
// 15/08/2026 = PLANTAO, alternando dia a dia a partir dai. De proposito uma
// constante fixa no codigo (nao variavel de ambiente/editavel pelo usuario)
// nesta versao -- mudar isso exigiria trocar o codigo e fazer deploy. Isso
// e so a BASE: o override manual sobrepoe quando o usuario troca de
// plantao na pratica (ver setOverride).
@Service
@Transactional(readOnly = true)
public class DayTypeService {

    private static final LocalDate PLANTAO_ANCHOR = LocalDate.of(2026, 8, 15);

    private final DayTypeOverrideRepository dayTypeOverrideRepository;
    private final AccountRepository accountRepository;

    public DayTypeService(DayTypeOverrideRepository dayTypeOverrideRepository, AccountRepository accountRepository) {
        this.dayTypeOverrideRepository = dayTypeOverrideRepository;
        this.accountRepository = accountRepository;
    }

    private UserAccount getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !(authentication.getPrincipal() instanceof UserAccount)) {
            throw new RuntimeException("Unauthenticated user");
        }

        return (UserAccount) authentication.getPrincipal();
    }

    private Account requireOwnedAccount(Long accountId) {
        UserAccount currentUser = getAuthenticatedUser();

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Account not found"));

        if (!account.getUserAccount().getId().equals(currentUser.getId())) {
            throw new RuntimeException("Access denied");
        }

        return account;
    }

    // Toda data tem exatamente um de {PLANTAO, FOLGA} + exatamente um de
    // {ENTREGA, FIM_DE_SEMANA} -- sempre 2 etiquetas. PLANTAO/FOLGA vem do
    // override manual da conta quando existir; senao cai no calculo
    // automatico a partir da ancora.
    public Set<DayType> calculate(Long accountId, LocalDate date) {
        DayType manual = dayTypeOverrideRepository.findByAccount_IdAndDate(accountId, date)
                .map(DayTypeOverride::getDayType)
                .orElse(null);

        DayType onCallTag = manual != null ? manual : automaticOnCallTag(date);
        return EnumSet.of(onCallTag, weekdayTag(date));
    }

    private DayType automaticOnCallTag(LocalDate date) {
        long diff = ChronoUnit.DAYS.between(PLANTAO_ANCHOR, date);
        // Math.floorMod (nao o operador %) pra tratar corretamente datas
        // anteriores ao anchor, onde diff e negativo.
        boolean isPlantao = Math.floorMod(diff, 2) == 0;
        return isPlantao ? DayType.PLANTAO : DayType.FOLGA;
    }

    private DayType weekdayTag(LocalDate date) {
        DayOfWeek dow = date.getDayOfWeek();
        boolean isWeekend = dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY;
        return isWeekend ? DayType.FIM_DE_SEMANA : DayType.ENTREGA;
    }

    // "Esse habito ocorre nessa data?" -- se o habito tiver etiquetas de tipo
    // de dia configuradas, elas tem prioridade (logica OU: basta uma bater
    // com o tipo do dia) e a frequencia antiga (DAILY/WEEKLY) e ignorada. Se
    // nao tiver etiqueta nenhuma, usa a logica antiga como sempre foi.
    public boolean habitOccursOn(AgendaEvent habit, LocalDate date) {
        if (habit.getDayTypeTags() != null && !habit.getDayTypeTags().isEmpty()) {
            Set<DayType> todayTags = calculate(habit.getAccount().getId(), date);
            return habit.getDayTypeTags().stream().anyMatch(todayTags::contains);
        }

        if (habit.getRecurrenceFrequency() == RecurrenceFrequency.DAILY) {
            return true;
        }
        if (habit.getRecurrenceFrequency() == RecurrenceFrequency.WEEKLY) {
            return habit.getDaysOfWeek() != null && habit.getDaysOfWeek().contains(date.getDayOfWeek());
        }
        return false;
    }

    // ===== Override manual (plantao/folga) =====

    public DayTypeResponseDTO getEffectiveDayType(Long accountId, LocalDate date) {
        requireOwnedAccount(accountId);

        boolean overridden = dayTypeOverrideRepository.findByAccount_IdAndDate(accountId, date).isPresent();
        List<DayType> tags = calculate(accountId, date).stream().sorted().toList();

        return new DayTypeResponseDTO(date, tags, overridden);
    }

    @Transactional
    public DayTypeResponseDTO setOverride(Long accountId, DayTypeOverrideRequestDTO dto) {
        Account account = requireOwnedAccount(accountId);

        if (dto.date() == null) {
            throw new RuntimeException("A data é obrigatória.");
        }

        if (dto.dayType() != DayType.PLANTAO && dto.dayType() != DayType.FOLGA) {
            throw new RuntimeException("Só é possível escolher manualmente entre plantão e folga.");
        }

        DayTypeOverride override = dayTypeOverrideRepository.findByAccount_IdAndDate(accountId, dto.date())
                .orElseGet(DayTypeOverride::new);
        override.setAccount(account);
        override.setDate(dto.date());
        override.setDayType(dto.dayType());
        dayTypeOverrideRepository.save(override);

        // Monta a resposta com o valor que acabamos de salvar em vez de
        // reconsultar via calculate() -- o override que acabou de ser
        // gravado e a fonte da verdade aqui, sem depender de round-trip
        // (flush/timing) pra refletir o proprio save que fizemos agora.
        List<DayType> tags = List.of(dto.dayType(), weekdayTag(dto.date())).stream().sorted().toList();
        return new DayTypeResponseDTO(dto.date(), tags, true);
    }
}
