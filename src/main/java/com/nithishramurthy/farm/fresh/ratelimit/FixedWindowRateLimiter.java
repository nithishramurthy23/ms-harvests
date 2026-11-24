package com.nithishramurthy.farm.fresh.ratelimit;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Counts requests per key (for example "otp:203.0.113.7") in fixed windows of
 * time. The first request starts a window; once the limit is reached, further
 * requests are refused until that window ends.
 *
 * Counts live in this backend copy's memory, so with two copies behind nginx a
 * client could get up to twice the limit. That's fine for slowing down abuse;
 * the per-email limit in OtpService is in the database and holds exactly.
 * Redis would make this one exact too (see design decision 15).
 */
public class FixedWindowRateLimiter {

    /** When this key's current window started, and how many requests it has had in it. Immutable, so compute() can swap it safely. */
    private record Window(Instant start, int count) {
    }

    private final ConcurrentMap<String, Window> windows = new ConcurrentHashMap<>();
    private final Clock clock;

    public FixedWindowRateLimiter(Clock clock) {
        this.clock = clock;
    }

    /**
     * @return 0 if the request is allowed, otherwise how many seconds until it would be
     */
    public long tryAcquire(String key, int limit, Duration window) {
        Instant now = clock.instant();
        // compute() runs atomically per key, so two requests at once can't both take the last slot
        Window updated = windows.compute(key, (k, current) -> {
            if (current == null || !now.isBefore(current.start().plus(window))) {
                return new Window(now, 1);
            }
            return new Window(current.start(), current.count() + 1);
        });
        if (updated.count() <= limit) {
            return 0;
        }
        return Math.max(1, Duration.between(now, updated.start().plus(window)).toSeconds() + 1);
    }

    /** Forgets windows that have ended, so the map doesn't grow forever. */
    public void forgetOlderThan(Duration window) {
        Instant cutoff = clock.instant().minus(window);
        windows.values().removeIf(w -> w.start().isBefore(cutoff));
    }

    int size() {
        return windows.size();
    }
}
