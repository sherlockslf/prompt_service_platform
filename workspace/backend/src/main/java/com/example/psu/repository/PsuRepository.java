package com.example.psu.repository;

import com.example.psu.entity.PsuUnit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * PSU仓库接口
 */
@Repository
public interface PsuRepository extends JpaRepository<PsuUnit, Long> {
    
    /**
     * 根据PSU ID查找PSU
     * @param psuId PSU ID
     * @return PSU实体
     */
    Optional<PsuUnit> findByPsuId(String psuId);
    
    /**
     * 检查PSU ID是否存在
     * @param psuId PSU ID
     * @return 是否存在
     */
    boolean existsByPsuId(String psuId);
    
    /**
     * 分页查询PSU列表，按更新时间倒序
     * @param pageable 分页参数
     * @return 分页结果
     */
    Page<PsuUnit> findAllByOrderByUpdatedAtDesc(Pageable pageable);

    /**
     * 按名称模糊查询PSU列表（忽略大小写），按更新时间倒序
     * @param name 名称关键字
     * @param pageable 分页参数
     * @return 分页结果
     */
    Page<PsuUnit> findByNameContainingIgnoreCaseOrderByUpdatedAtDesc(String name, Pageable pageable);
}
