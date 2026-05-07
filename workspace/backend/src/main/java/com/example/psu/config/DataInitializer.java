package com.example.psu.config;

import com.example.psu.entity.PromptFragment;
import com.example.psu.entity.PromptComposition;
import com.example.psu.entity.PsuUnit;
import com.example.psu.entity.User;
import com.example.psu.entity.VersionReview;
import com.example.psu.entity.JsonSchema;
import com.example.psu.enums.CompositionStatus;
import com.example.psu.enums.FragmentType;
import com.example.psu.enums.ReviewStatus;
import com.example.psu.enums.UserRole;
import com.example.psu.repository.PromptFragmentRepository;
import com.example.psu.repository.PromptCompositionRepository;
import com.example.psu.repository.PsuRepository;
import com.example.psu.repository.UserRepository;
import com.example.psu.repository.VersionReviewRepository;
import com.example.psu.repository.JsonSchemaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 数据初始化器 - 确保系统中有默认用户和Prompt片段
 */
@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PsuRepository psuRepository;
    
    @Autowired
    private PromptFragmentRepository promptFragmentRepository;
    
    @Autowired
    private PromptCompositionRepository promptCompositionRepository;
    
    @Autowired
    private VersionReviewRepository versionReviewRepository;
    
    @Autowired
    private JsonSchemaRepository jsonSchemaRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        // 启动时统一对齐默认账号，避免历史库里残留错误密码导致无法登录
        ensureDefaultUser("admin_user", "Admin@123", UserRole.ADMIN);
        ensureDefaultUser("dev_user", "Dev@123", UserRole.DEVELOPER);
        ensureDefaultUser("bus_user", "Bus@123", UserRole.BUSINESS);
        
        // 初始化默认Prompt片段
        ensureDefaultPromptFragments();
        
        // 对历史PSU做状态机回填，确保编排状态与审核记录对齐
        ensureCompositionStateAligned();
    }

    private void ensureDefaultUser(String username, String rawPassword, UserRole role) {
        // 查询默认账号是否已存在
        Optional<User> existingOpt = userRepository.findByUsername(username);
        String encodedPassword = passwordEncoder.encode(rawPassword);
        if (existingOpt.isEmpty()) {
            // 不存在则直接创建默认账号
            User user = new User();
            user.setUsername(username);
            // 启动初始化时统一写入加密密码。
            user.setPassword(encodedPassword);
            user.setRole(role);
            user.setEnabled(true);
            userRepository.save(user);
            System.out.println("已创建默认用户: " + username);
            return;
        }

        // 存在则校验关键字段，不一致时自动修正
        User existing = existingOpt.get();
        boolean needUpdate = false;

        // 密码不匹配时自动修正为最新默认口令的加密值。
        if (existing.getPassword() == null || !passwordEncoder.matches(rawPassword, existing.getPassword())) {
            existing.setPassword(encodedPassword);
            needUpdate = true;
        }
        if (existing.getRole() != role) {
            existing.setRole(role);
            needUpdate = true;
        }
        if (!Boolean.TRUE.equals(existing.getEnabled())) {
            existing.setEnabled(true);
            needUpdate = true;
        }

        if (needUpdate) {
            userRepository.save(existing);
            System.out.println("已修正默认用户: " + username);
        }
    }
    
    /**
     * 确保每个PSU都有默认的Prompt片段
     */
    private void ensureDefaultPromptFragments() {
        // 获取所有PSU
        java.util.List<PsuUnit> allPsus = psuRepository.findAll();
        
        for (PsuUnit psu : allPsus) {
            // 检查是否已有core_rules片段
            Optional<PromptFragment> existingOpt = promptFragmentRepository.findByPsuIdAndFragmentKey(
                psu.getId(), "core_rules");
            
            if (existingOpt.isEmpty()) {
                // 创建默认Prompt片段
                PromptFragment fragment = new PromptFragment();
                fragment.setPsuId(psu.getId());
                fragment.setFragmentKey("core_rules");
                fragment.setContent("你是一个专业的购物助手，请根据用户需求推荐商品。");
                fragment.setEditable(true);
                fragment.setType(FragmentType.CORE_RULES);
                fragment.setSortOrder(1);
                fragment.setCreatedAt(LocalDateTime.now());
                fragment.setUpdatedAt(LocalDateTime.now());
                promptFragmentRepository.save(fragment);
                System.out.println("已为PSU " + psu.getName() + " 创建默认Prompt片段");
            }
        }
    }
    
    /**
     * 历史数据状态回填：
     * 1) 确保每个PSU都有Composition草稿
     * 2) 按最新VersionReview状态对齐Composition状态
     */
    private void ensureCompositionStateAligned() {
        java.util.List<PsuUnit> allPsus = psuRepository.findAll();
        for (PsuUnit psu : allPsus) {
            PromptComposition composition = promptCompositionRepository.findByPsuId(psu.getId()).orElseGet(() -> {
                PromptComposition created = new PromptComposition();
                created.setPsuId(psu.getId());
                created.setSchemaVersion(resolveSchemaVersion(psu.getId()));
                created.setStatus(CompositionStatus.DRAFT);
                created.setContent("");
                created.setSpecJson("{}");
                created.setCreatedBy(psu.getCreatorId() == null ? 0L : psu.getCreatorId());
                created.setUpdatedBy(psu.getCreatorId() == null ? 0L : psu.getCreatorId());
                return promptCompositionRepository.save(created);
            });

            boolean needUpdate = false;
            if (composition.getSchemaVersion() == null || composition.getSchemaVersion() <= 0) {
                composition.setSchemaVersion(resolveSchemaVersion(psu.getId()));
                needUpdate = true;
            }

            java.util.Optional<VersionReview> latestReviewOpt = versionReviewRepository
                .findByPsuIdOrderBySubmittedAtDesc(psu.getId())
                .stream()
                .findFirst();

            if (latestReviewOpt.isPresent()) {
                VersionReview latestReview = latestReviewOpt.get();
                CompositionStatus targetStatus = mapReviewToCompositionStatus(latestReview);
                if (composition.getStatus() != targetStatus) {
                    composition.setStatus(targetStatus);
                    needUpdate = true;
                }
                if (latestReview.getStatus() == ReviewStatus.ARCHIVED) {
                    composition.setRejectionReason(latestReview.getRejectionReason());
                    composition.setRejectionType(latestReview.getRejectionType());
                    needUpdate = true;
                }
            }
            if (needUpdate) {
                if (composition.getUpdatedBy() == null) {
                    composition.setUpdatedBy(psu.getCreatorId() == null ? 0L : psu.getCreatorId());
                }
                promptCompositionRepository.save(composition);
            }
        }
    }
    
    private Integer resolveSchemaVersion(Long psuId) {
        return jsonSchemaRepository.findTopByPsuIdOrderByVersionDesc(psuId)
            .map(JsonSchema::getVersion)
            .orElse(1);
    }
    
    private CompositionStatus mapReviewToCompositionStatus(VersionReview review) {
        if (review.getStatus() == ReviewStatus.FORMAL) {
            return CompositionStatus.FORMAL;
        }
        if (review.getStatus() == ReviewStatus.CANDIDATE) {
            return CompositionStatus.CANDIDATE;
        }
        if (review.getStatus() == ReviewStatus.ARCHIVED) {
            return CompositionStatus.ARCHIVED;
        }
        return CompositionStatus.DRAFT;
    }
}
