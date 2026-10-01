package com.lmf.finpro.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.lmf.finpro.infrastructure.config.RateLimitProperties.Rule;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class RateLimiterTest {

    private static final Rule RULE = new Rule(2, Duration.ofMinutes(1));

    /** Relógio controlável para avançar o tempo sem dormir. */
    private static class MutableClock extends Clock {
        private Instant now = Instant.parse("2026-01-01T00:00:00Z");

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }

    @Test
    void allowsUpToTheLimitThenReturnsSecondsUntilTheWindowReopens() {
        MutableClock clock = new MutableClock();
        RateLimiter limiter = new RateLimiter(clock);

        assertThat(limiter.tryAcquire("k", RULE)).isZero();
        assertThat(limiter.tryAcquire("k", RULE)).isZero();

        clock.advance(Duration.ofSeconds(20));
        assertThat(limiter.tryAcquire("k", RULE)).isEqualTo(40);
    }

    @Test
    void windowResetsAfterItExpires() {
        MutableClock clock = new MutableClock();
        RateLimiter limiter = new RateLimiter(clock);
        limiter.tryAcquire("k", RULE);
        limiter.tryAcquire("k", RULE);
        assertThat(limiter.tryAcquire("k", RULE)).isPositive();

        clock.advance(Duration.ofMinutes(1));

        assertThat(limiter.tryAcquire("k", RULE)).isZero();
    }

    @Test
    void keysAreIndependent() {
        RateLimiter limiter = new RateLimiter(new MutableClock());
        limiter.tryAcquire("a", RULE);
        limiter.tryAcquire("a", RULE);

        assertThat(limiter.tryAcquire("a", RULE)).isPositive();
        assertThat(limiter.tryAcquire("b", RULE)).isZero();
    }
}
