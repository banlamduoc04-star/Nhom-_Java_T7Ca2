
package com.oism.realtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

class OrderEventServiceConcurrencyTest {

    private static final int THREAD_COUNT = 50;

    @Test
    void shouldHandle50ConcurrentSubscribersAndPublishers() throws Exception {
        List<RecordingSseEmitter> emitters = new CopyOnWriteArrayList<>();

        OrderEventService service = new OrderEventService(() -> {
            RecordingSseEmitter emitter = new RecordingSseEmitter();
            emitters.add(emitter);
            return emitter;
        });

        ExecutorService executor =
                Executors.newFixedThreadPool(THREAD_COUNT);

        try {
            CountDownLatch subscribeReady =
                    new CountDownLatch(THREAD_COUNT);
            CountDownLatch subscribeStart =
                    new CountDownLatch(1);

            List<Future<SseEmitter>> subscribeResults =
                    new ArrayList<>();

            for (int i = 0; i < THREAD_COUNT; i++) {
                subscribeResults.add(executor.submit(() -> {
                    subscribeReady.countDown();

                    if (!subscribeStart.await(10, TimeUnit.SECONDS)) {
                        throw new IllegalStateException(
                                "Subscribe start timeout"
                        );
                    }

                    return service.subscribe();
                }));
            }

            assertTrue(
                    subscribeReady.await(10, TimeUnit.SECONDS),
                    "50 threads could not get ready"
            );

            subscribeStart.countDown();

            for (Future<SseEmitter> result : subscribeResults) {
                result.get(10, TimeUnit.SECONDS);
            }

            assertEquals(THREAD_COUNT, emitters.size());

            CountDownLatch publishReady =
                    new CountDownLatch(THREAD_COUNT);
            CountDownLatch publishStart =
                    new CountDownLatch(1);

            List<Future<?>> publishResults = new ArrayList<>();

            for (int i = 0; i < THREAD_COUNT; i++) {
                final int orderNumber = i;

                publishResults.add(executor.submit(() -> {
                    publishReady.countDown();

                    if (!publishStart.await(10, TimeUnit.SECONDS)) {
                        throw new IllegalStateException(
                                "Publish start timeout"
                        );
                    }

                    service.publishOrderCreated(
                            "ORDER-" + orderNumber
                    );

                    return null;
                }));
            }

            assertTrue(
                    publishReady.await(10, TimeUnit.SECONDS),
                    "50 publisher threads could not get ready"
            );

            publishStart.countDown();

            for (Future<?> result : publishResults) {
                result.get(10, TimeUnit.SECONDS);
            }

            for (RecordingSseEmitter emitter : emitters) {
                assertEquals(
                        1 + THREAD_COUNT,
                        emitter.getSendCount(),
                        "An emitter did not receive all events"
                );
            }

            assertEquals(THREAD_COUNT, emitters.size());

        } finally {
            executor.shutdownNow();
        }
    }

    private static class RecordingSseEmitter extends SseEmitter {

        private final AtomicInteger sendCount = new AtomicInteger();

        RecordingSseEmitter() {
            super(0L);
        }

        @Override
        public void send(SseEventBuilder builder) throws IOException {
            sendCount.incrementAndGet();
        }

        int getSendCount() {
            return sendCount.get();
        }
    }
}
