"""No 26.2-only model parents may leak into the 26.1.2 production resource set."""
import json
from pathlib import Path
import unittest
from zipfile import ZipFile

ROOT = Path(__file__).resolve().parents[1]
GAME = ROOT / "build/moddev/artifacts/minecraft-patched-26.1.2.126.jar"
RES = ROOT / "src/main/resources"


def model_path(identifier):
    fields = identifier.split(":", 1)
    namespace, path = fields if len(fields) == 2 else ("minecraft", fields[0])
    return f"assets/{namespace}/models/{path}.json"


class TargetModelReferences2612(unittest.TestCase):
    def test_every_declared_model_parent_resolves_in_actual_target(self):
        with ZipFile(GAME) as game:
            target = set(game.namelist())
        models = {p.relative_to(RES).as_posix(): json.loads(p.read_text(encoding="utf8"))
                  for p in (RES / "assets/the_sift/models").rglob("*.json")}
        missing = []
        for name, data in models.items():
            parent = data.get("parent")
            if not parent or parent in ("minecraft:builtin/entity", "minecraft:builtin/generated",
                                        "builtin/entity", "builtin/generated"):
                continue
            dest = model_path(parent)
            if dest not in models and dest not in target:
                missing.append((name, parent))
        self.assertEqual(missing, [], str(missing[:8]))

    def test_blockstate_model_references_resolve(self):
        with ZipFile(GAME) as game:
            target = set(game.namelist())
        missing = []
        def walk(value, path):
            if isinstance(value, dict):
                if isinstance(value.get("model"), str):
                    name = model_path(value["model"])
                    if not (RES / name).is_file() and name not in target:
                        missing.append((str(path), value["model"]))
                for child in value.values():
                    walk(child, path)
            elif isinstance(value, list):
                for child in value:
                    walk(child, path)
        for path in (RES / "assets/the_sift/blockstates").glob("*.json"):
            walk(json.loads(path.read_text(encoding="utf8")), path)
        self.assertEqual(missing, [])


if __name__ == "__main__":
    unittest.main()
