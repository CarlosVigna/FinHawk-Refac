package com.carlos.finhawk_refac.service;

// Flag de contexto (por thread) que operacoes em lote ativam pra suprimir
// notificacoes individuais do WhatsApp por item processado (ex: importacao
// de extrato/backup com dezenas ou centenas de lancamentos de uma vez).
//
// IMPORTANTE pra quem mexer nisso no futuro: qualquer novo endpoint que crie/
// edite/exclua varios Bills (ou outra entidade notificavel) numa unica chamada
// deve envolver esse processamento com runSilently(...) ANTES de chamar os
// metodos de BillService -- caso contrario o grupo de WhatsApp da familia leva
// uma notificacao por item. Criacao/edicao/exclusao feitas pela tela normal do
// usuario (um item por vez) NUNCA devem passar por aqui.
public final class BulkOperationContext {

    private static final ThreadLocal<Boolean> ACTIVE = ThreadLocal.withInitial(() -> false);

    private BulkOperationContext() {
    }

    public static boolean isActive() {
        return ACTIVE.get();
    }

    // Ativa o modo silencioso, executa a acao e restaura o valor anterior do
    // flag (nao so "false") pra suportar chamadas aninhadas com seguranca.
    public static void runSilently(Runnable action) {
        boolean previous = ACTIVE.get();
        ACTIVE.set(true);
        try {
            action.run();
        } finally {
            ACTIVE.set(previous);
        }
    }
}
