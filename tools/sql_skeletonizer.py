#!/usr/bin/env python3
"""
sql_skeletonizer.py - Compact summaries of Flyway migrations for LoanApp.

Instead of pasting full V*__*.sql files into context, this extracts just the
schema-relevant facts: tables, columns (name/type/nullable/default), primary
keys, foreign keys, indexes and constraints. Useful to review migrations for
missing indexes, wrong types or missing constraints (M9) without spending
tokens on comment blocks / formatting.

Examples:
  python tools/sql_skeletonizer.py backend/src/main/resources/db/migration/V1__create_loans_table.sql
  python tools/sql_skeletonizer.py --dir backend/src/main/resources/db/migration
  python tools/sql_skeletonizer.py --dir backend/src/main/resources/db/migration --check
"""

from __future__ import annotations

import argparse
import re
import sys
from pathlib import Path
from typing import List


CREATE_TABLE_RE = re.compile(r'CREATE\s+TABLE\s+(?:IF\s+NOT\s+EXISTS\s+)?["`]?(\w+)["`]?\s*\(', re.IGNORECASE)
ALTER_TABLE_RE = re.compile(r'ALTER\s+TABLE\s+["`]?(\w+)["`]?', re.IGNORECASE)
CREATE_INDEX_RE = re.compile(
    r'CREATE\s+(UNIQUE\s+)?INDEX\s+["`]?(\w+)["`]?\s+ON\s+["`]?(\w+)["`]?\s*\(([^)]+)\)', re.IGNORECASE
)
COLUMN_LINE_RE = re.compile(
    r'^\s*["`]?(\w+)["`]?\s+([A-Za-z][\w()]*(?:\s+PRECISION)?(?:\([\d,\s]+\))?)\s*(.*?),?\s*$'
)
CONSTRAINT_KEYWORDS = ("PRIMARY KEY", "FOREIGN KEY", "CONSTRAINT", "UNIQUE", "CHECK")

# Types that warrant a warning for money/precision-sensitive columns.
IMPRECISE_MONEY_TYPES = {"double", "double precision", "float", "real"}
NON_NUMERIC_MONEY_TYPES_PREFIXES = ("varchar", "char", "text", "clob")
MONEY_NAME_HINTS = ("amount", "monto", "price", "precio", "balance", "saldo", "rate", "tasa", "total", "income", "ingreso", "installment", "cuota")


def strip_sql_comments(text: str) -> str:
    text = re.sub(r"--.*?$", "", text, flags=re.MULTILINE)
    text = re.sub(r"/\*.*?\*/", "", text, flags=re.DOTALL)
    return text


def split_statements(text: str) -> List[str]:
    return [s.strip() for s in text.split(";") if s.strip()]


def parse_create_table(stmt: str) -> dict:
    match = CREATE_TABLE_RE.search(stmt)
    if not match:
        return {}
    table_name = match.group(1)
    body_start = stmt.find("(", match.end() - 1)
    depth = 0
    body_end = len(stmt)
    for i in range(body_start, len(stmt)):
        if stmt[i] == "(":
            depth += 1
        elif stmt[i] == ")":
            depth -= 1
            if depth == 0:
                body_end = i
                break
    body = stmt[body_start + 1 : body_end]

    entries: List[str] = []
    depth = 0
    current = ""
    for ch in body:
        if ch == "(":
            depth += 1
        elif ch == ")":
            depth -= 1
        if ch == "," and depth == 0:
            entries.append(current.strip())
            current = ""
        else:
            current += ch
    if current.strip():
        entries.append(current.strip())

    columns = []
    constraints = []
    for entry in entries:
        upper = entry.upper().strip()
        if any(upper.startswith(kw) for kw in CONSTRAINT_KEYWORDS):
            constraints.append(re.sub(r"\s+", " ", entry.strip()))
            continue
        col_match = COLUMN_LINE_RE.match(entry.strip())
        if col_match:
            name, col_type, rest = col_match.groups()
            flags = []
            rest_upper = rest.upper()
            if "NOT NULL" in rest_upper:
                flags.append("NOT NULL")
            if "DEFAULT" in rest_upper:
                default_match = re.search(r"DEFAULT\s+([^\s,]+)", rest, re.IGNORECASE)
                flags.append(f"DEFAULT {default_match.group(1)}" if default_match else "DEFAULT")
            if "PRIMARY KEY" in rest_upper:
                flags.append("PK")
            if "UNIQUE" in rest_upper:
                flags.append("UNIQUE")
            if "REFERENCES" in rest_upper:
                ref_match = re.search(r"REFERENCES\s+(\w+)", rest, re.IGNORECASE)
                flags.append(f"FK->{ref_match.group(1)}" if ref_match else "FK")
            columns.append({"name": name, "type": col_type.strip(), "flags": flags})

    return {"table": table_name, "columns": columns, "constraints": constraints}


def parse_alter_table(stmt: str) -> dict:
    match = ALTER_TABLE_RE.search(stmt)
    if not match:
        return {}
    return {"table": match.group(1), "raw": re.sub(r"\s+", " ", stmt.strip())}


def parse_create_index(stmt: str) -> dict:
    match = CREATE_INDEX_RE.search(stmt)
    if not match:
        return {}
    unique, index_name, table_name, cols = match.groups()
    return {
        "unique": bool(unique),
        "index": index_name,
        "table": table_name,
        "columns": [c.strip() for c in cols.split(",")],
    }


