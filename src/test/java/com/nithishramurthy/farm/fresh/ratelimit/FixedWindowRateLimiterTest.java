package com.nithishramurthy.farm.fresh.ratelimit;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class FixedWindowRateLimiterTest {

    /** A clock the test can move forward. */
    static class TestClock extends Clock {
        Instant now = Instant.parse("2026-11-21T10:00:00Z");

        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return now; }
    }

    private final TestClock clock = new TestClock();
    private final FixedWindowRateLimiter limiter = new FixedWindowRateLimiter(clock);
    private final Duration tenMinutes = Duration.ofMinutes(10);

    @Test
    void allowsUpToTheLimitThenSaysHowLongToWait() {
        for (int i = 0; i < 3; i++) {
            assertThat(limiter.tryAcquire("otp:1.2.3.4", 3, tenMinutes)).isZero();
        }
        clock.now = clock.now.plusSeconds(60);
        assertThat(limiter.tryAcquire("otp:1.2.3.4", 3, tenMinutes)).isEqualTo(541); // 9 minutes left, rounded up
    }

    @Test
    void aNewWindowStartsWhenTheOldOneEnds() {
        for (int i = 0; i < 4; i++) {
            limiter.tryAcquire("otp:1.2.3.4", 3, tenMinutes);
        }
        clock.now = clock.now.plus(tenMinutes);
        assertThat(limiter.tryAcquire("otp:1.2.3.4", 3, tenMinutes)).isZero();
    }

    @Test
    void eachKeyHasItsOwnCount() {
        for (int i = 0; i < 3; i++) {
            limiter.tryAcquire("otp:1.2.3.4", 3, tenMinutes);
        }
        assertThat(limiter.tryAcquire("otp:5.6.7.8", 3, tenMinutes)).isZero();
        assertThat(limiter.tryAcquire("login:1.2.3.4", 3, tenMinutes)).isZero();
    }

    @Test
    void neverLetsMoreThanTheLimitThroughAtOnce() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(16);
        AtomicInteger allowed = new AtomicInteger();
        for (int i = 0; i < 200; i++) {
            pool.submit(() -> {
                if (limiter.tryAcquire("otp:1.2.3.4", 10, tenMinutes) == 0) {
                    allowed.incrementAndGet();
                }
            });
        }
        pool.shutdown();
        assertThat(pool.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
        assertThat(allowed.get()).isEqualTo(10);
    }

    @Test
    void forgetsWindowsThatHaveEnded() {
        limiter.tryAcquire("a", 3, tenMinutes);
        clock.now = clock.now.plus(Duration.ofMinutes(11));
        limiter.tryAcquire("b", 3, tenMinutes);

        limiter.forgetOlderThan(tenMinutes);
        assertThat(limiter.size()).isEqualTo(1);
    }

    @Test
    void knowsWhichEndpointsToLimit() {
        assertThat(AuthRateLimitFilter.bucket("/api/auth/send-otp")).isEqualTo("send");
        assertThat(AuthRateLimitFilter.bucket("/api/auth/login")).isEqualTo("check");
        assertThat(AuthRateLimitFilter.bucket("/api/products")).isNull();
    }
}
