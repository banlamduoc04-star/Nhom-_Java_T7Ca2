package com.oism.integration;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/** NFR-USA-01: healthcheck Docker dung /actuator/health (cong khai, khong lo chi tiet); endpoint actuator khac dong. */
class HealthIT extends AbstractIntegrationTest {

    @Test
    void healthIsPublicAndHidesDetails() {
        JsonNode health = body(call(get("/actuator/health"), null, null, 200));
        assertEquals("UP", health.get("status").asText());
        assertFalse(health.has("components"), "Khong lo chi tiet DB/disk ra ngoai");
    }

    @Test
    void otherActuatorEndpointsAreNotExposed() {
        call(get("/actuator/env"), null, null, 401);
        call(get("/actuator/env"), newTenant().token(), null, 404);
    }
}
