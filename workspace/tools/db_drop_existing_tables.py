#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
删除库中已有表
说明：仅删除 ai_prompt_* 前缀表，避免误删其他业务表
"""

import sys
import os
import re
from pathlib import Path
from urllib.parse import urlparse
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
    # 优先使用环境变量，便于临时覆盖
    if not missing:
        return {
            "host": host,
            "port": int(port),
            "user": user,
            "password": password,
            "database": database,
            "charset": "utf8mb4",
        }

    # 环境变量不完整时，自动从 application.yml 读取
    return parse_mysql_config_from_yml()


def yml_path() -> Path:
    # tools -> backend/src/main/resources/application.yml
    return Path(__file__).resolve().parents[1] / "backend" / "src" / "main" / "resources" / "application.yml"


def parse_jdbc_url(jdbc_url: str) -> dict:
    # 解析 JDBC URL，提取 host/port/database
    if not jdbc_url.startswith("jdbc:mysql://"):
        raise RuntimeError(f"仅支持 MySQL JDBC URL，当前为: {jdbc_url}")
    no_prefix = jdbc_url[len("jdbc:"):]
    parsed = urlparse(no_prefix)
    db_name = parsed.path.lstrip("/")
    if not parsed.hostname or not db_name:
        raise RuntimeError(f"JDBC URL 解析失败: {jdbc_url}")
    return {
        "host": parsed.hostname,
        "port": parsed.port or 3306,
        "database": db_name,
    }


def parse_mysql_config_from_yml() -> dict:
    # 从 application.yml 的 datasource 块读取数据库配置
    path = yml_path()
    if not path.exists():
        raise FileNotFoundError(f"未找到配置文件: {path}")
    text = path.read_text(encoding="utf-8")

    in_datasource = False
    datasource_indent = None
    data = {}
    for raw_line in text.splitlines():
        if not raw_line.strip():
            continue
        if raw_line.lstrip().startswith("#"):
            continue

        indent = len(raw_line) - len(raw_line.lstrip(" "))
        line = raw_line.strip()

        if re.match(r"^datasource\s*:\s*$", line):
            in_datasource = True
            datasource_indent = indent
            continue

        if in_datasource and indent <= (datasource_indent or 0):
            in_datasource = False

        if in_datasource and ":" in line:
            key, value = line.split(":", 1)
            key = key.strip()
            value = value.strip().strip("'\"")
            if key in {"url", "username", "password"}:
                data[key] = value

    missing = [k for k in ("url", "username", "password") if not data.get(k)]
    if missing:
        raise RuntimeError(f"application.yml 缺少 datasource 配置项: {', '.join(missing)}")

    jdbc_info = parse_jdbc_url(data["url"])
    return {
        "host": jdbc_info["host"],
        "port": int(jdbc_info["port"]),
        "user": data["username"],
        "password": data["password"],
        "database": jdbc_info["database"],
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
