#!/usr/bin/env python3
"""
project_map.py - One-shot structural index of LoanApp (backend + frontend).

Instead of Claude exploring backend/src and frontend/src file by file (Glob +
Read per file), this walks both trees once and prints one compact line per
Java/TypeScript file: relative path, detected mode (controller/entity/
service/dto/component/...) and symbol name. Meant to be read at the start of
a task to decide which files are actually worth reading in full.

Examples:
  python tools/project_map.py
  python tools/project_map.py --backend-only
  python tools/project_map.py --mode controller
  python tools/project_map.py --grep Loan
"""

from __future__ import annotations

import argparse
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent))
import skeletonizer as sk  # noqa: E402

REPO_ROOT = Path(__file__).parent.parent
BACKEND_JAVA = REPO_ROOT / "backend" / "src" / "main" / "java"
BACKEND_TEST = REPO_ROOT / "backend" / "src" / "test" / "java"
FRONTEND_APP = REPO_ROOT / "frontend" / "src" / "app"
MIGRATIONS = REPO_ROOT / "backend" / "src" / "main" / "resources" / "db" / "migration"


def symbol_from_summary(summary: str) -> str:
    for line in summary.splitlines():
        if line.startswith("- symbol:"):
            return line[len("- symbol:") :].strip()
    return ""


def describe_file(path: Path) -> tuple[str, str]:
    """Returns (mode, symbol) for a single Java/TS file."""
    try:
        content = path.read_text(encoding="utf-8", errors="replace")
    except OSError:
        return "?", ""
    mode = sk.detect_mode(path, content, "auto")
    summary = sk.build_summary(path, content, mode)
    return mode, symbol_from_summary(summary)


def walk(base: Path, exts: tuple[str, ...]) -> list[Path]:
    if not base.exists():
        return []
    files = []
    for ext in exts:
        files.extend(base.rglob(f"*.{ext}"))
    return sorted(files)


def main() -> None:
    parser = argparse.ArgumentParser(description="Compact structural index of LoanApp.")
    parser.add_argument("--backend-only", action="store_true")
    parser.add_argument("--frontend-only", action="store_true")
    parser.add_argument("--include-tests", action="store_true", help="Include *Test.java / *.spec.ts files")
    parser.add_argument("--mode", metavar="MODE", help="Filter by detected mode (controller, entity, service, dto, component, ...)")
    parser.add_argument("--grep", metavar="TEXT", help="Filter by substring in path or symbol name (case-insensitive)")
    args = parser.parse_args()

    rows: list[tuple[str, str, str]] = []  # (rel_path, mode, symbol)

    if not args.frontend_only:
        java_files = walk(BACKEND_JAVA, ("java",))
        if args.include_tests:
            java_files += walk(BACKEND_TEST, ("java",))
        for f in java_files:
            mode, symbol = describe_file(f)
            rows.append((str(f.relative_to(REPO_ROOT)), mode, symbol))

    if not args.backend_only:
        ts_files = walk(FRONTEND_APP, ("ts",))
        if not args.include_tests:
            ts_files = [f for f in ts_files if not f.name.endswith(".spec.ts")]
        for f in ts_files:
            mode, symbol = describe_file(f)
            rows.append((str(f.relative_to(REPO_ROOT)), mode, symbol))

    if args.mode:
        rows = [r for r in rows if r[1] == args.mode]
    if args.grep:
        needle = args.grep.lower()
        rows = [r for r in rows if needle in r[0].lower() or needle in r[2].lower()]

    rows.sort(key=lambda r: (r[1], r[0]))

    width_path = max((len(r[0]) for r in rows), default=10)
    width_mode = max((len(r[1]) for r in rows), default=10)
    for rel_path, mode, symbol in rows:
        print(f"{mode:<{width_mode}}  {rel_path:<{width_path}}  {symbol}")

    if not args.backend_only and not args.mode and not args.grep:
        migrations = sorted(MIGRATIONS.glob("V*.sql")) if MIGRATIONS.exists() else []
        if migrations:
            print()
            print("-- flyway migrations --")
            for m in migrations:
                print(f"sql            {m.relative_to(REPO_ROOT)}")

    print(f"\n# {len(rows)} files indexed", file=sys.stderr)


if __name__ == "__main__":
    main()
