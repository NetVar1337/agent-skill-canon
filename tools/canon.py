#!/usr/bin/env python3
"""Audit and index the agent skill canon without third-party dependencies."""

from __future__ import annotations

import argparse
import hashlib
import json
import re
import subprocess
import sys
from collections import Counter, defaultdict
from dataclasses import dataclass
from pathlib import Path
from typing import Iterable

ROOT = Path(__file__).resolve().parents[1]
SKILLS = ROOT / "skills"
CATALOG_JSON = ROOT / "catalog" / "catalog.json"
CATALOG_MD = ROOT / "catalog" / "CATALOG.md"
QUALITY_MD = ROOT / "catalog" / "QUALITY.md"
DOMAINS_JSON = ROOT / "config" / "domains.json"

NAME_RE = re.compile(r"^[a-z0-9][a-z0-9._-]*$")
LINK_RE = re.compile(r"!?\[[^\]]*\]\(([^)]+)\)")
CONFLICT_RE = re.compile(r"^(?:<{7}|>{7})(?:\s|$)", re.MULTILINE)


@dataclass(frozen=True)
class Skill:
    name: str
    description: str
    version: str
    license: str
    path: str
    sha256: str
    references: int
    scripts: int
    quality: int
    domains: tuple[str, ...]


def _unquote(value: str) -> str:
    value = value.strip()
    if len(value) >= 2 and value[0] == value[-1] and value[0] in "\"'":
        return value[1:-1]
    return value


def parse_frontmatter(path: Path) -> tuple[dict[str, str], str]:
    text = path.read_text(encoding="utf-8", errors="replace")
    lines = text.splitlines()
    if not lines or lines[0].strip() != "---":
        raise ValueError("frontmatter must start at byte zero")
    try:
        end = next(i for i in range(1, len(lines)) if lines[i].strip() == "---")
    except StopIteration as exc:
        raise ValueError("frontmatter is not closed") from exc

    data: dict[str, str] = {}
    i = 1
    while i < end:
        line = lines[i]
        if line.startswith((" ", "\t")) or ":" not in line:
            i += 1
            continue
        key, raw = line.split(":", 1)
        key, raw = key.strip(), raw.strip()
        if raw in {"|", ">", "|-", ">-"}:
            block: list[str] = []
            i += 1
            while i < end and (not lines[i].strip() or lines[i].startswith((" ", "\t"))):
                block.append(lines[i].strip())
                i += 1
            data[key] = " ".join(part for part in block if part)
            continue
        data[key] = _unquote(raw)
        i += 1
    return data, "\n".join(lines[end + 1 :])


def load_domains() -> list[dict[str, object]]:
    return json.loads(DOMAINS_JSON.read_text(encoding="utf-8"))["domains"]


def classify(path: str, name: str, description: str, domains: list[dict[str, object]]) -> tuple[str, ...]:
    haystack = f"{path} {name} {description}".lower()
    matched = []
    for domain in domains:
        patterns = domain.get("patterns", [])
        if any(re.search(str(pattern), haystack, re.IGNORECASE) for pattern in patterns):
            matched.append(str(domain["id"]))
    return tuple(matched or ["general"])


def quality_score(meta: dict[str, str], body: str, path: Path) -> int:
    score = 20
    description = meta.get("description", "")
    if len(description) >= 40:
        score += 15
    if re.search(r"\buse when\b|当.*使用|用于", description, re.IGNORECASE):
        score += 10
    if re.search(r"^##?\s+.*(workflow|method|流程|方法|步骤)", body, re.MULTILINE | re.IGNORECASE):
        score += 20
    if re.search(r"(completion gate|definition of done|verification|验证|完成.*检查|验收)", body, re.IGNORECASE):
        score += 20
    if (path.parent / "references").is_dir():
        score += 5
    if (path.parent / "scripts").is_dir():
        score += 5
    if meta.get("version"):
        score += 3
    if meta.get("license"):
        score += 2
    return min(score, 100)


def discover() -> list[Skill]:
    domains = load_domains()
    result: list[Skill] = []
    for path in sorted(SKILLS.rglob("SKILL.md"), key=lambda p: p.as_posix().lower()):
        meta, body = parse_frontmatter(path)
        rel = path.relative_to(ROOT).as_posix()
        name = meta.get("name", "").strip()
        description = meta.get("description", "").strip()
        result.append(
            Skill(
                name=name,
                description=description,
                version=meta.get("version", ""),
                license=meta.get("license", ""),
                path=rel,
                sha256=hashlib.sha256(path.read_bytes()).hexdigest(),
                references=sum(1 for p in path.parent.rglob("*") if p.is_file() and "references" in p.parts),
                scripts=sum(1 for p in path.parent.rglob("*") if p.is_file() and "scripts" in p.parts),
                quality=quality_score(meta, body, path),
                domains=classify(rel, name, description, domains),
            )
        )
    return result


