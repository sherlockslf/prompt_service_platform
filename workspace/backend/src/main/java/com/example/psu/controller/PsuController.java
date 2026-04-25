package com.example.psu.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.psu.dto.PsuCreateRequest;
import com.example.psu.dto.response.PsuResponse;
import com.example.psu.entity.PsuUnit;
import com.example.psu.service.PsuService;

import jakarta.validation.Valid;

/**
 * PSU管理控制器
 */
@RestController
@RequestMapping("/api/psus")
public class PsuController {

    private static final Long DEFAULT_OPERATOR_ID = 0L;
    
    @Autowired
    private PsuService psuService;
    
    /**
     * 开发侧PSU管理页面创建新PSU
     * 参数：psuId-全局唯一PSU标识，name-PSU名称，description-描述
     */
    @PostMapping
    public ResponseEntity<PsuResponse> createPsu(@Valid @RequestBody PsuCreateRequest request) {
        PsuUnit psu = psuService.createPsu(request, DEFAULT_OPERATOR_ID);
        PsuResponse response = psuService.convertToResponse(psu);
        return ResponseEntity.ok(response);
    }
    
    /**
     * 开发侧/业务侧PSU管理页面分页查询PSU列表
     * 参数：page-页码(默认1)，size-每页大小(默认10)
     */
    @GetMapping
    public ResponseEntity<Page<PsuResponse>> getPsus(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<PsuUnit> psus = psuService.getPsus(page, size);
        Page<PsuResponse> responsePage = psus.map(psuService::convertToResponse);
        return ResponseEntity.ok(responsePage);
    }
    
    /**
     * 开发侧PSU管理页面编辑更新PSU信息
     * 参数：id-PSU数据库ID，psuId/name/description-更新字段
     */
    @PutMapping("/{id}")
    public ResponseEntity<PsuResponse> updatePsu(@PathVariable Long id, @Valid @RequestBody PsuCreateRequest request) {
        PsuUnit psu = psuService.updatePsu(id, request);
        PsuResponse response = psuService.convertToResponse(psu);
        return ResponseEntity.ok(response);
    }
    
    /**
     * 各页面根据数据库ID获取单个PSU详情
     * 参数：id-PSU数据库ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<PsuResponse> getPsuById(@PathVariable Long id) {
        PsuUnit psu = psuService.getPsuById(id);
        PsuResponse response = psuService.convertToResponse(psu);
        return ResponseEntity.ok(response);
    }
    
    /**
     * 各页面根据全局PSU ID获取PSU详情
     * 参数：psuId-PSU全局唯一标识
     */
    @GetMapping("/by-psu-id/{psuId}")
    public ResponseEntity<PsuResponse> getPsuByPsuId(@PathVariable String psuId) {
        PsuUnit psu = psuService.getPsuByPsuId(psuId);
        PsuResponse response = psuService.convertToResponse(psu);
        return ResponseEntity.ok(response);
    }
    
    /**
     * 开发侧PSU管理页面归档删除PSU
     * 参数：id-PSU数据库ID
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePsu(@PathVariable Long id) {
        psuService.deletePsu(id);
        return ResponseEntity.ok().build();
    }
}

