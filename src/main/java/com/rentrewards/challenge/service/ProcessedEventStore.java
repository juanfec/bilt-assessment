package com.rentrewards.challenge.service;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks which webhook events have already been processed, so that the
 * RewardsEngine can ignore duplicate deliveries from the payment processor.
 *
 * Every processed eventId is remembered (not just the latest), so a resend of
 * an older event is still detected, and claiming an id is atomic so concurrent
 * deliveries of the same event cannot both win.
 */
public class ProcessedEventStore {

    private final Set<String> processedEventIds = ConcurrentHashMap.newKeySet();

    /**
     * @return true if this eventId has already been processed before.
     */
    public boolean isDuplicate(String eventId) {
        return processedEventIds.contains(eventId);
    }

    public void markProcessed(String eventId) {
        processedEventIds.add(eventId);
    }

    /**
     * Atomically claims an eventId.
     *
     * @return true if the caller is the first to claim it, false if it was already claimed.
     */
    public boolean tryMarkProcessed(String eventId) {
        return processedEventIds.add(eventId);
    }

    /** Releases a claim, e.g. when processing failed and the event should be retryable. */
    public void unmark(String eventId) {
        processedEventIds.remove(eventId);
    }
}
