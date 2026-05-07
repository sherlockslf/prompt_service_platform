package com.example.psu.service;

import com.example.psu.dto.request.CreatePromptRequest;
import com.example.psu.dto.response.PromptTestResponse;
import com.example.psu.entity.PromptFragment;
import com.example.psu.entity.PsuUnit;
import com.example.psu.enums.PsuStatus;
import com.example.psu.exception.RequestValidationUtils;
import com.example.psu.repository.PromptFragmentRepository;
import com.example.psu.repository.PsuRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Prompt管理服务
 *
 * @author SLF
 * @date 2026-04-29
 * @description 提供Prompt片段生命周期管理及统一测试能力
 */
@Service
public class PromptService {
    
    @Autowired
    private PromptFragmentRepository promptFragmentRepository;
    
    @Autowired
    private PsuRepository psuRepository;
    
    /**
     * 获取Prompt片段
     * @param psuId PSU ID
     * @return Prompt片段列表
     */
    public List<PromptFragment> getPromptFragments(Long psuId) {
        RequestValidationUtils.requireNonNull(psuId, "psuId");
        Long safePsuId = Objects.requireNonNull(psuId);
        // 检查PSU是否存在
        psuRepository.findById(safePsuId)
                .orElseThrow(() -> new RuntimeException("PSU not found: " + safePsuId));
        
        return promptFragmentRepository.findByPsuIdOrderBySortOrderAsc(safePsuId);
    }
    
    /**
     * 创建Prompt片段
     * @param request 创建请求
     * @param userId 用户ID
     * @return 创建的片段
     */
    public PromptFragment createPromptFragment(CreatePromptRequest request, Long userId) {
        RequestValidationUtils.requireNonNull(request, "request");
        RequestValidationUtils.requireNonNull(request.getPsuId(), "psuId");
        RequestValidationUtils.requireNonBlank(request.getFragmentKey(), "fragmentKey");
        RequestValidationUtils.requireNonBlank(request.getContent(), "content");
        Long safePsuId = Objects.requireNonNull(request.getPsuId());
        // 检查PSU是否存在
        PsuUnit psu = psuRepository.findById(safePsuId)
                .orElseThrow(() -> new RuntimeException("PSU not found: " + safePsuId));
        assertEditable(psu);
        
        // 检查fragmentKey是否已存在
        Optional<PromptFragment> existingFragment = promptFragmentRepository
                .findByPsuIdAndFragmentKey(safePsuId, request.getFragmentKey());
        if (existingFragment.isPresent()) {
            throw new RuntimeException("该PSU下已存在相同标识的Prompt片段: " + request.getFragmentKey());
        }
        
        // 创建新的Prompt片段
        PromptFragment fragment = new PromptFragment();
        fragment.setPsuId(safePsuId);
        fragment.setFragmentKey(request.getFragmentKey());
        fragment.setContent(request.getContent());
        fragment.setEditable(request.getEditable());
        fragment.setType(request.getType());
        fragment.setSortOrder(request.getSortOrder());
        
        PromptFragment saved = promptFragmentRepository.save(fragment);
        bumpPromptVersion(psu);
        return saved;
    }
    
    /**
     * 更新Prompt片段（权限分级）
     * 规则：
     * - editable=true：所有人可编辑
     * - editable=false（已定版）：所有人不可修改
     * @param fragmentId 片段ID
     * @param content 内容
     * @param userId 用户ID
     * @return 更新后的片段
     */
    public PromptFragment updatePromptFragment(Long fragmentId, Integer baseVersionNo, String content, Long userId) {
        RequestValidationUtils.requireNonNull(fragmentId, "fragmentId");
        RequestValidationUtils.requireNonBlank(content, "content");
        Long safeFragmentId = Objects.requireNonNull(fragmentId);
        // 获取片段
        PromptFragment fragment = promptFragmentRepository.findById(safeFragmentId)
                .orElseThrow(() -> new RuntimeException("Prompt fragment not found: " + safeFragmentId));

        Long fragmentPsuId = Objects.requireNonNull(fragment.getPsuId());
        PsuUnit psu = psuRepository.findById(fragmentPsuId)
                .orElseThrow(() -> new RuntimeException("PSU not found: " + fragmentPsuId));
        assertEditable(psu);
        validateBaseVersion(psu, baseVersionNo);

        // 已定版的Prompt（editable=false）所有人不可修改
        if (!fragment.getEditable()) {
            throw new RuntimeException("Prompt已定版，不允许修改");
        }
        
        // editable=true时，所有人可编辑
        fragment.setContent(content);
        
        PromptFragment savedFragment = promptFragmentRepository.save(fragment);
        
        // 编辑Prompt：版本号递增并切换预览标签
        bumpPromptVersion(psu);
        
        return savedFragment;
    }
    
