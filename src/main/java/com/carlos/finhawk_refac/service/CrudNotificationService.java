package com.carlos.finhawk_refac.service;

import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.ArrayList;
import java.util.List;

// Acumula mensagens de notificacao WhatsApp geradas durante operacoes de
// CRUD (lancamento, agenda) por transacao e so as envia se a transacao
// realmente commitar -- evita notificar uma acao que foi revertida por
// uma excecao no meio do caminho (ex: falha de validacao depois do
// primeiro save de um lote). Se muitas mensagens forem geradas na mesma
// transacao (ex: criar um lancamento parcelado em 12x, ou uma futura
// importacao em massa), consolida numa unica mensagem em vez de
// inundar o grupo com uma mensagem por item.
@Component
public class CrudNotificationService {

    private static final int CONSOLIDATION_THRESHOLD = 5;

    private static final ThreadLocal<List<String>> BUFFER = ThreadLocal.withInitial(ArrayList::new);
    private static final ThreadLocal<Boolean> SYNC_REGISTERED = ThreadLocal.withInitial(() -> false);

    private final WhatsAppNotificationService whatsAppNotificationService;

    public CrudNotificationService(WhatsAppNotificationService whatsAppNotificationService) {
        this.whatsAppNotificationService = whatsAppNotificationService;
    }

    public void notify(String message) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            // Sem transacao Spring ativa -- nao ha "commit" pra esperar, manda direto.
            whatsAppNotificationService.sendMessage(message);
            return;
        }

        BUFFER.get().add(message);

        if (Boolean.TRUE.equals(SYNC_REGISTERED.get())) {
            return;
        }

        SYNC_REGISTERED.set(true);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                List<String> messages = BUFFER.get();
                BUFFER.remove();
                SYNC_REGISTERED.remove();

                if (status != TransactionSynchronization.STATUS_COMMITTED || messages.isEmpty()) {
                    return;
                }

                if (messages.size() <= CONSOLIDATION_THRESHOLD) {
                    messages.forEach(whatsAppNotificationService::sendMessage);
                } else {
                    StringBuilder sb = new StringBuilder("📦 " + messages.size() + " atualizações de uma vez:\n");
                    messages.forEach(m -> sb.append("\n").append(m));
                    whatsAppNotificationService.sendMessage(sb.toString());
                }
            }
        });
    }
}
