#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
PSU全生命周期管理平台 - 数据库完整初始化工具
功能：一次性完成所有表创建、版本字段更新、默认数据插入
适用于新库初始化，确保只需执行一次即可完成全部初始化工作
"""

import pymysql
import sys
from datetime import datetime

# 数据库连接配置
DB_CONFIG = {
    'host': 'rm-bp1v0aoc564umyqbp.mysql.rds.aliyuncs.com',
    'port': 3306,
    'user': 'aim_user',
    'password': 'Aim@2026',
    'database': 'aim_product_ai',
    'charset': 'utf8mb4'
}

# 预期表列表（12个）
EXPECTED_TABLES = [
    'ai_prompt_users',
    'ai_prompt_psu',
    'ai_prompt_json_schemas',
    'ai_prompt_prompt_fragments',
    'ai_prompt_version_reviews',
    'ai_prompt_system_configs',
    'ai_prompt_audit_logs',
    'ai_prompt_test_datasets',
    'ai_prompt_compositions',           # 编排草稿表
    'ai_prompt_composition_revisions',  # 编排版本快照表
    'ai_prompt_test_runs',              # 测试运行记录表
    'ai_prompt_test_run_items'          # 测试运行用例明细表
]

# 建表SQL语句
CREATE_TABLE_SQLS = [
    # 1. 用户表
    """
    CREATE TABLE IF NOT EXISTS ai_prompt_users (
        id BIGINT PRIMARY KEY AUTO_INCREMENT,
        username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
        password VARCHAR(100) NOT NULL COMMENT '明文密码',
        role ENUM('ADMIN', 'DEVELOPER', 'BUSINESS') NOT NULL COMMENT '角色',
        enabled TINYINT(1) DEFAULT 1 COMMENT '启用状态: 1-启用, 0-停用',
        phone_number VARCHAR(20) COMMENT '手机号码',
        created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
        updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
        INDEX idx_users_username (username),
        INDEX idx_users_role (role),
        INDEX idx_users_enabled (enabled)
    ) COMMENT 'AI Prompt用户表'
    """,
    
    # 2. PSU单元表（包含版本号字段）
    """
    CREATE TABLE IF NOT EXISTS ai_prompt_psu (
        id BIGINT PRIMARY KEY AUTO_INCREMENT,
        psu_id VARCHAR(100) NOT NULL UNIQUE COMMENT '全局唯一PSU ID',
        name VARCHAR(200) NOT NULL COMMENT 'PSU名称',
        description TEXT COMMENT '描述',
        status ENUM('ACTIVE', 'ARCHIVED') NOT NULL DEFAULT 'ACTIVE' COMMENT '状态',
        creator_id BIGINT NOT NULL COMMENT '创建者ID',
        major_version INT NOT NULL DEFAULT 0 COMMENT '主版本号',
        minor_version INT NOT NULL DEFAULT 0 COMMENT '次版本号',
        patch_version INT NOT NULL DEFAULT 0 COMMENT '修订版本号',
        created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
        updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
        INDEX idx_psu_units_psu_id (psu_id),
        INDEX idx_psu_units_status (status),
        INDEX idx_psu_units_creator (creator_id),
        INDEX idx_psu_units_created (created_at)
    ) COMMENT 'AI Prompt PSU单元表'
    """,
    
    # 3. JSON Schema表
    """
    CREATE TABLE IF NOT EXISTS ai_prompt_json_schemas (
        id BIGINT PRIMARY KEY AUTO_INCREMENT,
        psu_id BIGINT NOT NULL COMMENT '关联PSU ID',
        schema_content JSON NOT NULL COMMENT 'JSON Schema内容',
        version INT NOT NULL DEFAULT 1 COMMENT '版本号',
        modified_by BIGINT NOT NULL COMMENT '修改者ID',
        created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
        change_log TEXT COMMENT '变更日志',
        INDEX idx_json_schemas_psu_id (psu_id),
        INDEX idx_json_schemas_version (version),
        INDEX idx_json_schemas_modified_by (modified_by),
        INDEX idx_json_schemas_created (created_at),
        CONSTRAINT uk_json_schemas_psu_version UNIQUE (psu_id, version)
    ) COMMENT 'AI Prompt JSON Schema表'
    """,
    
    # 4. Prompt片段表
    """
    CREATE TABLE IF NOT EXISTS ai_prompt_prompt_fragments (
        id BIGINT PRIMARY KEY AUTO_INCREMENT,
        psu_id BIGINT NOT NULL COMMENT '关联PSU ID',
        fragment_key VARCHAR(100) NOT NULL COMMENT '片段标识',
        content TEXT NOT NULL COMMENT 'Prompt内容',
        editable TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否可编辑: 0-锁定, 1-可编辑',
        type ENUM('CORE_RULES', 'MESSAGE_TEMPLATE') NOT NULL COMMENT '类型',
        sort_order INT NOT NULL DEFAULT 0 COMMENT '排序',
        created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
        updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
        INDEX idx_prompt_fragments_psu_id (psu_id),
        INDEX idx_prompt_fragments_type (type),
        INDEX idx_prompt_fragments_editable (editable),
        INDEX idx_prompt_fragments_sort (sort_order),
        CONSTRAINT uk_prompt_fragments_psu_key UNIQUE (psu_id, fragment_key)
    ) COMMENT 'AI Prompt片段表'
    """,
    
    # 5. 版本审核表
    """
    CREATE TABLE IF NOT EXISTS ai_prompt_version_reviews (
        id BIGINT PRIMARY KEY AUTO_INCREMENT,
        psu_id BIGINT NOT NULL COMMENT '关联PSU ID',
        major_version INT NOT NULL DEFAULT 1 COMMENT '主版本',
        minor_version INT NOT NULL DEFAULT 0 COMMENT '次版本',
        patch_version INT NOT NULL DEFAULT 0 COMMENT '修订版本',
        status ENUM('PENDING', 'APPROVED', 'REJECTED') NOT NULL DEFAULT 'PENDING' COMMENT '状态',
        submitter_id BIGINT NOT NULL COMMENT '提交者ID',
        reviewer_id BIGINT COMMENT '审核者ID',
        rejection_reason TEXT COMMENT '驳回原因',
        submitted_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '提交时间',
        reviewed_at DATETIME COMMENT '审核时间',
        git_commit_hash VARCHAR(64) COMMENT 'Git提交哈希',
        code_content LONGTEXT COMMENT '生成的代码内容',
        created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
        updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
        INDEX idx_version_reviews_psu_id (psu_id),
        INDEX idx_version_reviews_status (status),
        INDEX idx_version_reviews_submitter (submitter_id),
        INDEX idx_version_reviews_reviewer (reviewer_id),
        INDEX idx_version_reviews_submitted (submitted_at),
        INDEX idx_version_reviews_git_hash (git_commit_hash),
        CONSTRAINT uk_version_reviews_version UNIQUE (psu_id, major_version, minor_version, patch_version)
    ) COMMENT 'AI Prompt版本审核表'
    """,
    
    # 6. 系统配置表
    """
    CREATE TABLE IF NOT EXISTS ai_prompt_system_configs (
        id BIGINT PRIMARY KEY AUTO_INCREMENT,
        config_key VARCHAR(100) NOT NULL UNIQUE COMMENT '配置键',
        config_value TEXT NOT NULL COMMENT '配置值（加密存储）',
        config_type ENUM('API_KEY', 'OTHER') NOT NULL DEFAULT 'OTHER' COMMENT '配置类型',
        created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
        updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
        INDEX idx_system_configs_key (config_key),
        INDEX idx_system_configs_type (config_type)
    ) COMMENT 'AI Prompt系统配置表（仅存储API密钥等核心配置）'
    """,
    
    # 7. 审计日志表
    """
    CREATE TABLE IF NOT EXISTS ai_prompt_audit_logs (
        id BIGINT PRIMARY KEY AUTO_INCREMENT,
        user_id BIGINT COMMENT '操作用户ID',
        username VARCHAR(50) NOT NULL COMMENT '操作用户名',
        operation VARCHAR(100) NOT NULL COMMENT '操作类型',
        targetType VARCHAR(50) NOT NULL COMMENT '目标类型',
        target_id BIGINT COMMENT '目标ID',
        details JSON COMMENT '操作详情',
        ip_address VARCHAR(45) COMMENT 'IP地址',
        created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
        INDEX idx_audit_logs_user_id (user_id),
        INDEX idx_audit_logs_operation (operation),
        INDEX idx_audit_logs_target_type (targetType),
        INDEX idx_audit_logs_target_id (target_id),
        INDEX idx_audit_logs_created (created_at)
    ) COMMENT 'AI Prompt审计日志表'
    """,
    
    # 8. 测试数据集表
    """
    CREATE TABLE IF NOT EXISTS ai_prompt_test_datasets (
        id BIGINT PRIMARY KEY AUTO_INCREMENT,
        psu_id BIGINT NOT NULL COMMENT '关联PSU ID',
        name VARCHAR(200) NOT NULL COMMENT '数据集名称',
        data_content TEXT NOT NULL COMMENT '测试数据内容（JSON格式）',
        description VARCHAR(500) COMMENT '描述',
        created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
        updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
        INDEX idx_test_datasets_psu_id (psu_id)
    ) COMMENT 'AI Prompt测试数据集表'
    """,
    
    # 9. 编排草稿表
    """
    CREATE TABLE IF NOT EXISTS ai_prompt_compositions (
        id BIGINT PRIMARY KEY AUTO_INCREMENT,
        psu_id BIGINT NOT NULL COMMENT '关联PSU ID',
        status ENUM('DRAFT', 'SUBMITTED', 'DEV_REVIEWING', 'APPROVED', 'REJECTED') NOT NULL DEFAULT 'DRAFT' COMMENT '状态',
        content LONGTEXT COMMENT '编辑器原始内容',
        spec_json LONGTEXT COMMENT '编排规格JSON',
        schema_version VARCHAR(50) NOT NULL DEFAULT '1' COMMENT '关联Schema版本',
        rejection_reason VARCHAR(500) COMMENT '驳回原因',
        rejection_type VARCHAR(20) COMMENT '驳回类型: BACK_TO_DEV/BACK_TO_BIZ',
        created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
        updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
        INDEX idx_compositions_psu_id (psu_id),
        INDEX idx_compositions_status (status),
        CONSTRAINT uk_compositions_psu_id UNIQUE (psu_id)
    ) COMMENT 'AI Prompt编排草稿表'
    """,
    
    # 10. 编排版本快照表
    """
    CREATE TABLE IF NOT EXISTS ai_prompt_composition_revisions (
        id BIGINT PRIMARY KEY AUTO_INCREMENT,
        composition_id BIGINT NOT NULL COMMENT '关联编排ID',
        revision_no INT NOT NULL COMMENT '版本号',
        psu_id BIGINT NOT NULL COMMENT '关联PSU ID',
        status ENUM('DRAFT', 'SUBMITTED', 'DEV_REVIEWING', 'APPROVED', 'REJECTED') NOT NULL COMMENT '状态',
        content LONGTEXT COMMENT '编排内容快照',
        spec_json LONGTEXT COMMENT '编排规格快照',
        schema_version VARCHAR(50) NOT NULL COMMENT 'Schema版本快照',
        created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
        INDEX idx_revisions_composition_id (composition_id),
        INDEX idx_revisions_revision_no (revision_no)
    ) COMMENT 'AI Prompt编排版本快照表'
    """,
    
    # 11. 测试运行记录表
    """
    CREATE TABLE IF NOT EXISTS ai_prompt_test_runs (
        id BIGINT PRIMARY KEY AUTO_INCREMENT,
        psu_id BIGINT NOT NULL COMMENT '关联PSU ID',
        dataset_id BIGINT NOT NULL COMMENT '关联测试集ID',
        composition_id BIGINT NOT NULL COMMENT '关联编排ID',
        total_cases INT NOT NULL DEFAULT 0 COMMENT '总用例数',
        success_cases INT NOT NULL DEFAULT 0 COMMENT '成功用例数',
        failed_cases INT NOT NULL DEFAULT 0 COMMENT '失败用例数',
        created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
        INDEX idx_test_runs_psu_id (psu_id),
        INDEX idx_test_runs_dataset_id (dataset_id)
    ) COMMENT 'AI Prompt测试运行记录表'
    """,
    
    # 12. 测试运行用例明细表
    """
    CREATE TABLE IF NOT EXISTS ai_prompt_test_run_items (
        id BIGINT PRIMARY KEY AUTO_INCREMENT,
        run_id BIGINT NOT NULL COMMENT '关联运行记录ID',
        case_id BIGINT NOT NULL COMMENT '用例ID',
        case_name VARCHAR(200) NOT NULL COMMENT '用例名称',
        input_json LONGTEXT COMMENT '输入参数JSON',
        rendered_prompt LONGTEXT COMMENT '渲染后的Prompt',
        model_output LONGTEXT COMMENT '模型输出',
        error_message TEXT COMMENT '错误信息',
        success TINYINT(1) NOT NULL COMMENT '是否成功',
        latency_ms BIGINT NOT NULL COMMENT '耗时(毫秒)',
        created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
        INDEX idx_run_items_run_id (run_id)
    ) COMMENT 'AI Prompt测试运行用例明细表'
    """
]

# 默认账号密码映射
DEFAULT_PASSWORDS = {
    'admin_user': 'Admin@123',
    'dev_user': 'Dev@123',
    'bus_user': 'Bus@123'
}

# 初始化数据SQL
INIT_DATA_SQLS = [
    # 初始化默认用户（3个角色各一个）
    """
    INSERT IGNORE INTO ai_prompt_users (username, password, role, enabled) 
    VALUES 
    ('admin_user', 'Admin@123', 'ADMIN', 1),
    ('dev_user', 'Dev@123', 'DEVELOPER', 1),
    ('bus_user', 'Bus@123', 'BUSINESS', 1)
    """,
    
    # 初始化默认系统配置
    """
    INSERT IGNORE INTO ai_prompt_system_configs (config_key, config_value, config_type) 
    VALUES ('default_api_key', 'encrypted_value_placeholder', 'API_KEY')
    """
]


def print_header():
    """打印工具头部信息"""
    print("=" * 70)
    print("PSU全生命周期管理平台 - 数据库完整初始化工具")
    print(f"执行时间: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
    print("=" * 70)


def print_success(msg):
    """打印成功信息"""
    print(f"  ✓ {msg}")


def print_error(msg):
    """打印错误信息"""
    print(f"  ✗ {msg}")


def print_info(msg):
    """打印普通信息"""
    print(f"  - {msg}")


def is_bcrypt_hash(password):
    """判断密码是否为BCrypt哈希值"""
    if not password:
        return False
    return str(password).startswith('$2a$') or str(password).startswith('$2b$') or str(password).startswith('$2y$')


def init_database():
    """执行数据库完整初始化操作"""
    print_header()
    
    try:
        # 连接数据库
        print("\n[步骤 1/4] 连接数据库...")
        connection = pymysql.connect(**DB_CONFIG)
        print_success(f"数据库连接成功: {DB_CONFIG['host']}:{DB_CONFIG['port']}/{DB_CONFIG['database']}")
        
        cursor = connection.cursor()
        
        # 创建所有表
        print("\n[步骤 2/4] 创建/验证数据表...")
        created_count = 0
        existed_count = 0
        
        for i, sql in enumerate(CREATE_TABLE_SQLS):
            table_name = EXPECTED_TABLES[i]
            try:
                # 检查表是否已存在
                cursor.execute("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = %s AND table_name = %s", 
                             (DB_CONFIG['database'], table_name))
                exists = cursor.fetchone()[0] > 0
                
                if exists:
                    print_info(f"表 {table_name} 已存在，跳过创建")
                    existed_count += 1
                else:
                    cursor.execute(sql)
                    connection.commit()
                    print_success(f"表 {table_name} 创建成功")
                    created_count += 1
            except Exception as e:
                print_error(f"表 {table_name} 操作失败: {str(e)}")
        
        print(f"\n  统计: 新建 {created_count} 个表, 已存在 {existed_count} 个表")
        
        # 检查并更新PSU表版本号字段（兼容已有表的情况）
        print("\n[步骤 2.5/4] 检查PSU表版本号字段...")
        cursor.execute("""
            SELECT COUNT(*) 
            FROM information_schema.COLUMNS 
            WHERE TABLE_SCHEMA = %s 
            AND TABLE_NAME = 'ai_prompt_psu' 
            AND COLUMN_NAME = 'major_version'
        """, (DB_CONFIG['database'],))
        
        major_version_exists = cursor.fetchone()[0] > 0
        
        if not major_version_exists:
            print_info("正在添加版本号字段到ai_prompt_psu表...")
            alter_sqls = [
                "ALTER TABLE ai_prompt_psu ADD COLUMN major_version INT NOT NULL DEFAULT 0 COMMENT '主版本号' AFTER creator_id",
                "ALTER TABLE ai_prompt_psu ADD COLUMN minor_version INT NOT NULL DEFAULT 0 COMMENT '次版本号' AFTER major_version",
                "ALTER TABLE ai_prompt_psu ADD COLUMN patch_version INT NOT NULL DEFAULT 0 COMMENT '修订版本号' AFTER minor_version"
            ]
            
            for sql in alter_sqls:
                try:
                    cursor.execute(sql)
                    connection.commit()
                    print_success(f"字段添加成功: {sql[:60]}...")
                except Exception as e:
                    if "Duplicate column" in str(e):
                        print_info(f"字段已存在，跳过: {sql[:60]}...")
                    else:
                        print_error(f"字段添加失败: {sql[:60]}... - {str(e)}")
        else:
            print_info("PSU表已包含版本号字段，无需更新")
        
        # 插入初始化数据
        print("\n[步骤 3/4] 插入默认初始化数据...")
        
        # 插入默认用户
        try:
            cursor.execute(INIT_DATA_SQLS[0])
            connection.commit()
            print_success("默认用户数据插入成功 (admin_user/dev_user/bus_user)")
        except Exception as e:
            print_error(f"默认用户数据插入失败: {str(e)}")
        
        # 插入默认系统配置
        try:
            cursor.execute(INIT_DATA_SQLS[1])
            connection.commit()
            print_success("默认系统配置插入成功 (default_api_key)")
        except Exception as e:
            print_error(f"默认系统配置插入失败: {str(e)}")
        
        # 更新密码为明文（兼容历史加密密码）
        print("\n[步骤 3.5/4] 检查并更新密码为明文...")
        try:
            cursor.execute("SELECT id, username, password FROM ai_prompt_users")
            users = cursor.fetchall()
            
            updated_count = 0
            skipped_count = 0
            
            for user_id, username, current_password in users:
                try:
                    # 判断是否为加密密码
                    if is_bcrypt_hash(current_password):
                        # 是加密密码，需要更新
                        if username in DEFAULT_PASSWORDS:
                            new_password = DEFAULT_PASSWORDS[username]
                            cursor.execute(
                                "UPDATE ai_prompt_users SET password = %s WHERE id = %s",
                                (new_password, user_id)
                            )
                            connection.commit()
                            print_success(f"用户 {username}: 已更新为明文密码")
                            updated_count += 1
                        else:
                            print_info(f"用户 {username}: 非默认账号，跳过更新")
                            skipped_count += 1
                    else:
                        print_info(f"用户 {username}: 已是明文密码，跳过")
                        skipped_count += 1
                except Exception as e:
                    print_error(f"用户 {username} 更新失败: {str(e)}")
            
            print(f"\n  密码更新统计: 已更新 {updated_count} 个, 已跳过 {skipped_count} 个")
        except Exception as e:
            print_error(f"密码更新检查失败: {str(e)}")
        
        # 验证所有表
        print("\n[步骤 4/4] 验证数据库初始化结果...")
        print("\n" + "=" * 70)
        print("数据库表验证结果:")
        print("-" * 70)
        
        cursor.execute("SHOW TABLES LIKE 'ai_prompt_%'")
        tables = cursor.fetchall()
        existing_tables = [t[0] for t in tables]
        
        # 显示每个表的状态和数据量
        for table_name in EXPECTED_TABLES:
            if table_name in existing_tables:
                cursor.execute(f"SELECT COUNT(*) FROM {table_name}")
                count = cursor.fetchone()[0]
                print_success(f"{table_name}: {count} 条记录")
            else:
                print_error(f"{table_name}: 不存在")
        
        # 检查是否有缺失的表
        missing_tables = [t for t in EXPECTED_TABLES if t not in existing_tables]
        
        print("\n" + "=" * 70)
        if not missing_tables:
            print("✓ 数据库初始化完成！所有12个表均已创建并初始化。")
        else:
            print(f"✗ 以下表缺失: {', '.join(missing_tables)}")
        print("=" * 70)
        
        # 显示默认账号信息
        print("\n默认登录账号信息:")
        print("-" * 70)
        print("  管理员: admin_user / Admin@123")
        print("  研发:   dev_user   / Dev@123")
        print("  业务:   bus_user   / Bus@123")
        print("-" * 70)
        
        cursor.close()
        connection.close()
        
        print("\n✓ 数据库完整初始化完成！")
        
    except pymysql.Error as e:
        print(f"\n数据库错误: {str(e)}")
        sys.exit(1)
    except Exception as e:
        print(f"\n未知错误: {str(e)}")
        sys.exit(1)


if __name__ == '__main__':
    init_database()