def _is_external(target: str) -> bool:
    return bool(re.match(r"^(?:[a-z][a-z0-9+.-]*:|#|/|\\|\{\{|<)", target, re.IGNORECASE))


def broken_links(path: Path, body: str) -> list[str]:
    broken = []
    prose = re.sub(r"```.*?```|~~~.*?~~~", "", body, flags=re.DOTALL)
    prose = re.sub(r"`[^`\n]*`", "", prose)
    for raw in LINK_RE.findall(prose):
        target = raw.strip().strip("<>").split("#", 1)[0]
        target = re.sub(r"\s+[\"'][^\"']*[\"']$", "", target).strip()
        if not target or _is_external(target) or any(mark in target for mark in ("*", "...", "$", "%")):
            continue
        candidate = (path.parent / target).resolve()
        try:
            candidate.relative_to(ROOT.resolve())
        except ValueError:
            continue
        if not candidate.exists():
            broken.append(raw)
    return broken


def audit() -> tuple[list[str], list[str], dict[str, int]]:
    errors: list[str] = []
    warnings: list[str] = []
    metrics: Counter[str] = Counter()
    names: defaultdict[str, list[str]] = defaultdict(list)

    paths = sorted(SKILLS.rglob("SKILL.md"), key=lambda p: p.as_posix().lower())
    metrics["skill_files"] = len(paths)
    for path in paths:
        rel = path.relative_to(ROOT).as_posix()
        try:
            meta, body = parse_frontmatter(path)
        except ValueError as exc:
            errors.append(f"{rel}: {exc}")
            metrics["frontmatter_errors"] += 1
            continue
        name, description = meta.get("name", "").strip(), meta.get("description", "").strip()
        if not name or not description:
            errors.append(f"{rel}: name and description are required")
            metrics["required_field_errors"] += 1
        elif not NAME_RE.fullmatch(name):
            warnings.append(f"{rel}: non-canonical name {name!r}")
            metrics["noncanonical_names"] += 1
        names[name].append(rel)
        if len(description) < 40:
            metrics["short_descriptions"] += 1
        if quality_score(meta, body, path) < 50:
            metrics["low_quality_skills"] += 1
        if CONFLICT_RE.search(body):
            errors.append(f"{rel}: unresolved merge marker")
            metrics["conflict_markers"] += 1
        for target in broken_links(path, body):
            warnings.append(f"{rel}: unresolved relative link {target!r}")
            metrics["broken_links"] += 1

    declared_names = set(names)
    for domain in load_domains():
        for primary in domain.get("primary", []):
            if str(primary) not in declared_names:
                errors.append(f"domain {domain['id']!r}: missing primary skill {primary!r}")
                metrics["missing_domain_primaries"] += 1

    duplicate_groups = {name: members for name, members in names.items() if name and len(members) > 1}
    metrics["duplicate_name_groups"] = len(duplicate_groups)
    metrics["duplicate_name_entries"] = sum(len(v) - 1 for v in duplicate_groups.values())
    for name, members in sorted(duplicate_groups.items()):
        warnings.append(f"duplicate skill name {name!r}: {', '.join(members)}")

    tracked = set(
        subprocess.check_output(
            ["git", "ls-files", "-z"], cwd=ROOT, text=True, encoding="utf-8"
        ).split("\0")
    )
    artifacts = [
        p.relative_to(ROOT).as_posix()
        for p in ROOT.rglob("*")
        if p.is_file()
        and (p.suffix in {".pyc", ".pyo"} or p.name in {".DS_Store", "Thumbs.db"})
        and p.relative_to(ROOT).as_posix() in tracked
    ]
    metrics["generated_artifacts"] = len(artifacts)
    for artifact in artifacts:
        errors.append(f"generated artifact is tracked/present: {artifact}")
    return errors, warnings, dict(sorted(metrics.items()))


