package com.oism.infrastructure.websocket;

import com.oism.domain.event.OrderCreatedEvent;
import com.oism.domain.event.StockAlertEvent;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Lang nghe su kien va phat qua WebSocket STOMP sau khi transaction commit (JC-28).
 */
@Component
public class RealtimeEventListener {

    private final SimpMessagingTemplate messagingTemplate;

    public RealtimeEventListener(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onOrderCreated(OrderCreatedEvent event) {
        String topic = "/topic/tenants/" + event.tenantId() + "/orders/new";
        messagingTemplate.convertAndSend(topic, event);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onStockAlert(StockAlertEvent event) {
        String topic = "/topic/tenants/" + event.tenantId() + "/alerts";
        messagingTemplate.convertAndSend(topic, event);
    }
}
