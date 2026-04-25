package com.example.psu.service;

import com.example.psu.entity.ParamSet;
import com.example.psu.entity.PsuUnit;
import com.example.psu.enums.PsuStatus;
import com.example.psu.repository.ParamSetRepository;
import com.example.psu.repository.PsuRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

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
        // 先校验PSU存在，避免返回误导性404。
        psuRepository.findById(psuId)
            .orElseThrow(() -> new RuntimeException("PSU not found: " + psuId));
        return paramSetRepository.findByPsuId(psuId)
            .orElseThrow(() -> new RuntimeException("Param set not found for PSU: " + psuId));
    }

    /**
     * 覆盖写参数集
     */
    public ParamSet updateParamSet(Long psuId, String paramSetContent, Long userId, String changeLog) {
        // 先校验PSU存在，再执行覆盖写。
        PsuUnit psu = psuRepository.findById(psuId)
            .orElseThrow(() -> new RuntimeException("PSU not found: " + psuId));
        if (psu.getStatus() != PsuStatus.DRAFT) {
            throw new RuntimeException("当前PSU为只读状态，仅草稿可编辑参数集");
        }
        try {
            objectMapper.readTree(paramSetContent);
        } catch (Exception e) {
            throw new RuntimeException("Invalid param set JSON format");
        }
        ParamSet paramSet = paramSetRepository.findByPsuId(psuId).orElseGet(() -> {
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
