package com.example.psu.controller;

import com.example.psu.dto.request.UpdateParamSetRequest;
import com.example.psu.dto.response.ParamSetResponse;
import com.example.psu.entity.ParamSet;
import com.example.psu.repository.UserRepository;
import com.example.psu.service.ParamSetService;
import jakarta.validation.Valid;
import org.springframework.beans.BeanUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 参数集管理控制器
 */
@RestController
@RequestMapping("/api/param-sets")
public class ParamSetController {

    private static final Long DEFAULT_OPERATOR_ID = 0L;

    private final ParamSetService paramSetService;
    private final UserRepository userRepository;

    public ParamSetController(ParamSetService paramSetService, UserRepository userRepository) {
        this.paramSetService = paramSetService;
        this.userRepository = userRepository;
    }

    /**
     * 获取当前参数集
     */
    @GetMapping("/{psuId}")
    public ResponseEntity<ParamSetResponse> getParamSetByPsuId(@PathVariable Long psuId) {
        ParamSet paramSet = paramSetService.getParamSetByPsuId(psuId);
        return ResponseEntity.ok(toResponse(paramSet));
    }

    /**
     * 覆盖写参数集
     */
    @PutMapping("/{psuId}")
    public ResponseEntity<ParamSetResponse> updateParamSet(
        @PathVariable Long psuId,
        @Valid @RequestBody UpdateParamSetRequest requestBody
    ) {
        ParamSet paramSet = paramSetService.updateParamSet(
            psuId,
            requestBody.getParamSetContent(),
            DEFAULT_OPERATOR_ID,
            requestBody.getChangeLog()
        );
        return ResponseEntity.ok(toResponse(paramSet));
    }

    private ParamSetResponse toResponse(ParamSet paramSet) {
        ParamSetResponse response = new ParamSetResponse();
        BeanUtils.copyProperties(paramSet, response);
        userRepository.findById(paramSet.getModifiedBy()).ifPresent(modifier -> response.setModifierName(modifier.getUsername()));
        return response;
    }
}
