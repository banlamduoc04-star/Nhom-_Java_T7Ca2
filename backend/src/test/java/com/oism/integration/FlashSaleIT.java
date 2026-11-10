package com.oism.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.oism.application.order.OrderLine;
import com.oism.application.order.OrderService;
import com.oism.application.order.OrderView;
import com.oism.domain.exception.BusinessException;
import com.oism.domain.exception.ErrorCode;
import com.oism.domain.model.OrderChannel;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class FlashSaleIT extends AbstractIntegrationTest {

    @Autowired
    OrderService orderService;

    @Test
    void flashSale_50Threads_1Stock_OnlyOneSuccess() throws Exception {
        Session s = newTenant();
        JsonNode product = POST(s, "/api/v1/products", java.util.Map.of(
                "name", "Ao Flash Sale",
                "hasVariants", false,
                "skus", List.of(java.util.Map.of("code", "FLASH-001", "retailPrice", "100000"))
        ), 201);
        String skuId = product.get("skus").get(0).get("id").asText();
        confirmedReceipt(s, skuId, 1, "50000");

        UUID tenantId = UUID.fromString(s.tenantId());
        UUID branchId = UUID.fromString(s.branchId());
        UUID userId = UUID.fromString(s.userId());
        UUID skuUUID = UUID.fromString(skuId);

        int threads = 50;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch go = new CountDownLatch(1);
        AtomicInteger success = new AtomicInteger();
        AtomicInteger insufficient = new AtomicInteger();
        AtomicReference<OrderView> createdOrder = new AtomicReference<>();
        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < threads; i++) {
            final int index = i;
            Callable<Void> task = () -> {
                ready.countDown();
                go.await();
                try {
                    OrderView order = orderService.createAndReserve(
                            tenantId, userId, branchId,
                            OrderChannel.SHOPEE,
                            "FLASH-" + index + "-" + UUID.randomUUID(),
                            "Khách Flash Sale", "0900000000", "Địa chỉ",
                            List.of(new OrderLine(skuUUID, 1))
                    );
                    createdOrder.set(order);
                    success.incrementAndGet();
                } catch (BusinessException e) {
                    if (e.getErrorCode() == ErrorCode.IV40901) {
                        insufficient.incrementAndGet();
                    } else {
                        throw e;
                    }
                }
                return null;
            };
            futures.add(pool.submit(task));
        }

        ready.await();
        go.countDown();
        for (Future<?> f : futures) {
            f.get();
        }
        pool.shutdown();

        assertEquals(1, success.get(), "Chỉ đúng 1 luồng được mua thành công");
        assertEquals(49, insufficient.get(), "49 luồng còn lại phải bị từ chối IV40901");

        JsonNode stockReserved = GET(s, "/api/v1/stock?skuId=" + skuId, 200).get("content").get(0);
        assertEquals(1, stockReserved.get("onHand").asInt(), "Tồn kho thực tế còn 1 trước khi xác nhận");
        assertEquals(1, stockReserved.get("reserved").asInt(), "1 sản phẩm đã được giữ chỗ");
        assertEquals(0, stockReserved.get("available").asInt(), "Hàng khả dụng cuối phải = 0, không âm");

        assertNotNull(createdOrder.get());
        orderService.confirm(tenantId, userId, createdOrder.get().id());

        JsonNode stockFinal = GET(s, "/api/v1/stock?skuId=" + skuId, 200).get("content").get(0);
        assertEquals(0, stockFinal.get("onHand").asInt(), "Tồn kho cuối phải = 0 sau khi xuất hàng");
        assertEquals(0, stockFinal.get("reserved").asInt(), "Lượng giữ hàng về 0 sau khi xuất hàng");
        assertEquals(0, stockFinal.get("available").asInt(), "Hàng khả dụng cuối phải = 0");
    }
}
