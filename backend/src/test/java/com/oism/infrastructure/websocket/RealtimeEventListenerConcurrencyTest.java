package com.oism.infrastructure.websocket;

import com.oism.domain.event.OrderCreatedEvent;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.eq;

class RealtimeEventListenerConcurrencyTest {

    @Test
    void publishesAllOrderEventsFromFiftyConcurrentThreads() throws InterruptedException {
        int threadCount = 50;
        UUID tenantId = UUID.randomUUID();
        SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
        RealtimeEventListener listener = new RealtimeEventListener(messagingTemplate);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch finished = new CountDownLatch(threadCount);

        try {
            for (int i = 0; i < threadCount; i++) {
                final int sequence = i;
                executor.submit(() -> {
                    ready.countDown();
                    try {
                        if (!start.await(10, TimeUnit.SECONDS)) {
                            return;
                        }
                        listener.onOrderCreated(new OrderCreatedEvent(
                                tenantId,
                                UUID.randomUUID(),
                                "POS-CONCURRENT-" + sequence,
                                BigDecimal.valueOf(100_000L + sequence),
                                "POS",
                                "QA cashier",
                                Instant.now()));
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        finished.countDown();
                    }
                });
            }

            if (!ready.await(10, TimeUnit.SECONDS)) {
                throw new AssertionError("Not all 50 worker threads became ready");
            }
            start.countDown();
            if (!finished.await(20, TimeUnit.SECONDS)) {
                throw new AssertionError("50 concurrent event publications did not finish in time");
            }

            verify(messagingTemplate, times(threadCount))
                    .convertAndSend(
                eq("/topic/tenants/" + tenantId + "/orders/new"),
                any(Object.class));
        } finally {
            start.countDown();
            executor.shutdownNow();
            executor.awaitTermination(5, TimeUnit.SECONDS);
        }
    }
}
