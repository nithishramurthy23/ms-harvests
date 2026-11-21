package com.nithishramurthy.farm.fresh.entity;

import org.junit.jupiter.api.Test;

import static com.nithishramurthy.farm.fresh.entity.Role.ADMIN;
import static com.nithishramurthy.farm.fresh.entity.Role.MEMBER;
import static com.nithishramurthy.farm.fresh.entity.Role.OWNER;
import static com.nithishramurthy.farm.fresh.entity.Role.USER;
import static org.assertj.core.api.Assertions.assertThat;

class RoleTest {

    @Test
    void ownerCanGiveAnyRoleToAnyone() {
        for (Role current : Role.values()) {
            assertThat(OWNER.assignableTo(current)).containsExactly(USER, MEMBER, ADMIN, OWNER);
        }
    }

    @Test
    void adminOnlyMovesCustomersBetweenUserAndMember() {
        assertThat(ADMIN.canChange(USER, MEMBER)).isTrue();
        assertThat(ADMIN.canChange(MEMBER, USER)).isTrue();
        assertThat(ADMIN.canChange(USER, ADMIN)).isFalse();
        assertThat(ADMIN.canChange(MEMBER, OWNER)).isFalse();
        assertThat(ADMIN.assignableTo(ADMIN)).isEmpty();
        assertThat(ADMIN.assignableTo(OWNER)).isEmpty();
    }

    @Test
    void customersCantChangeRoles() {
        for (Role current : Role.values()) {
            assertThat(USER.assignableTo(current)).isEmpty();
            assertThat(MEMBER.assignableTo(current)).isEmpty();
        }
    }
}
