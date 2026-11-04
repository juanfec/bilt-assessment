package com.rentrewards.challenge.service;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProcessedEventStoreTest {

    @Test
    void remembersEveryProcessedIdNotJustTheLastOne() {
        ProcessedEventStore store = new ProcessedEventStore();

        store.markProcessed("evt-A");
        store.markProcessed("evt-B");
        store.markProcessed("evt-C");

        assertTrue(store.isDuplicate("evt-A"));
        assertTrue(store.isDuplicate("evt-B"));
        assertTrue(store.isDuplicate("evt-C"));
        assertFalse(store.isDuplicate("evt-D"));
    }

    @Test
    void tryMarkProcessedOnlySucceedsTheFirstTime() {
        ProcessedEventStore store = new ProcessedEventStore();

        assertTrue(store.tryMarkProcessed("evt-1"));
        assertFalse(store.tryMarkProcessed("evt-1"));
        assertTrue(store.isDuplicate("evt-1"));
    }

    @Test
    void unmarkMakesTheIdClaimableAgain() {
        ProcessedEventStore store = new ProcessedEventStore();
        store.tryMarkProcessed("evt-1");

        store.unmark("evt-1");

        assertFalse(store.isDuplicate("evt-1"));
        assertTrue(store.tryMarkProcessed("evt-1"));
    }

    @Test
    void unmarkingOneIdDoesNotAffectOthers() {
        ProcessedEventStore store = new ProcessedEventStore();
        store.tryMarkProcessed("evt-1");
        store.tryMarkProcessed("evt-2");

        store.unmark("evt-1");

        assertTrue(store.isDuplicate("evt-2"));
    }

    @Test
    void onlyOneThreadWinsTheClaim() throws Exception {
        ProcessedEventStore store = new ProcessedEventStore();
        int workers = 16;
        ExecutorService executor = Executors.newFixedThreadPool(workers);
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<Boolean>> futures = new ArrayList<>();
            for (int i = 0; i < workers; i++) {
                futures.add(executor.submit(() -> {
                    start.await();
                    return store.tryMarkProcessed("evt-race");
                }));
            }
            start.countDown();

            int winners = 0;
            for (Future<Boolean> future : futures) {
                if (future.get(2, TimeUnit.SECONDS)) {
                    winners++;
                }
            }
            assertEquals(1, winners);
        } finally {
            executor.shutdownNow();
        }
    }
}
