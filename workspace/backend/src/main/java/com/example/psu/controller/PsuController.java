package com.example.psu.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.psu.dto.PsuCreateRequest;
import com.example.psu.dto.response.PromptSchemaResolveResponse;
import com.example.psu.dto.response.PsuResponse;
import com.example.psu.dto.response.PsuVersionResponse;
import com.example.psu.entity.PsuUnit;
import com.example.psu.enums.PsuTag;
import com.example.psu.service.AsyncDispatchService;
import com.example.psu.service.PsuService;
import com.example.psu.service.ReleaseService;

import jakarta.validation.Valid;
import java.util.List;

/**
 * PSU管理控制器
 */
@RestController
@RequestMapping("/api/psus")
public class PsuController {

    private static final Long DEFAULT_OPERATOR_ID = 0L;
    
    @Autowired
    private PsuService psuService;
    @Autowired
    private AsyncDispatchService asyncDispatchService;
    @Autowired
    private ReleaseService releaseService;
    
    /**
     * 创建 PSU（同步）。
     * 请求方法与路径：POST /api/psus（兼容 /api/v1/psus）。
     * 入参：PsuCreateRequest（psuId、name、description）。
     * 返回：PsuResponse（含数据库主键、状态、版本号、创建时间等）。
     */
    @PostMapping
    public ResponseEntity<PsuResponse> createPsu(@Valid @RequestBody PsuCreateRequest request) {
        PsuUnit psu = psuService.createPsu(request, DEFAULT_OPERATOR_ID);
        PsuResponse response = psuService.convertToResponse(psu);
        return ResponseEntity.ok(response);
    }
    
    /**
     * 分页查询 PSU 列表。
     * 请求方法与路径：GET /api/psus（兼容 /api/v1/psus）。
     * 入参：page、size、name（可选名称模糊筛选）。
     * 返回：Page<PsuResponse>。
     */
    @GetMapping
    public ResponseEntity<Page<PsuResponse>> getPsus(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String name) {
        Page<PsuUnit> psus = psuService.getPsus(page, size, name);
        Page<PsuResponse> responsePage = psus.map(psuService::convertToResponse);
        return ResponseEntity.ok(responsePage);
    }

    /**
     * 创建 PSU（异步）。
     * 请求方法与路径：POST /api/psus/async（兼容 /api/v1/...）。
     * 入参：PsuCreateRequest。
     * 返回：202 ACCEPTED。
     */
    @PostMapping("/async")
    public ResponseEntity<String> createPsuAsync(@Valid @RequestBody PsuCreateRequest request) {
        asyncDispatchService.dispatch(() -> psuService.createPsu(request, DEFAULT_OPERATOR_ID));
        return ResponseEntity.status(HttpStatus.ACCEPTED).body("ACCEPTED");
    }
    
    /**
     * 更新 PSU（同步）。
     * 请求方法与路径：PUT /api/psus/by-id（兼容 /api/v1/...）。
     * 入参：id + PsuCreateRequest。
     * 返回：更新后的 PsuResponse。
     */
    @PostMapping("/by-id")
    public ResponseEntity<PsuResponse> updatePsu(@RequestParam Long id, @Valid @RequestBody PsuCreateRequest request) {
        PsuUnit psu = psuService.updatePsu(id, request);
        PsuResponse response = psuService.convertToResponse(psu);
        return ResponseEntity.ok(response);
    }

    /**
     * 更新 PSU（异步）。
     * 请求方法与路径：PUT /api/psus/by-id/async（兼容 /api/v1/...）。
     * 入参：id + PsuCreateRequest。
     * 返回：202 ACCEPTED。
     */
    @PostMapping("/by-id/async")
    public ResponseEntity<String> updatePsuAsync(@RequestParam Long id, @Valid @RequestBody PsuCreateRequest request) {
        asyncDispatchService.dispatch(() -> psuService.updatePsu(id, request));
        return ResponseEntity.status(HttpStatus.ACCEPTED).body("ACCEPTED");
    }
    
    /**
     * 按数据库主键查询 PSU 详情。
     * 请求方法与路径：GET /api/psus/by-id（兼容 /api/v1/...）。
     * 入参：id。
     * 返回：PsuResponse。
     */
    @GetMapping("/by-id")
    public ResponseEntity<PsuResponse> getPsuById(@RequestParam Long id) {
        PsuUnit psu = psuService.getPsuById(id);
        PsuResponse response = psuService.convertToResponse(psu);
        return ResponseEntity.ok(response);
    }
    
    /**
     * 按业务 PSU ID + 标签查询该标签对应的Prompt与Schema内容。
     * 请求方法与路径：GET /api/psus/by-psu-id/by-psuId?tag=FORMAL|PREVIEW（兼容 /api/v1/...）。
     * 入参：psuId（业务唯一标识）+ tag（FORMAL/PREVIEW）。
     * 返回：PromptSchemaResolveResponse（含json schema与prompt）。
     */
    @GetMapping("/by-psu-id/by-psuId")
    public ResponseEntity<PromptSchemaResolveResponse> getPsuByPsuId(
            @RequestParam String psuId,
            @RequestParam String tag) {
        PromptSchemaResolveResponse response = releaseService.getPromptAndSchema(
            psuId,
            PsuTag.valueOf(tag.trim().toUpperCase())
        );
        return ResponseEntity.ok(response);
    }

    /**
     * 查询指定PSU的版本历史（新到旧）。
     * 请求方法与路径：GET /api/psus/by-psuId/versions。
     */
    @GetMapping("/by-psuId/versions")
    public ResponseEntity<List<PsuVersionResponse>> getPsuVersionHistory(@RequestParam String psuId) {
        return ResponseEntity.ok(psuService.getPsuVersionHistory(psuId));
    }

    /**
     * 查询指定PSU可提交审核的版本列表（新到旧）。
     * 请求方法与路径：GET /api/psus/by-psuId/submittable-versions。
     */
    @GetMapping("/by-psuId/submittable-versions")
    public ResponseEntity<List<PsuVersionResponse>> getSubmittablePsuVersions(@RequestParam String psuId) {
        return ResponseEntity.ok(psuService.getSubmittablePsuVersions(psuId));
    }
    
    /**
     * 归档/删除 PSU（同步，具体行为由服务层状态机决定）。
     * 请求方法与路径：DELETE /api/psus/by-id（兼容 /api/v1/...）。
     * 入参：id。
     * 返回：200 OK（无 body）。
     */
    @DeleteMapping("/by-id")
    public ResponseEntity<Void> deletePsu(@RequestParam Long id) {
        psuService.deletePsu(id);
        return ResponseEntity.ok().build();
    }

    /**
     * 归档/删除 PSU（异步）。
     * 请求方法与路径：DELETE /api/psus/by-id/async（兼容 /api/v1/...）。
     * 入参：id。
     * 返回：202 ACCEPTED。
     */
    @DeleteMapping("/by-id/async")
    public ResponseEntity<String> deletePsuAsync(@RequestParam Long id) {
        asyncDispatchService.dispatch(() -> psuService.deletePsu(id));
        return ResponseEntity.status(HttpStatus.ACCEPTED).body("ACCEPTED");
    }
}