    /**
     * 删除Prompt片段
     * @param fragmentId 片段ID
     * @param userId 用户ID
     */
    public void deletePromptFragment(Long fragmentId, Long userId) {
        RequestValidationUtils.requireNonNull(fragmentId, "fragmentId");
        Long safeFragmentId = Objects.requireNonNull(fragmentId);
        // 获取片段
        PromptFragment fragment = promptFragmentRepository.findById(safeFragmentId)
                .orElseThrow(() -> new RuntimeException("Prompt fragment not found: " + safeFragmentId));

        Long fragmentPsuId = Objects.requireNonNull(fragment.getPsuId());
        PsuUnit psu = psuRepository.findById(fragmentPsuId)
                .orElseThrow(() -> new RuntimeException("PSU not found: " + fragmentPsuId));
        assertEditable(psu);

        // 已定版的Prompt不能删除
        if (!fragment.getEditable()) {
            throw new RuntimeException("已定版的Prompt片段不允许删除");
        }
        
        promptFragmentRepository.deleteById(safeFragmentId);
        bumpPromptVersion(psu);
    }
    
    /**
     * 定版Prompt片段（仅运营可操作）
     * @param fragmentId 片段ID
     * @param userId 用户ID
     * @return 定版后的片段
     */
    public PromptFragment finalizePromptFragment(Long fragmentId, Long userId) {
        RequestValidationUtils.requireNonNull(fragmentId, "fragmentId");
        Long safeFragmentId = Objects.requireNonNull(fragmentId);
        // 获取片段
        PromptFragment fragment = promptFragmentRepository.findById(safeFragmentId)
                .orElseThrow(() -> new RuntimeException("Prompt fragment not found: " + safeFragmentId));

        Long fragmentPsuId = Objects.requireNonNull(fragment.getPsuId());
        PsuUnit psu = psuRepository.findById(fragmentPsuId)
                .orElseThrow(() -> new RuntimeException("PSU not found: " + fragmentPsuId));
        assertEditable(psu);

        // 如果已经定版，不允许重复定版
        if (!fragment.getEditable()) {
            throw new RuntimeException("Prompt已定版，无需重复操作");
        }
        
        // 定版：设置editable=false
        fragment.setEditable(false);
        
        PromptFragment saved = promptFragmentRepository.save(fragment);
        bumpPromptVersion(psu);
        return saved;
    }
    
    /**
     * 测试Prompt效果
     * @param psuId PSU ID
     * @param inputParams 输入参数
     * @return 测试结果
     */
    public PromptTestResponse testPrompt(Long psuId, Map<String, Object> inputParams) {
        RequestValidationUtils.requireNonNull(psuId, "psuId");
        RequestValidationUtils.requireNonNull(inputParams, "inputParams");
        long begin = System.currentTimeMillis();
        // 获取所有Prompt片段
        List<PromptFragment> fragments = getPromptFragments(psuId);

        // 组装完整Prompt并收集缺失变量，统一返回结构化数据
        RenderResult renderResult = assembleFullPromptWithMissingVars(fragments, inputParams);
        PromptTestResponse response = new PromptTestResponse();
        response.setRenderedPrompt(renderResult.renderedPrompt());
        response.setMissingVars(renderResult.missingVars().stream().toList());
        response.setLatencyMs((int) (System.currentTimeMillis() - begin));
        response.setTraceId(UUID.randomUUID().toString());
        return response;
    }
    
