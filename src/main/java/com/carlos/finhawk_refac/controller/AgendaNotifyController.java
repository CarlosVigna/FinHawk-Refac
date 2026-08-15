package com.carlos.finhawk_refac.controller;

import com.carlos.finhawk_refac.dto.response.AgendaNotifyResponseDTO;
import com.carlos.finhawk_refac.scheduler.AgendaNotificationScheduler;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Resumos sob demanda via WhatsApp -- botoes na pagina de Agenda que disparam
// na hora, reaproveitando a mesma logica de consulta/montagem dos jobs
// automaticos de AgendaNotificationScheduler.
@RestController
@RequestMapping("/agenda/notify")
public class AgendaNotifyController {

    private final AgendaNotificationScheduler agendaNotificationScheduler;

    public AgendaNotifyController(AgendaNotificationScheduler agendaNotificationScheduler) {
        this.agendaNotificationScheduler = agendaNotificationScheduler;
    }

    @PostMapping("/today")
    public ResponseEntity<AgendaNotifyResponseDTO> notifyToday() {
        int itemCount = agendaNotificationScheduler.notifyToday();
        return ResponseEntity.ok(new AgendaNotifyResponseDTO(true, itemCount));
    }

    @PostMapping("/week")
    public ResponseEntity<AgendaNotifyResponseDTO> notifyWeek() {
        int itemCount = agendaNotificationScheduler.notifyWeek();
        return ResponseEntity.ok(new AgendaNotifyResponseDTO(true, itemCount));
    }

    @PostMapping("/open-bills")
    public ResponseEntity<AgendaNotifyResponseDTO> notifyOpenBills() {
        int itemCount = agendaNotificationScheduler.notifyOpenBills();
        return ResponseEntity.ok(new AgendaNotifyResponseDTO(true, itemCount));
    }
}
