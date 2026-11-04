package com.oism.realtime;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/v1/events")
public class OrderEventController {

    private final OrderEventService service;

    public OrderEventController(OrderEventService service) {
        this.service = service;
    }

    @GetMapping(
            value = "/orders",
            produces = MediaType.TEXT_EVENT_STREAM_VALUE
    )
    public SseEmitter subscribe() {
        return service.subscribe();
    }
}