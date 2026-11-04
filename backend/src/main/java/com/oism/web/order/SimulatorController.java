package com.oism.web.order;

import com.oism.application.auth.AuthenticatedUser;
import com.oism.application.order.OrderLine;
import com.oism.application.order.OrderService;
import com.oism.application.order.OrderView;
import com.oism.domain.exception.BusinessException;
import com.oism.domain.model.OrderChannel;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Bo gia lap don hang dong thoi (FR-SIM-01, NFR-PERF-02).
 * Cho phep gui N don dong thoi de kiem chung co che chong ban vuot (Flash Sale).
 */
@RestController
@RequestMapping("/api/v1/simulator")
@PreAuthorize("hasAnyRole('OWNER','STAFF')")
@Tag(name = "Simulator")
public class SimulatorController {

    private final OrderService orderService;

    public SimulatorController(OrderService orderService) {
        this.orderService = orderService;
    }

    public record SimulationRequest(
            @NotNull(message = "Chi nhánh không được để trống") UUID branchId,
            @NotNull(message = "Kênh không được để trống") OrderChannel channel,
            @NotNull(message = "skuId không được để trống") UUID skuId,
            @Min(value = 1, message = "Số lượng mỗi đơn tối thiểu là 1") int quantity,
            @Min(value = 1, message = "Số luồng tối thiểu là 1")
            @Max(value = 100, message = "Số luồng tối đa là 100") int orderCount) {
    }

    public record SimulationResult(
            int total,
            int successful,
            int failed,
            Map<String, Integer> errorCounts,
            List<String> successfulOrderCodes,
            long durationMs) {
    }

    @PostMapping("/orders")
    @Operation(summary = "Mô phỏng gửi N đơn đồng thời (Flash Sale) vào 1 SKU để kiểm tra chống bán vượt")
    public SimulationResult simulateOrders(@AuthenticationPrincipal AuthenticatedUser user,
                                           @Valid @RequestBody SimulationRequest req) throws InterruptedException {
        int count = req.orderCount();
        ExecutorService executor = Executors.newFixedThreadPool(Math.min(count, 50));
        CountDownLatch startSignal = new CountDownLatch(1);
        CountDownLatch doneSignal = new CountDownLatch(count);

        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger failCount = new AtomicInteger();
        List<String> successCodes = Collections.synchronizedList(new ArrayList<>());
        Map<String, AtomicInteger> errors = new HashMap<>();

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < count; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    startSignal.await();
                    String externalId = "SIM-" + System.currentTimeMillis() + "-" + index + "-" + UUID.randomUUID().toString().substring(0, 8);
                    OrderView view = orderService.createAndReserve(
                            user.tenantId(),
                            user.userId(),
                            req.branchId(),
                            req.channel(),
                            externalId,
                            "Khách mô phỏng #" + index,
                            "0988776655",
                            "Địa chỉ mô phỏng",
                            List.of(new OrderLine(req.skuId(), req.quantity()))
                    );
                    successCount.incrementAndGet();
                    successCodes.add(view.code());
                } catch (BusinessException be) {
                    failCount.incrementAndGet();
                    String code = be.getErrorCode().name();
                    errors.computeIfAbsent(code, k -> new AtomicInteger()).incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                    errors.computeIfAbsent("SY50001", k -> new AtomicInteger()).incrementAndGet();
                } finally {
                    doneSignal.countDown();
                }
            });
        }

        startSignal.countDown();
        doneSignal.await();
        executor.shutdown();

        long duration = System.currentTimeMillis() - startTime;

        Map<String, Integer> finalErrors = new HashMap<>();
        errors.forEach((k, v) -> finalErrors.put(k, v.get()));

        return new SimulationResult(count, successCount.get(), failCount.get(), finalErrors, successCodes, duration);
    }
}
