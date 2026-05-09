package com.example.psu.controller;

import com.example.psu.dto.request.UpdateParamSetRequest;
import com.example.psu.dto.response.ParamSetResponse;
import com.example.psu.entity.ParamSet;
import com.example.psu.exception.RequestValidationUtils;
import com.example.psu.repository.UserRepository;
import com.example.psu.service.AsyncDispatchService;
import com.example.psu.service.ParamSetService;
import jakarta.validation.Valid;
import org.springframework.beans.BeanUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Objects;

/**
 * 参数集管理控制器
 */
@RestController
@RequestMapping("/api/param-sets")
public class ParamSetController {

    private static final Long DEFAULT_OPERATOR_ID = 0L;

    private final ParamSetService paramSetService;
    private final UserRepository userRepository;
    private final AsyncDispatchService asyncDispatchService;

    public ParamSetController(ParamSetService paramSetService, UserRepository userRepository, AsyncDispatchService asyncDispatchService) {
        this.paramSetService = paramSetService;
        this.userRepository = userRepository;
        this.asyncDispatchService = asyncDispatchService;
    }

    /**
     * 查询指定 PSU 的参数集。
     * 请求方法与路径：GET /api/param-sets/by-psuId（兼容 /api/v1/...）。
     * 入参：psuId。
     * 返回：ParamSetResponse。
     */
    @GetMapping("/by-psuId")
    public ResponseEntity<ParamSetResponse> getParamSetByPsuId(@RequestParam Long psuId) {
        RequestValidationUtils.requireNonNull(psuId, "psuId");
        ParamSet paramSet = paramSetService.getParamSetByPsuId(psuId);
        return ResponseEntity.ok(toResponse(paramSet));
    }

    /**
     * 覆盖写参数集（同步）。
     * 请求方法与路径：PUT /api/param-sets/by-psuId（兼容 /api/v1/...）。
     * 入参：psuId + UpdateParamSetRequest（paramSetContent、changeLog）。
     * 返回：更新后的 ParamSetResponse。
     */
    @PostMapping("/by-psuId")
    public ResponseEntity<ParamSetResponse> updateParamSet(
        @RequestParam Long psuId,
        @Valid @RequestBody UpdateParamSetRequest requestBody
    ) {
        RequestValidationUtils.requireNonNull(psuId, "psuId");
        requestBody = RequestValidationUtils.requireNonNull(requestBody, "requestBody");
        ParamSet paramSet = paramSetService.updateParamSet(
            psuId,
            requestBody.getParamSetContent(),
            DEFAULT_OPERATOR_ID,
            requestBody.getChangeLog()
        );
        return ResponseEntity.ok(toResponse(paramSet));
    }

    /**
     * 覆盖写参数集（异步）。
     * 请求方法与路径：PUT /api/param-sets/by-psuId/async（兼容 /api/v1/...）。
     * 入参：psuId + UpdateParamSetRequest。
     * 返回：202 ACCEPTED。
     */
    @PostMapping("/by-psuId/async")
    public ResponseEntity<String> updateParamSetAsync(
        @RequestParam Long psuId,
        @Valid @RequestBody UpdateParamSetRequest requestBody
    ) {
        asyncDispatchService.dispatch(() -> paramSetService.updateParamSet(
            psuId,
            requestBody.getParamSetContent(),
            DEFAULT_OPERATOR_ID,
            requestBody.getChangeLog()
        ));
        return ResponseEntity.status(HttpStatus.ACCEPTED).body("ACCEPTED");
    }

    private ParamSetResponse toResponse(ParamSet paramSet) {
        RequestValidationUtils.requireNonNull(paramSet, "paramSet");
        ParamSet safeParamSet = Objects.requireNonNull(paramSet);
        ParamSetResponse response = new ParamSetResponse();
        BeanUtils.copyProperties(safeParamSet, response);
        Long modifierId = safeParamSet.getModifiedBy() == null ? 0L : safeParamSet.getModifiedBy();
        userRepository.findById(modifierId).ifPresent(modifier -> response.setModifierName(modifier.getUsername()));
        return response;
    }
}





