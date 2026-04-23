package com.example.psu.service;

import com.example.psu.dto.PsuCreateRequest;
import com.example.psu.dto.response.PsuResponse;
import com.example.psu.entity.PsuUnit;
import com.example.psu.entity.User;
import com.example.psu.enums.PsuStatus;
import com.example.psu.enums.UserRole;
import com.example.psu.exception.BusinessException;
import com.example.psu.exception.ErrorCode;
import com.example.psu.repository.PsuRepository;
import com.example.psu.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * PsuService单元测试
 */
@ExtendWith(MockitoExtension.class)
class PsuServiceTest {

    @Mock
    private PsuRepository psuRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private PsuService psuService;

    private PsuUnit testPsu;
    private User testUser;

    @BeforeEach
    void setUp() {
        // 创建测试用户
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("dev_user");
        testUser.setPassword("Dev@123");
        testUser.setRole(UserRole.DEVELOPER);
        testUser.setEnabled(true);

        // 创建测试PSU
        testPsu = new PsuUnit();
        testPsu.setId(1L);
        testPsu.setPsuId("test_psu_001");
        testPsu.setName("测试PSU");
        testPsu.setDescription("这是一个测试PSU");
        testPsu.setStatus(PsuStatus.ACTIVE);
        testPsu.setCreatorId(1L);
        testPsu.setMajorVersion(0);
        testPsu.setMinorVersion(0);
        testPsu.setPatchVersion(0);
        testPsu.setCreatedAt(LocalDateTime.now());
        testPsu.setUpdatedAt(LocalDateTime.now());
    }

    @Test
    void testCreatePsu_Success() {
        // 准备测试数据
        PsuCreateRequest request = new PsuCreateRequest();
        request.setPsuId("new_psu_001");
        request.setName("新PSU");
        request.setDescription("新PSU描述");

        // 配置mock
        when(psuRepository.existsByPsuId("new_psu_001")).thenReturn(false);
        when(psuRepository.save(any(PsuUnit.class))).thenReturn(testPsu);

        // 执行测试
        PsuUnit result = psuService.createPsu(request, 1L);

        // 验证结果
        assertNotNull(result);
        assertEquals("test_psu_001", result.getPsuId());
        assertEquals(PsuStatus.ACTIVE, result.getStatus());
        assertEquals(1L, result.getCreatorId());

        // 验证mock调用
        verify(psuRepository, times(1)).existsByPsuId("new_psu_001");
        verify(psuRepository, times(1)).save(any(PsuUnit.class));
    }

    @Test
    void testCreatePsu_DuplicatePsuId() {
        // 准备测试数据
        PsuCreateRequest request = new PsuCreateRequest();
        request.setPsuId("test_psu_001");
        request.setName("重复PSU");

        // 配置mock
        when(psuRepository.existsByPsuId("test_psu_001")).thenReturn(true);

        // 执行测试并验证异常
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            psuService.createPsu(request, 1L);
        });

        assertEquals(ErrorCode.PSU_ALREADY_EXISTS.getCode(), exception.getCode());
        assertTrue(exception.getMessage().contains("PSU ID已存在"));

        // 验证mock调用
        verify(psuRepository, times(1)).existsByPsuId("test_psu_001");
        verify(psuRepository, never()).save(any(PsuUnit.class));
    }

    @Test
    void testGetPsus_Success() {
        // 准备测试数据
        List<PsuUnit> psuList = Arrays.asList(testPsu);
        Page<PsuUnit> psuPage = new PageImpl<>(psuList);

        // 配置mock
        when(psuRepository.findAll(any(Pageable.class))).thenReturn(psuPage);

        // 执行测试
        Page<PsuUnit> result = psuService.getPsus(1, 10);

        // 验证结果
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("test_psu_001", result.getContent().get(0).getPsuId());

        // 验证mock调用
        verify(psuRepository, times(1)).findAll(any(Pageable.class));
    }

    @Test
    void testGetPsuById_Success() {
        // 配置mock
        when(psuRepository.findById(1L)).thenReturn(Optional.of(testPsu));

        // 执行测试
        PsuUnit result = psuService.getPsuById(1L);

        // 验证结果
        assertNotNull(result);
        assertEquals("test_psu_001", result.getPsuId());

        // 验证mock调用
        verify(psuRepository, times(1)).findById(1L);
    }

    @Test
    void testGetPsuById_NotFound() {
        // 配置mock
        when(psuRepository.findById(999L)).thenReturn(Optional.empty());

        // 执行测试并验证异常
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            psuService.getPsuById(999L);
        });

        assertEquals(ErrorCode.PSU_NOT_FOUND.getCode(), exception.getCode());

        // 验证mock调用
        verify(psuRepository, times(1)).findById(999L);
    }

    @Test
    void testDeletePsu_Success() {
        // 配置mock
        when(psuRepository.findById(1L)).thenReturn(Optional.of(testPsu));
        when(psuRepository.save(any(PsuUnit.class))).thenReturn(testPsu);

        // 执行测试
        psuService.deletePsu(1L);

        // 验证结果
        assertEquals(PsuStatus.ARCHIVED, testPsu.getStatus());

        // 验证mock调用
        verify(psuRepository, times(1)).findById(1L);
        verify(psuRepository, times(1)).save(testPsu);
    }

    @Test
    void testConvertToResponse_Success() {
        // 配置mock
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // 执行测试
        PsuResponse response = psuService.convertToResponse(testPsu);

        // 验证结果
        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("test_psu_001", response.getPsuId());
        assertEquals("测试PSU", response.getName());
        assertEquals("ACTIVE", response.getStatus());
        assertEquals("0.0.0", response.getFullVersion());
        assertEquals("dev_user", response.getCreatorName());

        // 验证mock调用
        verify(userRepository, times(1)).findById(1L);
    }
}
