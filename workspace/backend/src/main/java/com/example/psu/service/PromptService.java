package com.example.psu.service;

import com.example.psu.dto.request.CreatePromptRequest;
import com.example.psu.entity.PromptFragment;
import com.example.psu.entity.PsuUnit;
import com.example.psu.repository.PromptFragmentRepository;
import com.example.psu.repository.PsuRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
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
        // 检查PSU是否存在
        PsuUnit psu = psuRepository.findById(psuId)
                .orElseThrow(() -> new RuntimeException("PSU not found: " + psuId));
        
        return promptFragmentRepository.findByPsuIdOrderBySortOrderAsc(psuId);
    }
    
    /**
     * 创建Prompt片段
     * @param request 创建请求
     * @param userId 用户ID
     * @return 创建的片段
     */
    public PromptFragment createPromptFragment(CreatePromptRequest request, Long userId) {
        // 检查PSU是否存在
        psuRepository.findById(request.getPsuId())
                .orElseThrow(() -> new RuntimeException("PSU not found: " + request.getPsuId()));
        
        // 检查fragmentKey是否已存在
        Optional<PromptFragment> existingFragment = promptFragmentRepository
                .findByPsuIdAndFragmentKey(request.getPsuId(), request.getFragmentKey());
        if (existingFragment.isPresent()) {
            throw new RuntimeException("该PSU下已存在相同标识的Prompt片段: " + request.getFragmentKey());
        }
        
        // 创建新的Prompt片段
        PromptFragment fragment = new PromptFragment();
        fragment.setPsuId(request.getPsuId());
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
        // 获取片段
        PromptFragment fragment = promptFragmentRepository.findById(fragmentId)
                .orElseThrow(() -> new RuntimeException("Prompt fragment not found: " + fragmentId));

        // 已定版的Prompt（editable=false）所有人不可修改
        if (!fragment.getEditable()) {
            throw new RuntimeException("Prompt已定版，不允许修改");
        }
        
        // editable=true时，所有人可编辑
        fragment.setContent(content);
        
        PromptFragment savedFragment = promptFragmentRepository.save(fragment);
        
        // 更新PSU版本号（单字段递增）
        // 获取对应的PSU并更新版本
        PsuUnit psu = psuRepository.findById(fragment.getPsuId())
                .orElseThrow(() -> new RuntimeException("PSU not found: " + fragment.getPsuId()));
        
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
        // 获取片段
        PromptFragment fragment = promptFragmentRepository.findById(fragmentId)
                .orElseThrow(() -> new RuntimeException("Prompt fragment not found: " + fragmentId));

        // 已定版的Prompt不能删除
        if (!fragment.getEditable()) {
            throw new RuntimeException("已定版的Prompt片段不允许删除");
        }
        
        promptFragmentRepository.deleteById(fragmentId);
    }
    
    /**
     * 定版Prompt片段（仅运营可操作）
     * @param fragmentId 片段ID
     * @param userId 用户ID
     * @return 定版后的片段
     */
    public PromptFragment finalizePromptFragment(Long fragmentId, Long userId) {
        // 获取片段
        PromptFragment fragment = promptFragmentRepository.findById(fragmentId)
                .orElseThrow(() -> new RuntimeException("Prompt fragment not found: " + fragmentId));

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
}
