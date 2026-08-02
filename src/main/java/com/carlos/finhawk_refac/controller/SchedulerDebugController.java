package com.carlos.finhawk_refac.controller;

import com.carlos.finhawk_refac.entity.Bill;
import com.carlos.finhawk_refac.entity.NotificationLog;
import com.carlos.finhawk_refac.enums.StatusBill;
import com.carlos.finhawk_refac.repository.BillRepository;
import com.carlos.finhawk_refac.repository.NotificationLogRepository;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

// TEMPORARIO -- diagnostico pra validacao ao vivo dos schedulers de
// notificacao via WhatsApp (Parte 6 do FINHAWK_AGENDA_WHATSAPP.md).
// Remover esse controller assim que a validacao terminar. Requer
// autenticacao normalmente (nao esta na lista de permitAll do
// SecurityConfigurations, entao cai no anyRequest().authenticated()).
@RestController
@RequestMapping("/agenda/_debug")
public class SchedulerDebugController {

    private static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");

    private final BillRepository billRepository;
    private final NotificationLogRepository notificationLogRepository;

    public SchedulerDebugController(BillRepository billRepository, NotificationLogRepository notificationLogRepository) {
        this.billRepository = billRepository;
        this.notificationLogRepository = notificationLogRepository;
    }

    @GetMapping("/scheduler-status")
    public Map<String, Object> status() {
        LocalDate today = LocalDate.now(ZONE);
        LocalDate weekEnd = today.plusDays(7);

        List<Bill> billsToday = billRepository.findAllByMaturityAndStatus(today, StatusBill.PENDING);
        List<Bill> billsNext7 = billRepository.findAllByMaturityBetweenAndStatus(today.plusDays(1), weekEnd, StatusBill.PENDING);
        List<NotificationLog> allLogs = notificationLogRepository.findAll();

        return Map.of(
                "serverTimeSaoPaulo", LocalDateTime.now(ZONE).toString(),
                "today", today.toString(),
                "billsTodayCount", billsToday.size(),
                "billsTodayDescriptions", billsToday.stream().map(Bill::getDescription).toList(),
                "billsNext7Count", billsNext7.size(),
                "billsNext7Descriptions", billsNext7.stream().map(Bill::getDescription).toList(),
                "notificationLogEntries", allLogs.stream()
                        .map(l -> l.getJobKey() + " | " + l.getReferenceDate() + " | " + l.getSentAt())
                        .toList()
        );
    }

    @DeleteMapping("/notification-log/today")
    public Map<String, Object> clearTodayLogs() {
        LocalDate today = LocalDate.now(ZONE);
        List<NotificationLog> toDelete = notificationLogRepository.findAll().stream()
                .filter(l -> l.getReferenceDate().equals(today))
                .toList();
        notificationLogRepository.deleteAll(toDelete);
        return Map.of("deletedCount", toDelete.size());
    }
}
