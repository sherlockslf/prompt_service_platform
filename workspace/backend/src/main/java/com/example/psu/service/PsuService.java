package com.example.psu.service;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.example.psu.dto.PsuCreateRequest;
import com.example.psu.dto.response.PsuResponse;
import com.example.psu.dto.response.PsuVersionResponse;
import com.example.psu.entity.PsuUnit;
import com.example.psu.entity.PsuVersion;
import com.example.psu.enums.PsuStatus;
import com.example.psu.exception.BusinessException;
import com.example.psu.exception.ErrorCode;
import com.example.psu.exception.RequestValidationUtils;
import com.example.psu.repository.JsonSchemaRepository;
import com.example.psu.repository.PromptCompositionRepository;
import com.example.psu.repository.PromptCompositionRevisionRepository;
import com.example.psu.repository.PsuRepository;
import com.example.psu.repository.PsuVersionRepository;
import com.example.psu.repository.UserRepository;
import com.example.psu.repository.VersionReviewRepository;

/**
 * PSU管理服务
 */
@Service
public class PsuService {
    
    @Autowired
    private PsuRepository psuRepository;
    
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PsuVersionRepository psuVersionRepository;

    @Autowired
    private JsonSchemaRepository jsonSchemaRepository;

    @Autowired
    private PromptCompositionRepository promptCompositionRepository;

    @Autowired
    private PromptCompositionRevisionRepository promptCompositionRevisionRepository;

    @Autowired
    private VersionReviewRepository versionReviewRepository;
    
    /**
     * 创建PSU（仅研发可操作）
     * @param request 创建请求
     * @param creatorId 创建者ID
     * @return 创建的PSU
     */
    public PsuUnit createPsu(PsuCreateRequest request, Long creatorId) {
        request = RequestValidationUtils.requireNonNull(request, "request");
        RequestValidationUtils.requireNonBlank(request.getPsuId(), "psuId");
        RequestValidationUtils.requireNonBlank(request.getName(), "name");
        RequestValidationUtils.requireNonNull(creatorId, "creatorId");
        // 检查PSU ID是否已存在
        if (psuRepository.existsByPsuId(request.getPsuId())) {
            throw new BusinessException(ErrorCode.PSU_ALREADY_EXISTS, "PSU ID已存在: " + request.getPsuId());
        }
        
        PsuUnit psu = new PsuUnit();
        psu.setPsuId(request.getPsuId());
        psu.setName(request.getName());
        psu.setDescription(request.getDescription());
        psu.setStatus(PsuStatus.DRAFT);
        psu.setCreatorId(creatorId);
        psu.setVersionNo(1);
        
        PsuUnit saved = psuRepository.save(psu);
        recordVersionSnapshot(saved, creatorId, "CREATE_PSU");
        return saved;
    }
    
    /**
     * 分页查询PSU列表
     * @param page 页码（从1开始）
     * @param size 每页大小
     * @return 分页结果
     */
    public Page<PsuUnit> getPsus(int page, int size, String name) {
        // 页码从0开始，所以减1
        int pageNum = Math.max(0, page - 1);
        Pageable pageable = PageRequest.of(pageNum, size, Sort.by(Sort.Direction.DESC, "updatedAt"));
        if (StringUtils.hasText(name)) {
            return psuRepository.findByNameContainingIgnoreCaseOrderByUpdatedAtDesc(name.trim(), pageable);
        }
        return psuRepository.findAll(pageable);
    }
    
