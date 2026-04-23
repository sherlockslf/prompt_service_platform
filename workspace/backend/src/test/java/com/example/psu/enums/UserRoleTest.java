package com.example.psu.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * UserRole枚举测试
 */
class UserRoleTest {

    @Test
    void testFromCode_Admin() {
        UserRole role = UserRole.fromCode("ADMIN");
        assertEquals(UserRole.ADMIN, role);
        assertEquals("ADMIN", role.getCode());
        assertEquals("超级管理员", role.getDescription());
    }

    @Test
    void testFromCode_Developer() {
        UserRole role = UserRole.fromCode("DEVELOPER");
        assertEquals(UserRole.DEVELOPER, role);
        assertEquals("DEVELOPER", role.getCode());
        assertEquals("研发人员", role.getDescription());
    }

    @Test
    void testFromCode_Business() {
        UserRole role = UserRole.fromCode("BUSINESS");
        assertEquals(UserRole.BUSINESS, role);
        assertEquals("BUSINESS", role.getCode());
        assertEquals("产品/运营人员", role.getDescription());
    }

    @Test
    void testFromCode_InvalidCode() {
        assertThrows(IllegalArgumentException.class, () -> {
            UserRole.fromCode("INVALID");
        });
    }
}
