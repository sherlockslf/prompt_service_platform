package com.example.psu.service;

import com.example.psu.dto.request.CreatePromptRequest;
import com.example.psu.entity.PromptFragment;
import com.example.psu.entity.PsuUnit;
import com.example.psu.enums.PsuStatus;
import com.example.psu.exception.RequestValidationUtils;
import com.example.psu.repository.PromptFragmentRepository;
import com.example.psu.repository.PsuRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Prompt管理服务
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
        assertDraftEditable(psu);
        
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
        
        return promptFragmentRepository.save(fragment);
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
    public PromptFragment updatePromptFragment(Long fragmentId, String content, Long userId) {
        RequestValidationUtils.requireNonNull(fragmentId, "fragmentId");
        RequestValidationUtils.requireNonBlank(content, "content");
        Long safeFragmentId = Objects.requireNonNull(fragmentId);
        // 获取片段
        PromptFragment fragment = promptFragmentRepository.findById(safeFragmentId)
                .orElseThrow(() -> new RuntimeException("Prompt fragment not found: " + safeFragmentId));

        Long fragmentPsuId = Objects.requireNonNull(fragment.getPsuId());
        PsuUnit psu = psuRepository.findById(fragmentPsuId)
                .orElseThrow(() -> new RuntimeException("PSU not found: " + fragmentPsuId));
        assertDraftEditable(psu);

        // 已定版的Prompt（editable=false）所有人不可修改
        if (!fragment.getEditable()) {
            throw new RuntimeException("Prompt已定版，不允许修改");
        }
        
        // editable=true时，所有人可编辑
        fragment.setContent(content);
        
        PromptFragment savedFragment = promptFragmentRepository.save(fragment);
        
        // 更新PSU版本号（单字段递增）
        // 获取对应的PSU并更新版本
        psu.setVersionNo(psu.getVersionNo() + 1); // 版本号递增
        psuRepository.save(psu);
        
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
        assertDraftEditable(psu);

        // 已定版的Prompt不能删除
        if (!fragment.getEditable()) {
            throw new RuntimeException("已定版的Prompt片段不允许删除");
        }
        
        promptFragmentRepository.deleteById(safeFragmentId);
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
        assertDraftEditable(psu);

        // 如果已经定版，不允许重复定版
        if (!fragment.getEditable()) {
            throw new RuntimeException("Prompt已定版，无需重复操作");
        }
        
        // 定版：设置editable=false
        fragment.setEditable(false);
        
        return promptFragmentRepository.save(fragment);
    }
    
    /**
     * 测试Prompt效果
     * @param psuId PSU ID
     * @param inputParams 输入参数
     * @return 测试结果
     */
    public String testPrompt(Long psuId, Map<String, Object> inputParams) {
        RequestValidationUtils.requireNonNull(psuId, "psuId");
        RequestValidationUtils.requireNonNull(inputParams, "inputParams");
        // 获取所有Prompt片段
        List<PromptFragment> fragments = getPromptFragments(psuId);
        
        // 组装完整Prompt
        String fullPrompt = assembleFullPrompt(fragments, inputParams);
        
        // 这里应该调用大模型API进行测试
        // 为了演示，我们返回组装后的Prompt
        return "Test result for prompt: " + fullPrompt;
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
        StringBuilder sb = new StringBuilder();
        
        // 按排序顺序组装
        List<PromptFragment> orderedFragments = fragments.stream()
                .sorted((a, b) -> Integer.compare(a.getSortOrder(), b.getSortOrder()))
                .collect(Collectors.toList());
        
        for (PromptFragment fragment : orderedFragments) {
            String content = fragment.getContent();
            
            // 替换变量（简单实现）
            for (Map.Entry<String, Object> entry : variables.entrySet()) {
                content = content.replace("{{" + entry.getKey() + "}}", entry.getValue().toString());
            }
            
            sb.append(content).append("\n");
        }
        
        return sb.toString().trim();
    }

    private void assertDraftEditable(PsuUnit psu) {
        // 生命周期约束：仅草稿允许编辑Prompt内容
        if (psu.getStatus() != PsuStatus.DRAFT) {
            throw new RuntimeException("当前PSU为只读状态，仅草稿可编辑");
        }
    }
}


