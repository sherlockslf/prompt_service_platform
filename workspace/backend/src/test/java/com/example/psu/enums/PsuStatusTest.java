package com.example.psu.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PsuStatus枚举测试
 */
class PsuStatusTest {

    @Test
    void testFromCode_Draft() {
        PsuStatus status = PsuStatus.fromCode("DRAFT");
        assertEquals(PsuStatus.DRAFT, status);
        assertEquals("DRAFT", status.getCode());
        assertEquals("草稿", status.getDescription());
    }

    @Test
    void testFromCode_Candidate() {
        PsuStatus status = PsuStatus.fromCode("CANDIDATE");
        assertEquals(PsuStatus.CANDIDATE, status);
        assertEquals("CANDIDATE", status.getCode());
        assertEquals("发布候选", status.getDescription());
    }

    @Test
    void testFromCode_Formal() {
        PsuStatus status = PsuStatus.fromCode("FORMAL");
        assertEquals(PsuStatus.FORMAL, status);
        assertEquals("FORMAL", status.getCode());
        assertEquals("正式版本", status.getDescription());
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
