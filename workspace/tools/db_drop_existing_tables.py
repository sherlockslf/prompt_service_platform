#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
删除库中已有表
说明：仅删除 ai_prompt_* 前缀表，避免误删其他业务表
"""

import sys
import os
import pymysql

def load_db_config() -> dict:
    host = os.getenv("PSU_DB_HOST")
    port = os.getenv("PSU_DB_PORT")
    user = os.getenv("PSU_DB_USER")
    password = os.getenv("PSU_DB_PASSWORD")
    database = os.getenv("PSU_DB_NAME")
    missing = [k for k, v in {
        "PSU_DB_HOST": host,
        "PSU_DB_PORT": port,
        "PSU_DB_USER": user,
        "PSU_DB_PASSWORD": password,
        "PSU_DB_NAME": database,
    }.items() if not v]
    if missing:
        raise RuntimeError(f"缺少数据库环境变量: {', '.join(missing)}")
    return {
        "host": host,
        "port": int(port),
        "user": user,
        "password": password,
        "database": database,
        "charset": "utf8mb4",
    }


def log_info(msg: str) -> None:
    print(f"[INFO] {msg}")


def log_ok(msg: str) -> None:
    print(f"[OK]   {msg}")


def log_err(msg: str) -> None:
    print(f"[ERR]  {msg}")


def drop_existing_tables() -> None:
    db_config = load_db_config()
    conn = pymysql.connect(**db_config)
    cursor = conn.cursor()
    try:
        log_info(f"连接数据库成功: {db_config['host']}:{db_config['port']}/{db_config['database']}")

        cursor.execute("SHOW TABLES LIKE 'ai_prompt\\_%'")
        tables = [row[0] for row in cursor.fetchall()]
        if not tables:
            log_info("未发现 ai_prompt_* 表，无需删除")
            return

        # 关闭外键检查，避免删除顺序导致失败
        cursor.execute("SET FOREIGN_KEY_CHECKS = 0")
        for table in tables:
            cursor.execute(f"DROP TABLE IF EXISTS `{table}`")
            log_ok(f"已删除: {table}")
        cursor.execute("SET FOREIGN_KEY_CHECKS = 1")
        conn.commit()
        log_ok(f"删除完成，共删除 {len(tables)} 张表")
    finally:
        cursor.close()
        conn.close()


if __name__ == "__main__":
    try:
        drop_existing_tables()
    except Exception as ex:
        log_err(str(ex))
        sys.exit(1)
