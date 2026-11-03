
package com.oism.realtime;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
public class OrderEventService {

    private final ConcurrentMap<String, SseEmitter> clients =
            new ConcurrentHashMap<>();

    public SseEmitter subscribe() {
        String clientId = UUID.randomUUID().toString();
        SseEmitter emitter = new SseEmitter(0L);

        clients.put(clientId, emitter);

        emitter.onCompletion(() -> clients.remove(clientId));
        emitter.onTimeout(() -> {
            clients.remove(clientId);
            emitter.complete();
        });
        emitter.onError(error -> clients.remove(clientId));

        try {
            emitter.send(
                    SseEmitter.event()
                            .name("connected")
                            .data("SSE connection established")
            );
        } catch (IOException | IllegalStateException ex) {
            clients.remove(clientId, emitter);
            emitter.completeWithError(ex);
        }

        return emitter;
    }

    public void publishOrderCreated(String orderId) {
        OrderEvent event = OrderEvent.created(orderId);

        clients.forEach((clientId, emitter) -> {
            try {
                emitter.send(
                        SseEmitter.event()
                                .name("order-created")
                                .data(event)
                );
            } catch (IOException | IllegalStateException ex) {
                clients.remove(clientId, emitter);
                emitter.completeWithError(ex);
            }
        });
    }
}
