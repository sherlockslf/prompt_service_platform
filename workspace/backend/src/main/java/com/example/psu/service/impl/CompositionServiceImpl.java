package com.example.psu.service.impl;

import com.example.psu.dto.request.CompositionRenderRequest;
import com.example.psu.dto.request.CompositionSaveRequest;
import com.example.psu.dto.response.CompositionRenderResponse;
import com.example.psu.dto.response.CompositionValidateResponse;
import com.example.psu.entity.JsonSchema;
import com.example.psu.entity.PromptComposition;
import com.example.psu.entity.PromptCompositionRevision;
import com.example.psu.enums.CompositionStatus;
import com.example.psu.enums.RejectionType;
import com.example.psu.exception.BusinessException;
import com.example.psu.exception.ErrorCode;
import com.example.psu.repository.JsonSchemaRepository;
import com.example.psu.repository.PromptCompositionRepository;
import com.example.psu.repository.PromptCompositionRevisionRepository;
import com.example.psu.service.CompositionService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 编排服务实现
 */
@Service
@Transactional
public class CompositionServiceImpl implements CompositionService {

    private static final Pattern TOKEN_PATTERN = Pattern.compile("\\{\\{\\s*([^}]+?)\\s*\\}\\}");

    private final PromptCompositionRepository compositionRepository;
    private final PromptCompositionRevisionRepository revisionRepository;
    private final JsonSchemaRepository jsonSchemaRepository;
    private final ObjectMapper objectMapper;

    public CompositionServiceImpl(
        PromptCompositionRepository compositionRepository,
        PromptCompositionRevisionRepository revisionRepository,
        JsonSchemaRepository jsonSchemaRepository,
        ObjectMapper objectMapper
    ) {
        this.compositionRepository = compositionRepository;
        this.revisionRepository = revisionRepository;
        this.jsonSchemaRepository = jsonSchemaRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public Optional<PromptComposition> getCompositionByPsuId(Long psuId) {
        return compositionRepository.findByPsuId(psuId);
    }

    @Override
    public PromptComposition saveDraft(Long psuId, CompositionSaveRequest request, Long userId) {
        if (request == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "请求体不能为空");
        }

        CompositionValidateResponse validateResponse = validate(psuId, request);
        if (!validateResponse.isOk()) {
            String firstError = String.valueOf(validateResponse.getErrors().get(0).getOrDefault("message", "编排校验失败"));
            throw new BusinessException(ErrorCode.BAD_REQUEST, firstError);
        }

        PromptComposition composition = compositionRepository.findByPsuId(psuId).orElse(null);
        if (composition == null) {
            composition = new PromptComposition();
            composition.setPsuId(psuId);
            composition.setCreatedBy(userId);
            composition.setStatus(CompositionStatus.DRAFT);
            composition.setSchemaVersion(resolveCurrentSchemaVersion(psuId));
        } else if (composition.getStatus() != CompositionStatus.DRAFT) {
            throw new BusinessException(ErrorCode.VERSION_ALREADY_SUBMITTED, "当前编排已锁定，不能继续保存草稿");
        }

        composition.setContent(request.getContent());
        composition.setSpecJson(writeSpecJson(request));
        composition.setUpdatedBy(userId);
        composition.setRejectionReason(null);
        composition.setRejectionType(null);

        return compositionRepository.save(composition);
    }

