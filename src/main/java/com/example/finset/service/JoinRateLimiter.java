package com.example.finset.service;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Simple in-memory sliding-window rate limiter for the join endpoint.
 *
 * Allows MAX_ATTEMPTS per IP within WINDOW_SECONDS.
 * A background cleanup runs every ~5 minutes to prevent unbounded memory growth.
 *
 * No external dependency needed — works with plain Spring Boot.
 */
@Component
public class JoinRateLimiter {

    private static final int MAX_ATTEMPTS    = 10;
    private static final int WINDOW_SECONDS  = 3600; // 1 hour

    private record Bucket(AtomicInteger count, Instant windowStart) {}

    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    /**
     * Returns true if the request should be allowed, false if rate-limited.
     */
    public boolean tryConsume(String ipAddress) {
        Instant now = Instant.now();
        buckets.entrySet().removeIf(e ->
            now.getEpochSecond() - e.getValue().windowStart().getEpochSecond() > WINDOW_SECONDS * 2
        );

        Bucket bucket = buckets.compute(ipAddress, (ip, existing) -> {
            if (existing == null ||
                now.getEpochSecond() - existing.windowStart().getEpochSecond() > WINDOW_SECONDS) {
                // New window
                return new Bucket(new AtomicInteger(1), now);
            }
            existing.count().incrementAndGet();
            return existing;
        });

        return bucket.count().get() <= MAX_ATTEMPTS;
    }

    /**
     * How many seconds until the window resets for this IP.
     * Used to populate the Retry-After response header.
     */
    public long secondsUntilReset(String ipAddress) {
        Bucket b = buckets.get(ipAddress);
        if (b == null) return 0;
        long elapsed = Instant.now().getEpochSecond() - b.windowStart().getEpochSecond();
        return Math.max(0, WINDOW_SECONDS - elapsed);
    }
}