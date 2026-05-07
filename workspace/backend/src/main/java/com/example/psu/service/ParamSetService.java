package com.example.psu.service;

import com.example.psu.entity.ParamSet;
import com.example.psu.entity.PsuUnit;
import com.example.psu.enums.PsuStatus;
import com.example.psu.exception.RequestValidationUtils;
import com.example.psu.repository.ParamSetRepository;
import com.example.psu.repository.PsuRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * 参数集管理服务（覆盖写）
 */
@Service
public class ParamSetService {

    private final ParamSetRepository paramSetRepository;
    private final PsuRepository psuRepository;
    private final ObjectMapper objectMapper;

    public ParamSetService(
        ParamSetRepository paramSetRepository,
        PsuRepository psuRepository,
        ObjectMapper objectMapper
    ) {
        this.paramSetRepository = paramSetRepository;
        this.psuRepository = psuRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * 查询当前PSU的参数集
     */
    public ParamSet getParamSetByPsuId(Long psuId) {
        RequestValidationUtils.requireNonNull(psuId, "psuId");
        Long safePsuId = Objects.requireNonNull(psuId);
        // 先校验PSU存在，避免返回误导性404。
        psuRepository.findById(safePsuId)
            .orElseThrow(() -> new RuntimeException("PSU not found: " + safePsuId));
        return paramSetRepository.findByPsuId(safePsuId)
            .orElseThrow(() -> new RuntimeException("Param set not found for PSU: " + safePsuId));
    }

    /**
     * 覆盖写参数集
     */
    public ParamSet updateParamSet(Long psuId, String paramSetContent, Long userId, String changeLog) {
        RequestValidationUtils.requireNonNull(psuId, "psuId");
        RequestValidationUtils.requireNonBlank(paramSetContent, "paramSetContent");
        Long safePsuId = Objects.requireNonNull(psuId);
        // 先校验PSU存在，再执行覆盖写。
        PsuUnit psu = psuRepository.findById(safePsuId)
            .orElseThrow(() -> new RuntimeException("PSU not found: " + safePsuId));
        if (psu.getStatus() == PsuStatus.ARCHIVED) {
            throw new RuntimeException("当前PSU已删除，不允许编辑参数集");
        }
        try {
            objectMapper.readTree(paramSetContent);
        } catch (Exception e) {
            throw new RuntimeException("Invalid param set JSON format");
        }
        ParamSet paramSet = paramSetRepository.findByPsuId(safePsuId).orElseGet(() -> {
            ParamSet created = new ParamSet();
            created.setPsuId(psu.getId());
            return created;
        });
        paramSet.setParamSetContent(paramSetContent);
        paramSet.setModifiedBy(userId == null ? 0L : userId);
        paramSet.setChangeLog(changeLog);
        return paramSetRepository.save(paramSet);
    }
}