    @Override
    public CompositionValidateResponse validate(Long psuId, CompositionSaveRequest request) {
        CompositionValidateResponse response = new CompositionValidateResponse();

        if (request == null) {
            addError(response, "BAD_REQUEST", null, "请求体不能为空");
            response.setOk(false);
            return response;
        }

        String content = request.getContent();
        if (content == null || content.isBlank()) {
            addError(response, "CONTENT_EMPTY", "content", "content不能为空");
        }

        if (hasUnclosedBraces(content)) {
            addError(response, "UNCLOSED_BRACES", "content", "存在未闭合的 {{ 占位符");
        }

        Set<String> vars = extractVariables(content);
        Set<String> injectionPaths = extractInjectionPaths(request.getInjectionPlan());
        for (String var : vars) {
            if (!injectionPaths.contains(var)) {
                addError(response, "INJECTION_MISSING", var, "injectionPlan未包含变量: " + var);
            }
        }

        Optional<JsonSchema> schemaOpt = jsonSchemaRepository.findTopByPsuIdOrderByVersionDesc(psuId);
        if (schemaOpt.isPresent()) {
            JsonNode schemaRoot = parseSchema(schemaOpt.get().getSchemaContent());
            if (schemaRoot != null) {
                for (String var : vars) {
                    if (!existsInSchema(schemaRoot, var)) {
                        addError(response, "VAR_NOT_FOUND", var, "Schema中不存在字段: " + var);
                    }
                }
            } else {
                addWarning(response, "SCHEMA_PARSE_FAILED", null, "Schema解析失败，已跳过字段存在性校验");
            }
        } else {
            addWarning(response, "SCHEMA_NOT_FOUND", null, "未找到Schema，已跳过字段存在性校验");
        }

        response.setOk(response.getErrors().isEmpty());
        return response;
    }

