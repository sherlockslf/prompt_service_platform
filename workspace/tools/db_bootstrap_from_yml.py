#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
从 backend/src/main/resources/application.yml 读取 MySQL 配置，
自动创建数据库并执行 schema.sql 完成表结构初始化。
"""

from pathlib import Path
from urllib.parse import urlparse
import os
import re
import shutil
import subprocess
import sys

try:
    import pymysql
except Exception:
    pymysql = None


def log_info(msg: str) -> None:
    print(f"[INFO] {msg}")


def log_ok(msg: str) -> None:
    print(f"[OK]   {msg}")


def log_err(msg: str) -> None:
    print(f"[ERR]  {msg}")


def yml_path() -> Path:
    # 定位到后端配置文件路径
    return Path(__file__).resolve().parents[1] / "backend" / "src" / "main" / "resources" / "application.yml"


def schema_sql_path() -> Path:
    # 定位到 schema.sql 路径
    return Path(__file__).resolve().parents[1] / "backend" / "src" / "main" / "resources" / "schema.sql"


def parse_jdbc_url(jdbc_url: str) -> dict:
    # 解析 JDBC URL，提取 host/port/database 信息
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
    # 从 application.yml 的 spring.datasource 块读取连接参数
    path = yml_path()
    if not path.exists():
        raise FileNotFoundError(f"未找到配置文件: {path}")
    text = path.read_text(encoding="utf-8")

    # 逐行扫描，定位未注释的 datasource/url/username/password
    in_datasource = False
    datasource_indent = None
    data: dict[str, str] = {}
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

        # 离开 datasource 块时结束读取
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
        "database": jdbc_info["database"],
        "user": data["username"],
        "password": data["password"],
        "charset": "utf8mb4",
    }


def split_sql_statements(sql_text: str) -> list[str]:
    # 去掉注释后按分号拆分 SQL 语句
    no_line_comments = re.sub(r"(?m)^\s*--.*$", "", sql_text)
    statements = [p.strip() for p in no_line_comments.split(";")]
    return [stmt for stmt in statements if stmt]


def mysql_cli_executable() -> str | None:
    # 查找本机 mysql 客户端命令
    return shutil.which("mysql")


def run_mysql_cli(conf: dict, sql_text: str, database: str | None = None) -> None:
    # 通过 mysql 命令执行 SQL，避免认证插件依赖问题
    mysql_bin = mysql_cli_executable()
    if not mysql_bin:
        raise RuntimeError("未找到 mysql 客户端命令，请安装 MySQL Client 或配置 PATH")

    cmd = [
        mysql_bin,
        "-h",
        str(conf["host"]),
        "-P",
        str(conf["port"]),
        "-u",
        str(conf["user"]),
        "--default-character-set=utf8mb4",
    ]
    if database:
        cmd.extend(["-D", str(database)])

    env = dict(**os.environ)
    env["MYSQL_PWD"] = str(conf["password"])
    result = subprocess.run(
        cmd,
        input=sql_text,
        text=True,
        capture_output=True,
        env=env,
    )
    if result.returncode != 0:
        err = (result.stderr or result.stdout or "").strip()
        raise RuntimeError(f"mysql 命令执行失败: {err}")


def create_database_if_not_exists(conf: dict) -> None:
    # 先连接 MySQL 实例，再确保目标库存在
    sql = (
        f"CREATE DATABASE IF NOT EXISTS `{conf['database']}` "
        "DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;"
    )
    if pymysql:
        try:
            conn = pymysql.connect(
                host=conf["host"],
                port=conf["port"],
                user=conf["user"],
                password=conf["password"],
                charset=conf["charset"],
                autocommit=True,
            )
            try:
                with conn.cursor() as cursor:
                    cursor.execute(sql)
            finally:
                conn.close()
            log_ok(f"数据库已就绪: {conf['database']}")
            return
        except Exception as ex:
            # 遇到 caching_sha2_password 等依赖问题时回退到 mysql CLI
            log_info(f"pymysql 建库失败，回退 mysql CLI: {ex}")

    run_mysql_cli(conf, sql)
    log_ok(f"数据库已就绪: {conf['database']}")


def init_schema(conf: dict) -> None:
    # 连接目标库并执行 schema.sql 完成建表
    sql_file = schema_sql_path()
    if not sql_file.exists():
        raise FileNotFoundError(f"未找到 schema.sql: {sql_file}")
    sql_text = sql_file.read_text(encoding="utf-8")
    statements = split_sql_statements(sql_text)
    if not statements:
        raise RuntimeError("schema.sql 中没有可执行语句")
    log_info(f"开始执行 schema.sql，共 {len(statements)} 条语句")

    if pymysql:
        try:
            conn = pymysql.connect(
                host=conf["host"],
                port=conf["port"],
                user=conf["user"],
                password=conf["password"],
                database=conf["database"],
                charset=conf["charset"],
            )
            try:
                with conn.cursor() as cursor:
                    for idx, stmt in enumerate(statements, start=1):
                        cursor.execute(stmt)
                        if idx % 20 == 0:
                            conn.commit()
                    conn.commit()

                    cursor.execute("SHOW TABLES LIKE 'ai_prompt\\_%'")
                    tables = [row[0] for row in cursor.fetchall()]
                    log_ok(f"初始化完成，当前 ai_prompt_* 表数量: {len(tables)}")
                return
            finally:
                conn.close()
        except Exception as ex:
            # 遇到认证依赖问题时回退到 mysql CLI
            log_info(f"pymysql 初始化失败，回退 mysql CLI: {ex}")

    run_mysql_cli(conf, sql_text, database=conf["database"])
    log_ok("初始化完成，schema.sql 已执行")


def main() -> None:
    # 串行执行配置解析、建库、建表流程
    conf = parse_mysql_config_from_yml()
    log_info(f"使用配置: {conf['host']}:{conf['port']}/{conf['database']} 用户={conf['user']}")
    create_database_if_not_exists(conf)
    init_schema(conf)


if __name__ == "__main__":
    try:
        main()
    except Exception as ex:
        log_err(str(ex))
        sys.exit(1)
