package com.nithishramurthy.farm.fresh.entity;

import org.junit.jupiter.api.Test;

import static com.nithishramurthy.farm.fresh.entity.OrderStatus.*;
import static org.assertj.core.api.Assertions.assertThat;

class OrderStatusTest {

    @Test
    void ordersMoveForwardOneStepAtATime() {
        assertThat(PENDING.canMoveTo(PROCESSING)).isTrue();
        assertThat(PROCESSING.canMoveTo(SHIPPED)).isTrue();
        assertThat(SHIPPED.canMoveTo(DELIVERED)).isTrue();

        assertThat(PENDING.canMoveTo(SHIPPED)).isFalse();
        assertThat(SHIPPED.canMoveTo(PROCESSING)).isFalse();
    }

    @Test
    void cancellingIsOnlyPossibleBeforeShipping() {
        assertThat(PENDING.canMoveTo(CANCELLED)).isTrue();
        assertThat(PROCESSING.canMoveTo(CANCELLED)).isTrue();
        assertThat(SHIPPED.canMoveTo(CANCELLED)).isFalse();
    }

    @Test
    void deliveredAndCancelledAreFinal() {
        for (OrderStatus next : values()) {
            assertThat(DELIVERED.canMoveTo(next)).isFalse();
            assertThat(CANCELLED.canMoveTo(next)).isFalse();
        }
    }
}
