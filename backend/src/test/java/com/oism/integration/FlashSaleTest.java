package com.oism.integration;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.databind.JsonNode;
import com.oism.application.order.OrderLine;
import com.oism.application.order.OrderService;
import com.oism.domain.exception.BusinessException;
import com.oism.domain.exception.ErrorCode;
import com.oism.domain.model.OrderChannel;

class FlashSaleTest extends AbstractIntegrationTest {

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

        UUID tenantId = s.getTenantId();
        UUID branchId = s.getBranchId();
        // Sửa lỗi: Dùng UUID ngẫu nhiên thay vì s.getUserId() vì class Session không có hàm này
        UUID userId = UUID.randomUUID(); 
        UUID skuUUID = UUID.fromString(skuId);

        int threads = 50;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch go = new CountDownLatch(1);
        AtomicInteger success = new AtomicInteger();
        AtomicInteger insufficient = new AtomicInteger();
        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < threads; i++) {
            final int index = i;
            Callable<Void> task = () -> {
                ready.countDown();
                go.await();
                try {
                    orderService.createAndReserve(
                            tenantId, userId, branchId,
                            OrderChannel.SHOPEE,
                            "FLASH-" + index + "-" + UUID.randomUUID(),
                            "Khách Flash Sale", "0900000000", "Địa chỉ",
                            List.of(new OrderLine(skuUUID, 1))
                    );
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

        JsonNode stock = GET(s, "/api/v1/stock?skuId=" + skuId, 200).get("content").get(0);
        assertEquals(0, stock.get("onHand").asInt(), "Tồn kho cuối phải = 0");
        assertEquals(0, stock.get("available").asInt(), "Hàng khả dụng cuối phải = 0, không âm");
    }
}
