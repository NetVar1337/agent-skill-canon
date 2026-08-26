from __future__ import annotations

import importlib.util
import sys
import tempfile
import unittest
from pathlib import Path

MODULE_PATH = Path(__file__).resolve().parents[1] / "tools" / "canon.py"
SPEC = importlib.util.spec_from_file_location("canon", MODULE_PATH)
canon = importlib.util.module_from_spec(SPEC)
assert SPEC and SPEC.loader
sys.modules[SPEC.name] = canon
SPEC.loader.exec_module(canon)


class FrontmatterTests(unittest.TestCase):
    def write_skill(self, text: str) -> Path:
        directory = tempfile.TemporaryDirectory()
        self.addCleanup(directory.cleanup)
        path = Path(directory.name) / "SKILL.md"
        path.write_text(text, encoding="utf-8")
        return path

    def test_parses_scalar_and_block_fields(self) -> None:
        path = self.write_skill(
            "---\nname: sample-skill\ndescription: |\n  Use when testing a sample.\nversion: 1.2.3\n---\n# Body\n"
        )
        meta, body = canon.parse_frontmatter(path)
        self.assertEqual(meta["name"], "sample-skill")
        self.assertEqual(meta["description"], "Use when testing a sample.")
        self.assertEqual(meta["version"], "1.2.3")
        self.assertIn("# Body", body)

    def test_rejects_missing_frontmatter(self) -> None:
        path = self.write_skill("# No metadata\n")
        with self.assertRaisesRegex(ValueError, "byte zero"):
            canon.parse_frontmatter(path)

    def test_detects_only_local_broken_links(self) -> None:
        path = self.write_skill(
            "---\nname: sample\ndescription: Use when testing links.\n---\n"
            "[good](https://example.com) [bad](references/missing.md)\n"
        )
        _, body = canon.parse_frontmatter(path)
        old_root = canon.ROOT
        self.addCleanup(setattr, canon, "ROOT", old_root)
        canon.ROOT = path.parent
        self.assertEqual(canon.broken_links(path, body), ["references/missing.md"])

    def test_classifies_across_multiple_domains(self) -> None:
        domains = [
            {"id": "game", "patterns": ["game"]},
            {"id": "windows", "patterns": ["windows"]},
        ]
        self.assertEqual(
            canon.classify("skills/game/SKILL.md", "windows-game", "", domains),
            ("game", "windows"),
        )


if __name__ == "__main__":
    unittest.main()