def check_issues(tables: List[dict], indexes: List[dict], alters: List[dict]) -> List[str]:
    """Heuristic checks aligned with the M9 intentional problem (missing indexes,
    wrong types, missing constraints)."""
    issues: List[str] = []
    indexed_cols = {}
    for idx in indexes:
        indexed_cols.setdefault(idx["table"], set()).update(
            c.strip().lower() for c in idx["columns"]
        )

    for table in tables:
        table_name = table["table"]
        fk_cols = set()
        for col in table["columns"]:
            flags_str = " ".join(col["flags"])
            col_name = col["name"]
            col_type = col["type"].lower()

            if "FK->" in flags_str and col_name.lower() not in indexed_cols.get(table_name, set()):
                fk_cols.add(col_name)

            is_money_name = any(h in col_name.lower() for h in MONEY_NAME_HINTS)
            if is_money_name and col_type in IMPRECISE_MONEY_TYPES:
                issues.append(
                    f"[{table_name}.{col_name}] type '{col['type']}' is imprecise for money-like values; prefer NUMERIC/DECIMAL"
                )
            elif is_money_name and col_type.startswith(NON_NUMERIC_MONEY_TYPES_PREFIXES):
                issues.append(
                    f"[{table_name}.{col_name}] type '{col['type']}' is non-numeric for a money/amount-like column; prefer NUMERIC/DECIMAL"
                )

            if "NOT NULL" not in flags_str and "PK" not in flags_str and col_name.lower() not in ("id",):
                pass  # too noisy to flag every nullable column; left for manual review

        for fk_col in fk_cols:
            issues.append(f"[{table_name}.{fk_col}] foreign key column has no index")

        if not any("PK" in " ".join(c["flags"]) for c in table["columns"]) and not any(
            "PRIMARY KEY" in c.upper() for c in table["constraints"]
        ):
            issues.append(f"[{table_name}] no PRIMARY KEY found")

    return issues


def render_file(path: Path) -> tuple[str, List[dict], List[dict], List[dict]]:
    raw = path.read_text(encoding="utf-8", errors="replace")
    clean = strip_sql_comments(raw)
    statements = split_statements(clean)

    tables, alters, indexes = [], [], []
    for stmt in statements:
        upper = stmt.upper().strip()
        if upper.startswith("CREATE TABLE"):
            parsed = parse_create_table(stmt)
            if parsed:
                tables.append(parsed)
        elif upper.startswith("ALTER TABLE"):
            parsed = parse_alter_table(stmt)
            if parsed:
                alters.append(parsed)
        elif upper.startswith("CREATE INDEX") or upper.startswith("CREATE UNIQUE INDEX"):
            parsed = parse_create_index(stmt)
            if parsed:
                indexes.append(parsed)

    lines_out = [f"# {path}"]
    for table in tables:
        lines_out.append(f"TABLE {table['table']}")
        for col in table["columns"]:
            flags = f"  [{', '.join(col['flags'])}]" if col["flags"] else ""
            lines_out.append(f"  {col['name']}: {col['type']}{flags}")
        for constraint in table["constraints"]:
            lines_out.append(f"  CONSTRAINT: {constraint}")
    for alter in alters:
        lines_out.append(f"ALTER {alter['table']}: {alter['raw']}")
    for idx in indexes:
        kind = "UNIQUE INDEX" if idx["unique"] else "INDEX"
        lines_out.append(f"{kind} {idx['index']} ON {idx['table']} ({', '.join(idx['columns'])})")

    return "\n".join(lines_out) + "\n", tables, alters, indexes


def collect_files(args: argparse.Namespace) -> List[Path]:
    files = [Path(f) for f in args.files]
    if args.dir:
        files.extend(sorted(Path(args.dir).rglob("V*.sql")))
    deduped, seen = [], set()
    for p in files:
        key = str(p.resolve()) if p.exists() else str(p)
        if key not in seen:
            seen.add(key)
            deduped.append(p)
    return deduped


def main() -> None:
    parser = argparse.ArgumentParser(description="Compact summaries of Flyway SQL migrations.")
    parser.add_argument("files", nargs="*", help="Individual .sql files to process")
    parser.add_argument("--dir", metavar="DIRECTORY", help="Process all V*.sql files in a directory (recursive)")
    parser.add_argument("--check", action="store_true", help="Print heuristic schema issues (missing PK/index, imprecise types)")
    args = parser.parse_args()

    files = collect_files(args)
    if not files:
        parser.print_help()
        sys.exit(1)

    all_tables, all_indexes, all_alters = [], [], []
    for fpath in files:
        if not fpath.exists():
            print(f"WARN: not found - {fpath}", file=sys.stderr)
            continue
        rendered, tables, alters, indexes = render_file(fpath)
        all_tables.extend(tables)
        all_alters.extend(alters)
        all_indexes.extend(indexes)
        if not args.check:
            print(rendered)

    if args.check:
        issues = check_issues(all_tables, all_indexes, all_alters)
        if issues:
            print("# Schema issues found:")
            for issue in issues:
                print(f"  - {issue}")
        else:
            print("# No heuristic issues found.")


if __name__ == "__main__":
    main()
