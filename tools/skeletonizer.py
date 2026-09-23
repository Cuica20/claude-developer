#!/usr/bin/env python3
"""
skeletonizer.py - Context skeletonizer for LoanApp (Spring Boot + Angular).

Goal:
- reduce tokens when Claude inspects Java and TypeScript files
- keep structure, fields, signatures and high-value annotations
- provide compact summaries for common file types (controller, entity,
  service, dto, Angular component/service)

Examples:
  python tools/skeletonizer.py backend/src/main/java/com/capacitacion/loanapp/api/controller/LoanController.java
  python tools/skeletonizer.py LoanController.java --mode controller --summary-only
  python tools/skeletonizer.py frontend/src/app/features/loan/loan-form/loan-form.component.ts --summary-only
  python tools/skeletonizer.py --dir backend/src/main/java --ext java --stats
  python tools/skeletonizer.py --dir frontend/src/app --ext ts --auto-summary --stats
"""

from __future__ import annotations

import argparse
import re
import sys
from dataclasses import dataclass
from pathlib import Path
from typing import List, Optional


JAVA_MAPPING_RE = re.compile(r'@(Get|Post|Put|Delete|Patch)Mapping(?:\((.*?)\))?')
JAVA_REQUEST_MAPPING_RE = re.compile(r'@RequestMapping(?:\((.*?)\))?')
JAVA_CLASS_RE = re.compile(
    r'(?:public|protected|private|abstract|final|static|\s)*(class|interface|enum|record)\s+(\w+)'
)
TS_CLASS_RE = re.compile(r'(?:export\s+)?(?:abstract\s+)?class\s+(\w+)')
TS_INTERFACE_RE = re.compile(r'(?:export\s+)?(interface|type)\s+(\w+)')

LOMBOK_GETTER = {"@Data", "@Getter", "@Value"}
LOMBOK_SETTER = {"@Data", "@Setter"}
LOMBOK_CTOR = {"@Data", "@AllArgsConstructor", "@NoArgsConstructor", "@RequiredArgsConstructor", "@Value"}
LOMBOK_BUILDER = {"@Builder", "@SuperBuilder"}
LOMBOK_OBJECT = {"@Data", "@EqualsAndHashCode", "@ToString", "@Value"}


@dataclass
class SkeletonOptions:
    keep_imports: bool = False
    keep_comments: bool = False
    public_only: bool = False
    summary_only: bool = False
    fields_only: bool = False
    endpoints_only: bool = False
    mode: str = "auto"


@dataclass
class ParsedFile:
    path: Path
    content: str
    mode: str
    skeleton: str
    summary: str


