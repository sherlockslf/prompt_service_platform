package com.example.psu.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PsuStatus枚举测试
 */
class PsuStatusTest {

    @Test
    void testFromCode_Active() {
        PsuStatus status = PsuStatus.fromCode("ACTIVE");
        assertEquals(PsuStatus.ACTIVE, status);
        assertEquals("ACTIVE", status.getCode());
        assertEquals("活跃", status.getDescription());
    }

    @Test
    void testFromCode_Archived() {
        PsuStatus status = PsuStatus.fromCode("ARCHIVED");
        assertEquals(PsuStatus.ARCHIVED, status);
        assertEquals("ARCHIVED", status.getCode());
        assertEquals("已归档", status.getDescription());
    }

    @Test
    void testFromCode_InvalidCode() {
        assertThrows(IllegalArgumentException.class, () -> {
            PsuStatus.fromCode("INVALID");
        });
    }
}
