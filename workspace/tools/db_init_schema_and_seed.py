#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
初始化表结构并初始化原始数据
说明：按 backend/src/main/resources/schema.sql 执行全部 SQL 语句
"""

from pathlib import Path
import re
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


def schema_sql_path() -> Path:
    # tools -> backend/src/main/resources/schema.sql
    return Path(__file__).resolve().parents[1] / "backend" / "src" / "main" / "resources" / "schema.sql"


def split_sql_statements(sql_text: str) -> list[str]:
    # 去除整行注释，避免影响 SQL 拆分
    no_comments = re.sub(r"(?m)^\s*--.*$", "", sql_text)
    parts = [p.strip() for p in no_comments.split(";")]
    return [p for p in parts if p]


def init_schema_and_seed() -> None:
    sql_file = schema_sql_path()
    if not sql_file.exists():
        raise FileNotFoundError(f"未找到 schema.sql: {sql_file}")

    sql_text = sql_file.read_text(encoding="utf-8")
    statements = split_sql_statements(sql_text)
    if not statements:
        raise RuntimeError("schema.sql 中没有可执行语句")

    db_config = load_db_config()
    conn = pymysql.connect(**db_config)
    cursor = conn.cursor()
    try:
        log_info(f"连接数据库成功: {db_config['host']}:{db_config['port']}/{db_config['database']}")
        log_info(f"开始执行 schema.sql，共 {len(statements)} 条语句")

        # 顺序执行 schema.sql，确保建表与初始化数据一致
        for idx, stmt in enumerate(statements, start=1):
            cursor.execute(stmt)
            if idx % 20 == 0:
                conn.commit()
        conn.commit()

        cursor.execute("SHOW TABLES LIKE 'ai_prompt\\_%'")
        tables = [row[0] for row in cursor.fetchall()]
        log_ok(f"初始化完成，当前 ai_prompt_* 表数量: {len(tables)}")
    finally:
        cursor.close()
        conn.close()


if __name__ == "__main__":
    try:
        init_schema_and_seed()
    except Exception as ex:
        log_err(str(ex))
        sys.exit(1)