def approx_tokens(text: str) -> int:
    return max(1, len(text) // 4)


def count_braces(line: str) -> int:
    """Net open braces minus close braces, ignoring simple strings and // comments."""
    in_double = False
    in_single = False
    escaped = False
    count = 0
    i = 0
    while i < len(line):
        c = line[i]
        if escaped:
            escaped = False
            i += 1
            continue
        if c == "\\" and (in_double or in_single):
            escaped = True
            i += 1
            continue
        if c == '"' and not in_single:
            in_double = not in_double
        elif c == "'" and not in_double:
            in_single = not in_single
        elif (
            c == "/"
            and i + 1 < len(line)
            and line[i + 1] == "/"
            and not in_double
            and not in_single
        ):
            break
        elif not in_double and not in_single:
            if c == "{":
                count += 1
            elif c == "}":
                count -= 1
        i += 1
    return count


def extract_annotation_name(line: str) -> str:
    return re.split(r"[\s(]", line.strip())[0]


def is_comment_line(stripped: str) -> bool:
    return stripped.startswith("//") or stripped.startswith("/*") or stripped.startswith("*") or stripped.startswith("*/")


def collapse_blank_lines(text: str) -> str:
    return re.sub(r"\n{3,}", "\n\n", text).strip() + "\n"


def is_lombok_generated(method_sig: str, class_annotations: set[str]) -> bool:
    sig = method_sig.strip()

    if (LOMBOK_GETTER & class_annotations) and re.match(r"(public\s+)?([\w<>\[\], ?]+\s+)?(get|is)[A-Z]\w*\(\s*\)", sig):
        return True
    if (LOMBOK_SETTER & class_annotations) and re.match(r"(public\s+)?(void\s+)?set[A-Z]\w*\([^)]+\)", sig):
        return True
    if LOMBOK_OBJECT & class_annotations:
        if re.search(r"\bequals\s*\(", sig):
            return True
        if re.search(r"\bhashCode\s*\(\s*\)", sig):
            return True
        if re.search(r"\btoString\s*\(\s*\)", sig):
            return True
        if re.search(r"\bcanEqual\s*\(", sig):
            return True
    if LOMBOK_BUILDER & class_annotations:
        if re.search(r"\bbuilder\s*\(\s*\)", sig):
            return True
        if re.search(r"\btoBuilder\s*\(\s*\)", sig):
            return True
    return False


def detect_mode(path: Path, content: str, requested_mode: str) -> str:
    if requested_mode != "auto":
        return requested_mode

    lower_name = path.name.lower()
    lower_path = str(path).lower()
    if path.suffix.lower() == ".java":
        if "@RestController" in content or "@Controller" in content:
            return "controller"
        if "@Entity" in content:
            return "entity"
        if "Dto" in path.stem or "/dto/" in lower_path or "\\dto\\" in lower_path:
            return "dto"
        if "/validation/" in lower_path or "\\validation\\" in lower_path or lower_name.endswith("validator.java"):
            return "validator"
        if lower_name.endswith("service.java") or lower_name.endswith("serviceimpl.java"):
            return "service"
        if lower_name.endswith("test.java"):
            return "test"
        return "java"

    if path.suffix.lower() in (".ts", ".tsx"):
        if "@Component" in content:
            return "component"
        if lower_name.endswith(".guard.ts"):
            return "guard"
        if lower_name.endswith(".interceptor.ts"):
            return "interceptor"
        if "@Injectable" in content or lower_name.endswith(".service.ts"):
            return "service"
        if lower_name.endswith(".spec.ts"):
            return "test"
        if "interface" in lower_name or "model" in lower_name or "dto" in lower_name:
            return "dto"
        return "typescript"

    return "unknown"


def parse_java_path_from_mapping(raw: str) -> str:
    if not raw:
        return ""
    quoted = re.findall(r'"([^"]+)"', raw)
    if quoted:
        return quoted[0]
    single = re.findall(r"'([^']+)'", raw)
    if single:
        return single[0]
    value_match = re.search(r"\bvalue\s*=\s*\"([^\"]+)\"", raw)
    if value_match:
        return value_match.group(1)
    path_match = re.search(r"\bpath\s*=\s*\"([^\"]+)\"", raw)
    if path_match:
        return path_match.group(1)
    return ""


def join_paths(base: str, sub: str) -> str:
    base = (base or "").strip()
    sub = (sub or "").strip()
    if not base and not sub:
        return ""
    if not base:
        return sub if sub.startswith("/") else f"/{sub}"
    if not sub:
        return base if base.startswith("/") else f"/{base}"
    return "/" + "/".join(part.strip("/") for part in (base, sub) if part.strip("/"))


def extract_java_method_name(sig: str) -> str:
    match = re.search(r"([A-Za-z_]\w*)\s*\([^()]*\)\s*$", sig.strip())
    return match.group(1) if match else sig.strip()


def extract_return_type(sig: str) -> str:
    compact = re.sub(r"\s+", " ", sig).strip()
    before_params = compact.split("(")[0].strip()
    tokens = before_params.split()
    if not tokens:
        return ""
    if len(tokens) == 1:
        return tokens[0]
    return tokens[-2]


def extract_type_and_name_from_field(line: str) -> tuple[str, str]:
    cleaned = line.strip().rstrip(";")
    cleaned = re.sub(r"\s*=\s*.*$", "", cleaned)
    cleaned = re.sub(r"@\w+(?:\([^)]*\))?\s*", "", cleaned)
    cleaned = re.sub(
        r"\b(private|public|protected|static|final|transient|volatile|readonly)\b",
        "",
        cleaned,
    )
    cleaned = re.sub(r"\s+", " ", cleaned).strip()
    if not cleaned:
        return "", ""
    parts = cleaned.split(" ")
    if len(parts) == 1:
        return "", parts[0]
    return " ".join(parts[:-1]), parts[-1]


def skeletonize_java(content: str, options: SkeletonOptions) -> str:
    lines = content.splitlines()
    result: List[str] = []
    depth = 0
    skip_until_depth: Optional[int] = None
    pending_sig: List[str] = []
    pending_annotations: List[str] = []
    class_annotations: set[str] = set()

    for line in lines:
        stripped = line.strip()
        net = count_braces(line)

        if skip_until_depth is not None:
            depth += net
            if depth <= skip_until_depth:
                skip_until_depth = None
            continue

        if pending_sig:
            pending_sig.append(line)
            depth += net
            if "{" in line:
                indent = len(pending_sig[0]) - len(pending_sig[0].lstrip())
                full = " ".join(part.strip() for part in pending_sig)
                sig = full[: full.rfind("{")].rstrip()
                if (not options.public_only or "public " in sig) and not is_lombok_generated(sig, class_annotations):
                    result.append(" " * indent + sig + " {}")
                pending_sig = []
                skip_until_depth = depth - 1
            continue

        if not stripped:
            result.append("")
            depth += net
            continue

        if is_comment_line(stripped):
            if options.keep_comments:
                result.append(line)
            depth += net
            continue

        if stripped.startswith("package "):
            result.append(line)
            depth += net
            continue

        if stripped.startswith("import "):
            if options.keep_imports:
                result.append(line)
            depth += net
            continue

        if stripped.startswith("@"):
            pending_annotations.append(extract_annotation_name(stripped))
            result.append(line)
            depth += net
            continue

        class_match = JAVA_CLASS_RE.search(stripped)
        if class_match:
            class_annotations = set(pending_annotations)
            pending_annotations = []
            result.append(line)
            depth += net
            continue

        if stripped in ("}", "};"):
            pending_annotations = []
            result.append(line)
            depth += net
            continue

        if stripped.endswith(";") and "(" not in stripped.split(";")[0]:
            if not options.public_only or re.search(r"\bpublic\b", stripped):
                result.append(line)
            pending_annotations = []
            depth += net
            continue

        if depth >= 1 and "{" in line and ")" in line:
            sig = line[: line.rfind("{")].rstrip()
            if (not options.public_only or "public " in sig) and not is_lombok_generated(sig, class_annotations):
                result.append(sig + " {}")
            pending_annotations = []
            depth += net
            skip_until_depth = depth - 1
            continue

        if depth >= 1 and ")" in line and "{" not in line and not stripped.endswith(";"):
            pending_sig = [line]
            depth += net
            continue

        pending_annotations = []
        result.append(line)
        depth += net

    return collapse_blank_lines("\n".join(result))


def skeletonize_typescript(content: str, options: SkeletonOptions) -> str:
    lines = content.splitlines()
    result: List[str] = []
    depth = 0
    skip_until_depth: Optional[int] = None
    pending_sig: List[str] = []
    in_interface = False
    interface_depth = 0

    for line in lines:
        stripped = line.strip()
        net = count_braces(line)

        if skip_until_depth is not None:
            depth += net
            if depth <= skip_until_depth:
                skip_until_depth = None
                if stripped.startswith("}"):
                    result.append(line)
            continue

        if pending_sig:
            pending_sig.append(line)
            depth += net
            if "{" in line:
                indent = len(pending_sig[0]) - len(pending_sig[0].lstrip())
                full = " ".join(part.strip() for part in pending_sig)
                sig = full[: full.rfind("{")].rstrip()
                if not options.public_only or "public " in sig:
                    result.append(" " * indent + sig + " {}")
                pending_sig = []
                skip_until_depth = depth - 1
            elif stripped.endswith(";"):
                indent = len(pending_sig[0]) - len(pending_sig[0].lstrip())
                full = " ".join(part.strip() for part in pending_sig)
                if not options.public_only or "public " in full:
                    result.append(" " * indent + full)
                pending_sig = []
            continue

        if not stripped:
            result.append("")
            depth += net
            continue

        if is_comment_line(stripped):
            if options.keep_comments:
                result.append(line)
            depth += net
            continue

        if stripped.startswith("import "):
            if options.keep_imports:
                result.append(line)
            depth += net
            continue

        if stripped.startswith("@"):
            result.append(line)
            depth += net
            continue

        if in_interface:
            result.append(line)
            depth += net
            if depth <= interface_depth:
                in_interface = False
            continue

        if TS_INTERFACE_RE.match(stripped):
            in_interface = True
            interface_depth = depth
            result.append(line)
            depth += net
            continue

        if TS_CLASS_RE.match(stripped):
            result.append(line)
            depth += net
            continue

        if stripped.startswith("}"):
            result.append(line)
            depth += net
            continue

        if stripped.endswith(";") and "(" not in stripped:
            if not options.public_only or stripped.startswith("public "):
                result.append(line)
            depth += net
            continue

        if re.match(r"(private|public|protected|readonly|static|abstract|override)\s+", stripped) and "(" not in stripped:
            if not options.public_only or stripped.startswith("public "):
                result.append(line)
            depth += net
            continue

        if depth >= 1 and "(" in line and "{" in line:
            sig = line[: line.rfind("{")].rstrip()
            if not options.public_only or "public " in sig:
                result.append(sig + " {}")
            depth += net
            skip_until_depth = depth - 1
            continue

        if depth >= 1 and "(" in line and "{" not in line and not stripped.endswith(";"):
            pending_sig = [line]
            depth += net
            continue

        if stripped.endswith(";"):
            result.append(line)
            depth += net
            continue

        result.append(line)
        depth += net

    return collapse_blank_lines("\n".join(result))


def extract_java_summary(path: Path, content: str, mode: str) -> str:
    lines = content.splitlines()
    package_name = ""
    class_name = ""
    class_kind = ""
    class_annotations: List[str] = []
    pending_annotations: List[str] = []
    fields: List[str] = []
    methods: List[str] = []
    endpoints: List[str] = []
    class_base_path = ""
    endpoint_annotations: List[str] = []
    depth = 0

    for line in lines:
        stripped = line.strip()
        net = count_braces(line)

        if stripped.startswith("package "):
            package_name = stripped[len("package ") :].rstrip(";")

        if stripped.startswith("@"):
            pending_annotations.append(extract_annotation_name(stripped))
            endpoint_annotations.append(stripped)
            if "@RequestMapping" in stripped and depth == 0:
                match = JAVA_REQUEST_MAPPING_RE.search(stripped)
                if match:
                    class_base_path = parse_java_path_from_mapping(match.group(1) or "")
            depth += net
            continue

        class_match = JAVA_CLASS_RE.search(stripped)
        if class_match and not class_name:
            class_kind = class_match.group(1)
            class_name = class_match.group(2)
            class_annotations = pending_annotations[:]
            pending_annotations = []
            depth += net
            continue

        if depth >= 1 and stripped.endswith(";") and "(" not in stripped.split(";")[0]:
            field_type, field_name = extract_type_and_name_from_field(stripped)
            if field_name:
                fields.append(f"{field_type} {field_name}".strip())

        if depth >= 1 and "(" in stripped and (stripped.endswith("{") or stripped.endswith(";")):
            normalized = stripped.rstrip("{").rstrip(";").strip()
            method_name = extract_java_method_name(normalized)
            methods.append(method_name)
            mapping_line = next((ann for ann in reversed(endpoint_annotations) if "Mapping" in ann), "")
            mapping_match = JAVA_MAPPING_RE.search(mapping_line)
            if mapping_match:
                verb = mapping_match.group(1).upper()
                sub_path = parse_java_path_from_mapping(mapping_match.group(2) or "")
                full_path = join_paths(class_base_path, sub_path)
                return_type = extract_return_type(normalized)
                endpoints.append(f"{verb} {full_path or '/'} -> {return_type}".strip())
            endpoint_annotations = []

        if stripped and not stripped.startswith("@"):
            pending_annotations = []
            if depth == 0:
                endpoint_annotations = []

        depth += net

    lines_out = [
        f"# Summary: {path}",
        f"- mode: {mode}",
    ]
    if package_name:
        lines_out.append(f"- package: {package_name}")
    if class_name:
        lines_out.append(f"- symbol: {class_kind} {class_name}")
    if class_annotations:
        lines_out.append(f"- annotations: {', '.join(class_annotations)}")
    if fields:
        preview = ", ".join(fields[:8])
        if len(fields) > 8:
            preview += ", ..."
        lines_out.append(f"- fields ({len(fields)}): {preview}")
    if methods:
        preview = ", ".join(methods[:8])
        if len(methods) > 8:
            preview += ", ..."
        lines_out.append(f"- methods ({len(methods)}): {preview}")
    if endpoints:
        lines_out.append("- endpoints:")
        lines_out.extend(f"  - {endpoint}" for endpoint in endpoints)
    return "\n".join(lines_out) + "\n"


def extract_ts_constructor_injections(content: str) -> List[str]:
    match = re.search(r"constructor\s*\((.*?)\)\s*{", content, re.S)
    if not match:
        return []
    raw = match.group(1)
    params = [part.strip() for part in raw.split(",") if part.strip()]
    result: List[str] = []
    for param in params:
        compact = re.sub(r"\s+", " ", param)
        name_match = re.search(r"\b(private|public|protected|readonly)\s+([A-Za-z_]\w*)\s*:\s*([^=]+)", compact)
        if name_match:
            result.append(f"{name_match.group(2)}: {name_match.group(3).strip()}")
    return result


def _ts_field_name(text: str) -> str:
    """Extract the field name from a TypeScript field declaration (removes modifiers)."""
    cleaned = re.sub(r"\b(public|private|protected|readonly|static|abstract|override)\s+", "", text.strip())
    match = re.match(r"([A-Za-z_]\w*)\s*[?!]?\s*:", cleaned)
    return match.group(1) if match else ""


def extract_typescript_summary(path: Path, content: str, mode: str) -> str:
    lines = content.splitlines()
    symbol_kind = ""
    symbol_name = ""
    decorators: List[str] = []
    methods: List[str] = []
    fields: List[str] = []
    inputs: List[str] = []
    outputs: List[str] = []
    signals: List[str] = []
    pending_decorators: List[str] = []
    pending_input = False
    pending_output = False
    depth = 0

    for line in lines:
        stripped = line.strip()
        net = count_braces(line)

        if stripped.startswith("@"):
            pending_decorators.append(extract_annotation_name(stripped))
            if re.match(r"@Input\b", stripped):
                inline = re.search(r"@Input[^)]*\)\s+(.*)", stripped)
                if inline:
                    name = _ts_field_name(inline.group(1))
                    if name:
                        inputs.append(name)
                    else:
                        pending_input = True
                else:
                    pending_input = True
            if re.match(r"@Output\b", stripped):
                inline = re.search(r"@Output[^)]*\)\s+(.*)", stripped)
                if inline:
                    name = _ts_field_name(inline.group(1))
                    if name:
                        outputs.append(name)
                    else:
                        pending_output = True
                else:
                    pending_output = True
            depth += net
            continue

        if pending_input or pending_output:
            name = _ts_field_name(stripped)
            if name:
                if pending_input:
                    inputs.append(name)
                if pending_output:
                    outputs.append(name)
            pending_input = False
            pending_output = False

        # Angular signals: `foo = signal(...)`, `bar = input<T>()`, `baz = output<T>()`
        signal_match = re.match(r"(?:readonly\s+)?([A-Za-z_]\w*)\s*=\s*(signal|input|output|computed)[<(]", stripped)
        if signal_match:
            signals.append(f"{signal_match.group(1)}: {signal_match.group(2)}()")

        class_match = TS_CLASS_RE.match(stripped)
        if class_match and not symbol_name:
            symbol_kind = "class"
            symbol_name = class_match.group(1)
            decorators = pending_decorators[:]
            pending_decorators = []
            depth += net
            continue

        interface_match = TS_INTERFACE_RE.match(stripped)
        if interface_match and not symbol_name:
            symbol_kind = interface_match.group(1)
            symbol_name = interface_match.group(2)
            decorators = pending_decorators[:]
            pending_decorators = []
            depth += net
            continue

        if depth >= 1 and stripped.endswith(";") and "(" not in stripped:
            fields.append(stripped.rstrip(";"))

        if depth >= 1 and "(" in stripped and (stripped.endswith("{") or stripped.endswith(";")):
            method_name = extract_java_method_name(stripped.rstrip("{").rstrip(";").strip())
            methods.append(method_name)

        if stripped and not stripped.startswith("@"):
            pending_decorators = []

        depth += net

    constructor_injections = extract_ts_constructor_injections(content)
    selector_match = re.search(r"selector\s*:\s*['\"]([^'\"]+)['\"]", content)
    template_match = re.search(r"templateUrl\s*:\s*['\"]([^'\"]+)['\"]", content)
    standalone_match = re.search(r"standalone\s*:\s*(true|false)", content)

    lines_out = [
        f"# Summary: {path}",
        f"- mode: {mode}",
    ]
    if symbol_name:
        lines_out.append(f"- symbol: {symbol_kind} {symbol_name}")
    if decorators:
        lines_out.append(f"- decorators: {', '.join(decorators)}")
    if selector_match:
        lines_out.append(f"- selector: {selector_match.group(1)}")
    if standalone_match:
        lines_out.append(f"- standalone: {standalone_match.group(1)}")
    if template_match:
        lines_out.append(f"- templateUrl: {template_match.group(1)}")
    if inputs:
        lines_out.append(f"- inputs ({len(inputs)}): {', '.join(inputs[:8])}")
    if outputs:
        lines_out.append(f"- outputs ({len(outputs)}): {', '.join(outputs[:8])}")
    if signals:
        preview = ", ".join(signals[:8])
        if len(signals) > 8:
            preview += ", ..."
        lines_out.append(f"- signals ({len(signals)}): {preview}")
    if constructor_injections:
        preview = ", ".join(constructor_injections[:8])
        if len(constructor_injections) > 8:
            preview += ", ..."
        lines_out.append(f"- injections ({len(constructor_injections)}): {preview}")
    if fields:
        preview = ", ".join(fields[:8])
        if len(fields) > 8:
            preview += ", ..."
        lines_out.append(f"- fields ({len(fields)}): {preview}")
    if methods:
        preview = ", ".join(methods[:8])
        if len(methods) > 8:
            preview += ", ..."
        lines_out.append(f"- methods ({len(methods)}): {preview}")
    return "\n".join(lines_out) + "\n"


def build_summary(path: Path, content: str, mode: str) -> str:
    if path.suffix.lower() == ".java":
        return extract_java_summary(path, content, mode)
    if path.suffix.lower() in (".ts", ".tsx"):
        return extract_typescript_summary(path, content, mode)
    return f"# Summary: {path}\n- mode: {mode}\n"


def build_skeleton(path: Path, content: str, mode: str, options: SkeletonOptions) -> str:
    if path.suffix.lower() == ".java":
        return skeletonize_java(content, options)
    if path.suffix.lower() in (".ts", ".tsx"):
        return skeletonize_typescript(content, options)
    return f"# Unsupported type: {path.suffix.lower()}\n"


def _all_java_fields(content: str) -> List[tuple]:
    """Extract all (field_type, field_name) tuples from a Java file (no truncation)."""
    lines = content.splitlines()
    depth = 0
    fields: List[tuple] = []
    for line in lines:
        stripped = line.strip()
        net = count_braces(line)
        if depth >= 1 and stripped.endswith(";") and "(" not in stripped.split(";")[0]:
            ftype, fname = extract_type_and_name_from_field(stripped)
            if fname:
                fields.append((ftype, fname))
        depth += net
    return fields


def _all_ts_fields(content: str) -> List[tuple]:
    """Extract all (declaration, field_name) tuples from a TypeScript file (no truncation)."""
    lines = content.splitlines()
    depth = 0
    fields: List[tuple] = []
    for line in lines:
        stripped = line.strip()
        net = count_braces(line)
        if depth >= 1 and stripped.endswith(";") and "(" not in stripped:
            name = _ts_field_name(stripped)
            if name:
                fields.append((stripped.rstrip(";"), name))
        depth += net
    return fields


def find_field_in_file(path: Path, field_name: str, content: str) -> str:
    """Return 'FOUND ...' or 'NOT FOUND ...' for a field name search in a file."""
    lower = field_name.lower()
    suffix = path.suffix.lower()
    if suffix == ".java":
        for ftype, fname in _all_java_fields(content):
            if fname.lower() == lower:
                return f"FOUND  {path}  ->  {ftype} {fname}"
    elif suffix in (".ts", ".tsx"):
        for decl, fname in _all_ts_fields(content):
            if fname.lower() == lower:
                return f"FOUND  {path}  ->  {decl}"
    return f"NOT FOUND  {path}"


def find_interface_in_file(path: Path, name: str, content: str) -> str:
    """Return 'FOUND ...' or 'NOT FOUND ...' for a TypeScript interface or type search."""
    pattern = re.compile(rf"\b(interface|type)\s+{re.escape(name)}\b")
    for i, line in enumerate(content.splitlines(), 1):
        if pattern.search(line):
            return f"FOUND  {path}:{i}  ->  {line.strip()}"
    return f"NOT FOUND  {path}"


def parse_file(path: Path, options: SkeletonOptions) -> ParsedFile:
    content = path.read_text(encoding="utf-8", errors="replace")
    mode = detect_mode(path, content, options.mode)
    summary = build_summary(path, content, mode)
    skeleton = build_skeleton(path, content, mode, options)
    return ParsedFile(path=path, content=content, mode=mode, skeleton=skeleton, summary=summary)


def _filter_summary(summary: str, options: SkeletonOptions) -> str:
    lines = summary.splitlines()
    result = []
    in_endpoints = False
    for line in lines:
        if line.startswith("# Summary") or line.startswith("- mode"):
            result.append(line)
            in_endpoints = False
        elif options.fields_only and line.startswith("- fields"):
            result.append(line)
            in_endpoints = False
        elif options.endpoints_only:
            if line.startswith("- endpoints"):
                in_endpoints = True
                result.append(line)
            elif in_endpoints and line.startswith("  - "):
                result.append(line)
            else:
                in_endpoints = False
    return "\n".join(result) + "\n"


def render_output(parsed: ParsedFile, options: SkeletonOptions) -> str:
    if options.fields_only or options.endpoints_only:
        return _filter_summary(parsed.summary, options)
    if options.summary_only:
        return parsed.summary
    return parsed.summary + "\n" + parsed.skeleton


def collect_files(args: argparse.Namespace) -> List[Path]:
    files = [Path(f) for f in args.files]
    if args.dir:
        base = Path(args.dir)
        exts = args.ext or ["java", "ts"]
        for ext in exts:
            files.extend(sorted(base.rglob(f"*.{ext}")))
    deduped: List[Path] = []
    seen = set()
    for path in files:
        key = str(path.resolve()) if path.exists() else str(path)
        if key not in seen:
            seen.add(key)
            deduped.append(path)
    return deduped


def main() -> None:
    parser = argparse.ArgumentParser(
        description="Context skeletonizer for Java and TypeScript with compact summaries."
    )
    parser.add_argument("files", nargs="*", help="Individual files to process")
    parser.add_argument("--dir", metavar="DIRECTORY", help="Process a directory recursively")
    parser.add_argument("--ext", action="append", choices=["java", "ts", "tsx"], metavar="EXT", help="Extensions to include")
    parser.add_argument("--out", metavar="DIRECTORY", help="Write output files here instead of stdout")
    parser.add_argument("--stats", action="store_true", help="Print estimated token reduction")
    parser.add_argument("--keep-imports", action="store_true", help="Keep import lines in skeleton output")
    parser.add_argument("--keep-comments", action="store_true", help="Keep comment lines in skeleton output")
    parser.add_argument("--public-only", action="store_true", help="Keep only public members where applicable")
    parser.add_argument("--summary-only", action="store_true", help="Print only the compact structural summary")
    parser.add_argument("--fields-only", action="store_true", help="Print only the fields line from the summary (implies --summary-only)")
    parser.add_argument("--endpoints-only", action="store_true", help="Print only the endpoints section from the summary (implies --summary-only)")
    parser.add_argument("--find-field", metavar="FIELDNAME", help="Search for a field by name across all processed files; prints FOUND/NOT FOUND per file")
    parser.add_argument("--find-interface", metavar="NAME", help="Search for a TypeScript interface or type by name; prints FOUND/NOT FOUND per file")
    parser.add_argument("--auto-summary", action="store_true", help="Automatically use --summary-only when processing more than 10 files via --dir")
    parser.add_argument(
        "--mode",
        choices=["auto", "java", "entity", "controller", "service", "component", "dto", "validator", "guard", "interceptor", "test", "typescript"],
        default="auto",
        help="Force a file interpretation mode",
    )
    args = parser.parse_args()

    files = collect_files(args)
    if not files:
        parser.print_help()
        sys.exit(1)

    # find-field / find-interface mode — scan files and exit
    if args.find_field or args.find_interface:
        found_any = False
        for fpath in files:
            if not fpath.exists():
                print(f"WARN: not found - {fpath}", file=sys.stderr)
                continue
            content = fpath.read_text(encoding="utf-8", errors="replace")
            if args.find_field:
                result = find_field_in_file(fpath, args.find_field, content)
            else:
                result = find_interface_in_file(fpath, args.find_interface, content)
            if result.startswith("FOUND"):
                print(result)
                found_any = True
        if not found_any:
            print("(no match found in any processed file)")
        return

    options = SkeletonOptions(
        keep_imports=args.keep_imports,
        keep_comments=args.keep_comments,
        public_only=args.public_only,
        summary_only=args.summary_only,
        fields_only=args.fields_only,
        endpoints_only=args.endpoints_only,
        mode=args.mode,
    )

    # auto-summary: force summary-only when scanning many files with --dir
    if args.auto_summary and args.dir and len(files) > 10 and not args.summary_only:
        options.summary_only = True

    total_orig = 0
    total_rendered = 0

    for fpath in files:
        if not fpath.exists():
            print(f"WARN: not found - {fpath}", file=sys.stderr)
            continue

        parsed = parse_file(fpath, options)
        rendered = render_output(parsed, options)
        orig_tok = approx_tokens(parsed.content)
        rendered_tok = approx_tokens(rendered)
        total_orig += orig_tok
        total_rendered += rendered_tok

        if args.out:
            out_dir = Path(args.out)
            out_dir.mkdir(parents=True, exist_ok=True)
            out_file = out_dir / f"{fpath.stem}.skeleton{fpath.suffix}"
            out_file.write_text(rendered, encoding="utf-8")
            if args.stats:
                pct = (1 - rendered_tok / orig_tok) * 100 if orig_tok else 0
                print(f"{fpath.name:50s}  {orig_tok:>6} -> {rendered_tok:>5} tokens  ({pct:.0f}% less)")
        else:
            if len(files) > 1:
                print(f'\n{"=" * 80}')
                print(f"# {fpath}")
                print(f'{"=" * 80}')
            print(rendered.rstrip())

    if args.stats and len(files) > 1:
        pct = (1 - total_rendered / total_orig) * 100 if total_orig else 0
        print(f'\n{"-" * 80}')
        print(f"TOTAL  {total_orig:>6} -> {total_rendered:>5} tokens  ({pct:.0f}% estimated reduction)")


if __name__ == "__main__":
    main()
