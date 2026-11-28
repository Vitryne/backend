package com.vitryne.api.order.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Cancela os pedidos que a loja não aceitou no prazo (ETI-412).
 *
 * Cada pedido é cancelado na sua própria transação; a busca usa
 * FOR UPDATE SKIP LOCKED, então várias instâncias da API podem rodar a
 * rotina ao mesmo tempo sem cancelar o mesmo pedido duas vezes.
 */
@Component
@RequiredArgsConstructor
public class OrderAcceptanceExpirationJob {

    private static final Logger log = LoggerFactory.getLogger(OrderAcceptanceExpirationJob.class);

    private final OrderService orderService;

    @Value("${vitryne.orders.acceptance-expiration.max-per-run:500}")
    private int maxPerRun;

    @Scheduled(
            fixedDelayString = "${vitryne.orders.acceptance-expiration.interval:PT1M}",
            initialDelayString = "${vitryne.orders.acceptance-expiration.interval:PT1M}"
    )
    public void run() {
        int expired = expireOverdueOrders();
        if (expired > 0) {
            log.info("Pedidos cancelados por falta de aceite no prazo: {}", expired);
        }
    }

    /** Cancela os pedidos vencidos e devolve quantos foram cancelados nesta execução. */
    public int expireOverdueOrders() {
        int expired = 0;
        while (expired < maxPerRun) {
            try {
                if (orderService.expireNextOverdueAcceptance().isEmpty()) {
                    break;
                }
                expired++;
            } catch (RuntimeException e) {
                // Não insiste no mesmo pedido nesta execução; a próxima tenta de novo.
                log.error("Falha ao cancelar pedido vencido sem aceite", e);
                break;
            }
        }
        return expired;
    }
}
