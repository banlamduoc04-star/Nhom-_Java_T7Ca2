
package com.oism.realtime;

import java.util.Map;

import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile("local")
@RequestMapping("/api/v1/events/test")
public class LocalOrderEventTestController {

    private final OrderEventService service;

    public LocalOrderEventTestController(OrderEventService service) {
        this.service = service;
    }

    @GetMapping("/orders/{orderId}")
    public Map<String, String> publishOrderCreated(
            @PathVariable String orderId) {

        service.publishOrderCreated(orderId);

        return Map.of(
                "message", "Order event published",
                "orderId", orderId
        );
    }
}