    @Override
    public CompositionRenderResponse render(Long psuId, CompositionRenderRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "渲染请求不能为空");
        }

        PromptComposition composition;
        if (request.getCompositionId() != null) {
            composition = compositionRepository.findById(request.getCompositionId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "编排不存在"));
            if (!composition.getPsuId().equals(psuId)) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "编排不属于当前PSU");
            }
        } else {
            composition = compositionRepository.findByPsuId(psuId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "编排不存在"));
        }

        Map<String, Object> input = request.getInput() == null ? new HashMap<>() : request.getInput();
        String template = composition.getContent() == null ? "" : composition.getContent();

        Set<String> missing = new LinkedHashSet<>();
        Set<String> used = new LinkedHashSet<>();
        Matcher matcher = TOKEN_PATTERN.matcher(template);
        StringBuffer rendered = new StringBuffer();
        while (matcher.find()) {
            String rawPath = matcher.group(1).trim();
            used.add(rawPath);
            Object value = getValueByPath(input, rawPath);
            if (value == null) {
                missing.add(rawPath);
                matcher.appendReplacement(rendered, "");
            } else if (value instanceof Map || value instanceof List) {
                matcher.appendReplacement(rendered, Matcher.quoteReplacement(writeValueAsString(value)));
            } else {
                matcher.appendReplacement(rendered, Matcher.quoteReplacement(String.valueOf(value)));
            }
        }
        matcher.appendTail(rendered);

        CompositionRenderResponse response = new CompositionRenderResponse();
        response.setRenderedPrompt(rendered.toString());
        response.setMissingVars(new ArrayList<>(missing));
        response.setUsedVars(new ArrayList<>(used));
        return response;
    }

    @Override
    public PromptComposition submit(Long psuId, Long userId) {
        PromptComposition composition = compositionRepository.findByPsuId(psuId)
            .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "编排不存在"));

        if (composition.getStatus() != CompositionStatus.DRAFT) {
            throw new BusinessException(ErrorCode.VERSION_ALREADY_SUBMITTED, "仅DRAFT状态可提交审核");
        }

        CompositionSaveRequest request = toSaveRequest(composition);
        CompositionValidateResponse validateResponse = validate(psuId, request);
        if (!validateResponse.isOk()) {
            String firstError = String.valueOf(validateResponse.getErrors().get(0).getOrDefault("message", "编排校验失败"));
            throw new BusinessException(ErrorCode.BAD_REQUEST, firstError);
        }

        composition.setStatus(CompositionStatus.SUBMITTED);
        composition.setUpdatedBy(userId);
        PromptComposition saved = compositionRepository.save(composition);
        createRevisionSnapshot(saved, userId);
        return saved;
    }

    @Override
    public PromptComposition updateStatus(Long psuId, CompositionStatus status, String rejectionReason, RejectionType rejectionType) {
        PromptComposition composition = compositionRepository.findByPsuId(psuId)
            .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "编排不存在"));

        composition.setStatus(status);
        composition.setRejectionReason(rejectionReason);
        composition.setRejectionType(rejectionType);
        composition.setUpdatedBy(composition.getUpdatedBy() == null ? 0L : composition.getUpdatedBy());
        return compositionRepository.save(composition);
    }

    @Override
    public PromptCompositionRevision createRevisionSnapshot(PromptComposition composition, Long operatorId) {
        int nextRevisionNo = revisionRepository.findTopByCompositionIdOrderByRevisionNoDesc(composition.getId())
            .map(rev -> rev.getRevisionNo() + 1)
            .orElse(1);

        PromptCompositionRevision revision = new PromptCompositionRevision();
        revision.setCompositionId(composition.getId());
        revision.setPsuId(composition.getPsuId());
        revision.setRevisionNo(nextRevisionNo);
        revision.setStatusAtTime(composition.getStatus());
        revision.setSchemaVersion(composition.getSchemaVersion());
        revision.setSchemaVersionAtTime(composition.getSchemaVersion());
        revision.setContentSnapshot(composition.getContent() == null ? "" : composition.getContent());
        revision.setSpecJsonSnapshot(composition.getSpecJson() == null ? "{}" : composition.getSpecJson());
        revision.setCreatedBy(operatorId == null ? 0L : operatorId);

        return revisionRepository.save(revision);
    }

    @Override
    public Optional<PromptCompositionRevision> getLatestRevision(Long compositionId) {
        return revisionRepository.findTopByCompositionIdOrderByRevisionNoDesc(compositionId);
    }

    private Integer resolveCurrentSchemaVersion(Long psuId) {
        return jsonSchemaRepository.findTopByPsuIdOrderByVersionDesc(psuId)
            .map(JsonSchema::getVersion)
            .orElse(1);
    }

    private String writeSpecJson(CompositionSaveRequest request) {
        try {
            Map<String, Object> spec;
            if (request.getSpecJson() != null) {
                spec = request.getSpecJson();
                if (request.getContent() != null && !spec.containsKey("content")) {
                    spec.put("content", request.getContent());
                }
            } else {
                spec = new HashMap<>();
                spec.put("content", request.getContent());
                spec.put("tokens", request.getTokens() == null ? List.of() : request.getTokens());
                spec.put("injectionPlan", request.getInjectionPlan() == null ? List.of() : request.getInjectionPlan());
                spec.put("assembledFragments", request.getAssembledFragments() == null ? List.of() : request.getAssembledFragments());
            }
            return objectMapper.writeValueAsString(spec);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "specJson序列化失败");
        }
    }

    private CompositionSaveRequest toSaveRequest(PromptComposition composition) {
        CompositionSaveRequest request = new CompositionSaveRequest();
        request.setContent(composition.getContent());
        if (composition.getSpecJson() != null && !composition.getSpecJson().isBlank()) {
            try {
                Map<String, Object> spec = objectMapper.readValue(composition.getSpecJson(), new TypeReference<Map<String, Object>>() {
                });
                request.setSpecJson(spec);
                request.setTokens(readListMap(spec.get("tokens")));
                request.setInjectionPlan(readListMap(spec.get("injectionPlan")));
                request.setAssembledFragments(readListMap(spec.get("assembledFragments")));
            } catch (Exception ignored) {
                request.setSpecJson(Map.of());
            }
        }
        return request;
    }

    private List<Map<String, Object>> readListMap(Object node) {
        if (!(node instanceof List<?> list)) {
            return List.of();
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof Map<?, ?> map) {
                Map<String, Object> normalized = new HashMap<>();
                map.forEach((k, v) -> normalized.put(String.valueOf(k), v));
                result.add(normalized);
            }
        }
        return result;
    }

    private JsonNode parseSchema(String schemaContent) {
        if (schemaContent == null || schemaContent.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readTree(schemaContent);
        } catch (Exception e) {
            return null;
        }
    }

    private Set<String> extractVariables(String content) {
        Set<String> vars = new LinkedHashSet<>();
        if (content == null || content.isBlank()) {
            return vars;
        }
        Matcher matcher = TOKEN_PATTERN.matcher(content);
        while (matcher.find()) {
            String path = matcher.group(1).trim();
            if (!path.isEmpty()) {
                vars.add(path);
            }
        }
        return vars;
    }

    private Set<String> extractInjectionPaths(List<Map<String, Object>> injectionPlan) {
        Set<String> paths = new HashSet<>();
        if (injectionPlan == null) {
            return paths;
        }
        for (Map<String, Object> item : injectionPlan) {
            if (item == null) {
                continue;
            }
            Object path = item.get("path");
            if (path != null && !String.valueOf(path).isBlank()) {
                paths.add(String.valueOf(path));
            }
        }
        return paths;
    }

    private boolean hasUnclosedBraces(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        int i = 0;
        while (i < text.length()) {
            if (i + 1 < text.length() && text.charAt(i) == '{' && text.charAt(i + 1) == '{') {
                int close = text.indexOf("}}", i + 2);
                if (close < 0) {
                    return true;
                }
                i = close + 2;
            } else {
                i++;
            }
        }
        return false;
    }

    private boolean existsInSchema(JsonNode schemaRoot, String rawPath) {
        if (schemaRoot == null || rawPath == null || rawPath.isBlank()) {
            return false;
        }
        String normalized = rawPath.replaceAll("\\[(\\d+)\\]", ".$1");
        String[] parts = normalized.split("\\.");
        
        // 首先尝试标准JSON Schema格式 (properties-based)
        if (schemaRoot.has("properties")) {
            JsonNode current = schemaRoot;
            for (String part : parts) {
                if (part.isBlank()) {
                    continue;
                }
                JsonNode properties = current.get("properties");
                if (properties == null || !properties.has(part)) {
                    return false;
                }
                current = properties.get(part);
                if (current == null) {
                    return false;
                }
                if ("array".equals(current.path("type").asText()) && current.has("items")) {
                    current = current.get("items");
                }
            }
            return true;
        } 
        // 如果不是标准格式，尝试简单JSON对象格式
        else {
            JsonNode current = schemaRoot;
            for (String part : parts) {
                if (part.isBlank()) {
                    continue;
                }
                // 直接检查当前节点是否包含该字段
                if (!current.has(part)) {
                    return false;
                }
                current = current.get(part);
                if (current == null) {
                    return false;
                }
                // 如果当前节点是数组类型，继续向下查找
                if (current.isArray() && current.size() > 0) {
                    // 对于数组，我们假设第一个元素具有代表性
                    current = current.get(0);
                } else if (current.isObject()) {
                    // 如果是对象，继续使用当前节点
                    continue;
                } else {
                    // 如果是基本类型，无法进一步深入
                    break;
                }
            }
            return true;
        }
    }

    private Object getValueByPath(Map<String, Object> source, String rawPath) {
        if (source == null || rawPath == null || rawPath.isBlank()) {
            return null;
        }
        String normalized = rawPath.replaceAll("\\[(\\d+)\\]", ".$1");
        String[] parts = normalized.split("\\.");
        Object current = source;
        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }
            if (current instanceof Map<?, ?> map) {
                current = map.get(part);
            } else if (current instanceof List<?> list) {
                int idx;
                try {
                    idx = Integer.parseInt(part);
                } catch (NumberFormatException e) {
                    return null;
                }
                if (idx < 0 || idx >= list.size()) {
                    return null;
                }
                current = list.get(idx);
            } else {
                return null;
            }
            if (current == null) {
                return null;
            }
        }
        return current;
    }

    private String writeValueAsString(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return String.valueOf(value);
        }
    }

    private void addError(CompositionValidateResponse response, String code, String path, String message) {
        Map<String, Object> error = new HashMap<>();
        error.put("code", code);
        error.put("path", path);
        error.put("message", message);
        response.getErrors().add(error);
    }

    private void addWarning(CompositionValidateResponse response, String code, String path, String message) {
        Map<String, Object> warning = new HashMap<>();
        warning.put("code", code);
        warning.put("path", path);
        warning.put("message", message);
        response.getWarnings().add(warning);
    }
}