    /**
     * 组装完整Prompt
     * @param fragments 片段列表
     * @param variables 变量映射
     * @return 完整Prompt
     */
    public String assembleFullPrompt(List<PromptFragment> fragments, Map<String, Object> variables) {
        RequestValidationUtils.requireNonNull(fragments, "fragments");
        RequestValidationUtils.requireNonNull(variables, "variables");
        // 保持原有方法签名，对外返回渲染后的Prompt文本
        return assembleFullPromptWithMissingVars(fragments, variables).renderedPrompt();
    }

    private void assertEditable(PsuUnit psu) {
        if (psu.getStatus() == PsuStatus.ARCHIVED) {
            throw new RuntimeException("当前PSU已归档，不允许编辑");
        }
    }

    private void validateBaseVersion(PsuUnit psu, Integer baseVersionNo) {
        if (baseVersionNo == null) {
            throw new RuntimeException("baseVersionNo不能为空");
        }
        int current = psu.getVersionNo() == null ? 0 : psu.getVersionNo();
        if (baseVersionNo > current) {
            throw new RuntimeException("baseVersionNo不能大于当前版本");
        }
        if (current - baseVersionNo > 1) {
            throw new RuntimeException("版本已发生并发更新，请刷新后重试");
        }
    }

    private void bumpPromptVersion(PsuUnit psu) {
        psu.setVersionNo((psu.getVersionNo() == null ? 0 : psu.getVersionNo()) + 1);
        psuRepository.save(psu);
    }

    private RenderResult assembleFullPromptWithMissingVars(List<PromptFragment> fragments, Map<String, Object> variables) {
        // 按排序顺序组装，并在替换过程中收集缺失变量
        StringBuilder sb = new StringBuilder();
        Set<String> missingVars = new LinkedHashSet<>();
        List<PromptFragment> orderedFragments = fragments.stream()
            .sorted((a, b) -> Integer.compare(a.getSortOrder(), b.getSortOrder()))
            .collect(Collectors.toList());

        for (PromptFragment fragment : orderedFragments) {
            String renderedContent = renderFragment(fragment.getContent(), variables, missingVars);
            sb.append(renderedContent).append("\n");
        }
        return new RenderResult(sb.toString().trim(), missingVars);
    }

    private String renderFragment(String template, Map<String, Object> variables, Set<String> missingVars) {
        // 逐个解析占位符，支持a.b.c路径读取并记录缺失字段
        if (template == null || template.isBlank()) {
            return "";
        }
        StringBuilder rendered = new StringBuilder();
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("\\{\\{\\s*([^}]+?)\\s*\\}\\}").matcher(template);
        int lastIndex = 0;
        while (matcher.find()) {
            rendered.append(template, lastIndex, matcher.start());
            String path = matcher.group(1).trim();
            Object value = getValueByPath(variables, path);
            if (value == null) {
                missingVars.add(path);
            } else if (value instanceof Map<?, ?> || value instanceof List<?>) {
                rendered.append(String.valueOf(value));
            } else {
                rendered.append(value);
            }
            lastIndex = matcher.end();
        }
        rendered.append(template.substring(lastIndex));
        return rendered.toString();
    }

    private Object getValueByPath(Map<String, Object> source, String rawPath) {
        // 兼容a.b.c和数组索引路径读取（如items[0].name）
        if (source == null || rawPath == null || rawPath.isBlank()) {
            return null;
        }
        String normalized = rawPath.replace("[*]", ".0").replaceAll("\\[(\\d+)]", ".$1");
        String[] parts = normalized.split("\\.");
        Object current = source;
        for (String part : parts) {
            if (part == null || part.isBlank()) {
                continue;
            }
            if (current instanceof Map<?, ?> map) {
                current = map.get(part);
                continue;
            }
            if (current instanceof List<?> list) {
                try {
                    int index = Integer.parseInt(part);
                    current = index >= 0 && index < list.size() ? list.get(index) : null;
                } catch (NumberFormatException e) {
                    return null;
                }
                continue;
            }
            return null;
        }
        return current;
    }

    private record RenderResult(String renderedPrompt, Set<String> missingVars) {
    }
}