def render_catalog(skills: list[Skill]) -> tuple[str, str, str]:
    domains = load_domains()
    by_domain: defaultdict[str, list[Skill]] = defaultdict(list)
    by_name: defaultdict[str, list[Skill]] = defaultdict(list)
    for skill in skills:
        by_name[skill.name].append(skill)
        for domain in skill.domains:
            by_domain[domain].append(skill)
    canonical = {
        name: min(members, key=lambda item: (item.path.count("/"), len(item.path), item.path)).path
        for name, members in by_name.items()
    }
    duplicate_names = {
        name: [member.path for member in members]
        for name, members in sorted(by_name.items())
        if len(members) > 1
    }

    payload = {
        "schemaVersion": 1,
        "skillCount": len(skills),
        "uniqueNameCount": len({skill.name for skill in skills}),
        "duplicateNames": duplicate_names,
        "domains": [
            {
                "id": domain["id"],
                "title": domain["title"],
                "summary": domain["summary"],
                "primary": domain.get("primary", []),
                "skillCount": len(by_domain[str(domain["id"])]),
            }
            for domain in domains
        ],
        "skills": [
            {**skill.__dict__, "canonical": skill.path == canonical[skill.name]}
            for skill in skills
        ],
    }
    json_text = json.dumps(payload, ensure_ascii=False, indent=2, sort_keys=True) + "\n"

    lines = [
        "# Skill Catalog",
        "",
        "> Generated by `py -3 tools/canon.py catalog`. Do not edit manually.",
        "",
        f"**{len(skills)} skill files · {payload['uniqueNameCount']} unique names · {len(domains)} domain routes**",
        "",
        "## Domain coverage",
        "",
        "| Domain | Skills | Primary entrypoints |",
        "|---|---:|---|",
    ]
    for domain in domains:
        primary = ", ".join(f"`{name}`" for name in domain.get("primary", []))
        lines.append(f"| [{domain['title']}](#{domain['id']}) | {len(by_domain[str(domain['id'])])} | {primary} |")

    for domain in domains:
        domain_id = str(domain["id"])
        lines.extend(["", f"<a id=\"{domain_id}\"></a>", f"## {domain['title']}", "", str(domain["summary"]), ""])
        for skill in sorted(by_domain[domain_id], key=lambda item: (item.name, item.path)):
            description = skill.description.replace("\n", " ").replace("|", "\\|")
            if len(description) > 180:
                description = description[:177].rstrip() + "..."
            label = " **canonical**" if len(by_name[skill.name]) > 1 and skill.path == canonical[skill.name] else ""
            lines.append(f"- [`{skill.name}`](../{skill.path}){label} — {description}")
    errors, warnings, metrics = audit()
    buckets = Counter((skill.quality // 10) * 10 for skill in skills)
    quality_lines = [
        "# Canon Quality",
        "",
        "> Generated by `py -3 tools/canon.py catalog`. Debt is baseline-controlled by CI.",
        "",
        f"- Skill files: **{len(skills)}**",
        f"- Unique names: **{len(by_name)}**",
        f"- Duplicate name groups: **{metrics.get('duplicate_name_groups', 0)}**",
        f"- Unresolved relative links: **{metrics.get('broken_links', 0)}**",
        f"- Structural errors: **{len(errors)}**",
        "",
        "## Quality distribution",
        "",
        "| Score | Skills |",
        "|---:|---:|",
    ]
    for bucket in range(100, 0, -10):
        quality_lines.append(f"| {bucket:02d}–{min(bucket + 9, 100):02d} | {buckets.get(bucket, 0)} |")
    quality_lines.extend([
        "",
        "## Lowest-scoring canonical entrypoints",
        "",
        "These are prioritization leads, not automatic defect findings. Scores reward trigger precision, workflow structure, verification gates, references, scripts, versioning, and license metadata.",
        "",
        "| Score | Skill | Path |",
        "|---:|---|---|",
    ])
    canonical_skills = [skill for skill in skills if skill.path == canonical[skill.name]]
    for skill in sorted(canonical_skills, key=lambda item: (item.quality, item.name, item.path))[:100]:
        quality_lines.append(f"| {skill.quality} | `{skill.name}` | [`{skill.path}`](../{skill.path}) |")
    quality_lines.extend(["", "## Baseline debt", "", "```json", json.dumps(metrics, indent=2, sort_keys=True), "```", ""])
    return json_text, "\n".join(lines) + "\n", "\n".join(quality_lines)


def write_or_check(path: Path, content: str, check: bool) -> bool:
    current = path.read_text(encoding="utf-8") if path.exists() else None
    if current == content:
        return True
    if check:
        print(f"stale generated file: {path.relative_to(ROOT).as_posix()}", file=sys.stderr)
        return False
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(content, encoding="utf-8", newline="\n")
    return True


def cmd_validate(args: argparse.Namespace) -> int:
    errors, warnings, metrics = audit()
    for item in errors:
        print(f"ERROR {item}")
    for item in warnings:
        print(f"WARN  {item}")
    print(json.dumps(metrics, sort_keys=True))

    if args.snapshot:
        target = Path(args.snapshot)
        if not target.is_absolute():
            target = ROOT / target
        target.parent.mkdir(parents=True, exist_ok=True)
        debt_metrics = {key: value for key, value in metrics.items() if key != "skill_files"}
        target.write_text(json.dumps({"schemaVersion": 1, "maximums": debt_metrics}, indent=2, sort_keys=True) + "\n", encoding="utf-8")

    baseline_failed = False
    if args.baseline:
        baseline_path = Path(args.baseline)
        if not baseline_path.is_absolute():
            baseline_path = ROOT / baseline_path
        maximums = json.loads(baseline_path.read_text(encoding="utf-8"))["maximums"]
        for key, maximum in maximums.items():
            actual = metrics.get(key, 0)
            if actual > int(maximum):
                print(f"ERROR quality regression: {key}={actual} exceeds baseline {maximum}")
                baseline_failed = True
    return 1 if errors or baseline_failed or (args.fail_on_warnings and warnings) else 0


def cmd_catalog(args: argparse.Namespace) -> int:
    skills = discover()
    json_text, md_text, quality_text = render_catalog(skills)
    ok = write_or_check(CATALOG_JSON, json_text, args.check)
    ok = write_or_check(CATALOG_MD, md_text, args.check) and ok
    ok = write_or_check(QUALITY_MD, quality_text, args.check) and ok
    print(f"catalog: {len(skills)} skill files, {len({skill.name for skill in skills})} unique names")
    return 0 if ok else 1


def cmd_search(args: argparse.Namespace) -> int:
    skills = discover()
    query = " ".join(args.query).strip().lower()
    by_name: defaultdict[str, list[Skill]] = defaultdict(list)
    for skill in skills:
        by_name[skill.name].append(skill)
    canonical = {
        name: min(members, key=lambda item: (item.path.count("/"), len(item.path), item.path)).path
        for name, members in by_name.items()
    }
    ranked: list[tuple[int, Skill]] = []
    for skill in skills:
        if args.domain and args.domain not in skill.domains:
            continue
        if not args.all and skill.path != canonical[skill.name]:
            continue
        haystack = f"{skill.name} {skill.description} {skill.path}".lower()
        if query and query not in haystack:
            continue
        score = 0
        if query:
            if skill.name == query:
                score += 100
            elif query in skill.name:
                score += 50
            elif query in skill.path.lower():
                score += 20
            else:
                score += 10
        score += skill.quality // 10
        ranked.append((score, skill))
    for _, skill in sorted(ranked, key=lambda item: (-item[0], item[1].name, item[1].path))[: args.limit]:
        print(f"{skill.name}\t{skill.path}\t{','.join(skill.domains)}\tq={skill.quality}")
    return 0 if ranked else 1


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(description=__doc__)
    sub = parser.add_subparsers(dest="command", required=True)
    validate = sub.add_parser("validate", help="audit skill structure and links")
    validate.add_argument("--baseline", help="fail when a metric exceeds a saved baseline")
    validate.add_argument("--snapshot", help="write current metrics as a baseline")
    validate.add_argument("--fail-on-warnings", action="store_true")
    validate.set_defaults(func=cmd_validate)
    catalog = sub.add_parser("catalog", help="generate the JSON and Markdown catalogs")
    catalog.add_argument("--check", action="store_true", help="fail instead of rewriting stale output")
    catalog.set_defaults(func=cmd_catalog)
    search = sub.add_parser("search", help="find canonical skills by text and domain")
    search.add_argument("query", nargs="*", help="text to match in name, description, or path")
    search.add_argument("--domain", help="restrict to a domain id from config/domains.json")
    search.add_argument("--all", action="store_true", help="include duplicate non-canonical paths")
    search.add_argument("--limit", type=int, default=25)
    search.set_defaults(func=cmd_search)
    return parser


def main(argv: Iterable[str] | None = None) -> int:
    args = build_parser().parse_args(argv)
    return args.func(args)


if __name__ == "__main__":
    raise SystemExit(main())