    /**
     * 更新PSU信息
     * @param id PSU数据库ID
     * @param request 更新请求
     * @return 更新后的PSU
     */
    public PsuUnit updatePsu(Long id, PsuCreateRequest request) {
        RequestValidationUtils.requireNonNull(id, "id");
        request = RequestValidationUtils.requireNonNull(request, "request");
        Long safeId = Objects.requireNonNull(id);
        PsuUnit psu = psuRepository.findById(safeId)
            .orElseThrow(() -> new BusinessException(ErrorCode.PSU_NOT_FOUND, "PSU不存在，ID: " + safeId));
        
        // 仅草稿允许编辑，候选/正式/归档均只读
        if (psu.getStatus() != PsuStatus.DRAFT) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "仅草稿状态允许编辑");
        }
        
        // 更新字段
        psu.setName(request.getName());
        psu.setDescription(request.getDescription());
        
        PsuUnit saved = psuRepository.save(psu);
        recordVersionSnapshot(saved, 0L, "UPDATE_PSU_INFO");
        return saved;
    }
    
    /**
     * 根据数据库ID获取PSU信息
     * @param id PSU数据库ID
     * @return PSU信息
     */
    public PsuUnit getPsuById(Long id) {
        RequestValidationUtils.requireNonNull(id, "id");
        Long safeId = Objects.requireNonNull(id);
        return psuRepository.findById(safeId)
            .orElseThrow(() -> new BusinessException(ErrorCode.PSU_NOT_FOUND, "PSU不存在，ID: " + safeId));
    }
    
    /**
     * 根据PSU ID（全局唯一ID）获取PSU信息
     * @param psuId PSU全局唯一ID
     * @return PSU信息
     */
    public PsuUnit getPsuByPsuId(String psuId) {
        RequestValidationUtils.requireNonBlank(psuId, "psuId");
        return psuRepository.findByPsuId(psuId)
            .orElseThrow(() -> new BusinessException(ErrorCode.PSU_NOT_FOUND, "PSU不存在，PSU ID: " + psuId));
    }

    /**
     * 删除PSU（归档）
     * @param id PSU数据库ID
     */
    public void deletePsu(Long id) {
        RequestValidationUtils.requireNonNull(id, "id");
        Long safeId = Objects.requireNonNull(id);
        PsuUnit psu = psuRepository.findById(safeId)
            .orElseThrow(() -> new BusinessException(ErrorCode.PSU_NOT_FOUND, "PSU不存在，ID: " + safeId));
        
        // 正式版禁止删除；非正式状态统一归档
        if (psu.getStatus() == PsuStatus.FORMAL) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "正式版本PSU不允许删除");
        }
        psu.setStatus(PsuStatus.ARCHIVED);
        psuRepository.save(psu);
    }

    public PsuUnit bumpVersionAndSnapshot(Long psuRefId, Long operatorId, String changeSource) {
        RequestValidationUtils.requireNonNull(psuRefId, "psuRefId");
        PsuUnit psu = psuRepository.findById(psuRefId)
            .orElseThrow(() -> new BusinessException(ErrorCode.PSU_NOT_FOUND, "PSU不存在，ID: " + psuRefId));
        psu.setVersionNo((psu.getVersionNo() == null ? 0 : psu.getVersionNo()) + 1);
        PsuUnit saved = psuRepository.save(psu);
        recordVersionSnapshot(saved, operatorId, changeSource);
        return saved;
    }

    public List<PsuVersionResponse> getPsuVersionHistory(String psuId) {
        RequestValidationUtils.requireNonBlank(psuId, "psuId");
        return psuVersionRepository.findByPsuIdOrderByVersionNoDesc(psuId).stream()
            .map(this::toPsuVersionResponse)
            .collect(Collectors.toList());
    }

    public List<PsuVersionResponse> getSubmittablePsuVersions(String psuId) {
        RequestValidationUtils.requireNonBlank(psuId, "psuId");
        PsuUnit psu = psuRepository.findByPsuId(psuId)
            .orElseThrow(() -> new BusinessException(ErrorCode.PSU_NOT_FOUND, "PSU不存在，PSU ID: " + psuId));
        Long psuRefId = psu.getId();
        return psuVersionRepository.findByPsuIdOrderByVersionNoDesc(psuId).stream()
            .filter(this::isVersionSubmittable)
            .filter(v -> versionReviewRepository.findByPsuIdAndVersionNo(psuRefId, v.getVersionNo()).isEmpty())
            .map(this::toPsuVersionResponse)
            .collect(Collectors.toList());
    }
    
    /**
     * 将PSU实体转换为响应DTO
     * @param psu PSU实体
     * @return PSU响应DTO
     */
    public PsuResponse convertToResponse(PsuUnit psu) {
        RequestValidationUtils.requireNonNull(psu, "psu");
        PsuUnit safePsu = Objects.requireNonNull(psu);
        PsuResponse response = new PsuResponse();
        BeanUtils.copyProperties(safePsu, response);
        response.setStatus(safePsu.getStatus().getCode());
        response.setVersionNo(safePsu.getVersionNo());
        
        // 获取创建者名称
        Long creatorId = Objects.requireNonNull(safePsu.getCreatorId());
        userRepository.findById(creatorId).ifPresent(creator -> {
            response.setCreatorName(creator.getUsername());
        });
        
        return response;
    }

    private void recordVersionSnapshot(PsuUnit psu, Long operatorId, String changeSource) {
        if (psu == null || psu.getPsuId() == null || psu.getVersionNo() == null) {
            return;
        }
        if (psuVersionRepository.findByPsuIdAndVersionNo(psu.getPsuId(), psu.getVersionNo()).isPresent()) {
            return;
        }
        PsuVersion version = new PsuVersion();
        version.setPsuRefId(psu.getId());
        version.setPsuId(psu.getPsuId());
        version.setVersionNo(psu.getVersionNo());
        version.setName(psu.getName());
        version.setDescription(psu.getDescription());
        version.setStatus(psu.getStatus());
        version.setOperatorId(operatorId == null ? 0L : operatorId);
        version.setChangeSource(changeSource == null || changeSource.isBlank() ? "UNKNOWN" : changeSource);
        jsonSchemaRepository.findTopByPsuIdOrderByVersionDesc(psu.getId()).ifPresent(schema -> {
            version.setSchemaVersionNo(schema.getVersion());
        });
        promptCompositionRepository.findByPsuId(psu.getId()).ifPresent(composition -> {
            version.setCompositionId(composition.getId());
            promptCompositionRevisionRepository.findTopByCompositionIdOrderByRevisionNoDesc(composition.getId())
                .ifPresent(revision -> version.setCompositionRevisionNo(revision.getRevisionNo()));
        });
        psuVersionRepository.save(version);
    }

    private PsuVersionResponse toPsuVersionResponse(PsuVersion v) {
        PsuVersionResponse response = new PsuVersionResponse();
        response.setId(v.getId());
        response.setPsuRefId(v.getPsuRefId());
        response.setPsuId(v.getPsuId());
        response.setVersionNo(v.getVersionNo());
        response.setName(v.getName());
        response.setDescription(v.getDescription());
        response.setStatus(v.getStatus() == null ? null : v.getStatus().getCode());
        response.setOperatorId(v.getOperatorId());
        response.setChangeSource(v.getChangeSource());
        response.setSchemaVersionNo(v.getSchemaVersionNo());
        response.setCompositionId(v.getCompositionId());
        response.setCompositionRevisionNo(v.getCompositionRevisionNo());
        response.setCreatedAt(v.getCreatedAt());
        return response;
    }

    private boolean isVersionSubmittable(PsuVersion v) {
        if (v == null) return false;
        if (v.getStatus() == PsuStatus.FORMAL) return false;
        if (v.getSchemaVersionNo() == null) return false;
        if (v.getCompositionId() == null || v.getCompositionRevisionNo() == null) return false;
        return true;
    }
}


