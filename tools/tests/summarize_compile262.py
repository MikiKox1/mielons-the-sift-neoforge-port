#!/usr/bin/env python3
"""Summarize exact javac failures for the independent NeoForge26.2 backport."""
from __future__ import annotations

import csv
import json
import re
from collections import Counter
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
LOGS = ROOT / "logs"
OUT = ROOT / "evidence"


def categorize(path: str) -> str:
    if "/worldgen/" in path:
        return "worldgen-java"
    if "/client/" in path and ("render" in path or "mixin" in path):
        return "client-render-mixin"
    if "/client/" in path:
        return "client-other"
    if "/block/" in path or "/item/" in path:
        return "blocks-items"
    if "/entity/" in path:
        return "entity"
    if "/mixin/" in path:
        return "server-mixins"
    return "platform-other"


def main() -> None:
    log = LOGS / "phase2-compile-262-feature-api.log"
    data = log.read_text(encoding="utf-8", errors="replace")
    expression = re.compile(r"(?m)^(.*?\.java):(\d+): error: (.+)$")
    errors = []
    for m in expression.finditer(data):
        path = m.group(1).replace("\\", "/")
        tail = path.find("/src/")
        path = path[tail+1:] if tail >= 0 else path
        errors.append({
            "path": path,
            "line": int(m.group(2)),
            "category": categorize(path),
            "diagnostic": m.group(3).strip(),
        })
    assert errors, "No compilation diagnostics found"
    OUT.mkdir(parents=True, exist_ok=True)
    categories = dict(sorted(Counter(e["category"] for e in errors).items()))
    report = {
        "target": "MC26.2 NeoForge 26.2.0.90",
        "status": "COMPILE_FAIL",
        "source_log": str(log.relative_to(ROOT)).replace("\\", "/"),
        "errors_reported": len(errors),
        "compiler_limit": "First 100 per pass; output can be truncated; not exhaustive",
        "categories": categories,
        "unique_files": len(set(e["path"] for e in errors)),
        "first_error": errors[0],
        "next_gate": "Fix exact version-specific API and repeat clean build before server/client claims",
    }
    (OUT / "phase2-compile-issues.json").write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")
    with (OUT / "phase2-compile-issues.csv").open("w", newline="", encoding="utf-8-sig") as stream:
        writer = csv.DictWriter(stream, fieldnames=list(errors[0]))
        writer.writeheader()
        writer.writerows(errors)
    print(json.dumps(report, indent=2))


if __name__ == "__main__":
    main()
