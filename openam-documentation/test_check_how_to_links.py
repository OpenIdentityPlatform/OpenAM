# The contents of this file are subject to the terms of the Common Development and
# Distribution License (the License). You may not use this file except in compliance with the
# License.
#
# You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
# specific language governing permission and limitations under the License.
#
# When distributing Covered Software, include this CDDL Header Notice in each file and include
# the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
# Header, with the fields enclosed by brackets [] replaced by your own identifying
# information: "Portions copyright [year] [name of copyright owner]".
#
# Copyright 2026 3A Systems, LLC.

"""Regression tests for How-To link validation."""

from pathlib import Path
import subprocess
import sys
import tempfile
import unittest


CHECKER = Path(__file__).with_name("check-how-to-links.py")


class HowToLinksTest(unittest.TestCase):
    def check_link(self, anchor, definition):
        with tempfile.TemporaryDirectory() as directory:
            source = Path(directory)
            (source / "how-to").mkdir()
            (source / "reference").mkdir()
            (source / "how-to/page.adoc").write_text(
                f"xref:../reference/module.adoc#{anchor}[Module]\n", encoding="utf-8")
            (source / "reference/module.adoc").write_text(definition, encoding="utf-8")
            return subprocess.run([sys.executable, str(CHECKER), str(source)],
                                  capture_output=True, text=True)

    def test_double_hyphen_between_word_characters_needs_passthrough(self):
        result = self.check_link("adaptive-risk--module-ref", "[[adaptive-risk--module-ref]]\n")
        self.assertEqual(1, result.returncode, result.stdout + result.stderr)
        self.assertIn("fragment needs ++...++", result.stdout)
        self.assertIn("Checked 1 How-To xrefs", result.stdout)

    def test_passthrough_preserves_double_hyphen(self):
        result = self.check_link("++adaptive-risk--module-ref++", "[[adaptive-risk--module-ref]]\n")
        self.assertEqual(0, result.returncode, result.stdout + result.stderr)

    def test_double_hyphen_outside_words_does_not_need_passthrough(self):
        result = self.check_link("--module-ref", "[[--module-ref]]\n")
        self.assertEqual(0, result.returncode, result.stdout + result.stderr)

    def test_anchors_inside_verbatim_blocks_are_not_destinations(self):
        for delimiter in ("////", "----", "...."):
            for definition in ("[[hidden]]", "[#hidden]"):
                with self.subTest(delimiter=delimiter, definition=definition):
                    result = self.check_link("hidden", f"{delimiter}\n{definition}\n{delimiter}\n")
                    self.assertEqual(1, result.returncode, result.stdout + result.stderr)
                    self.assertIn("broken anchor", result.stdout)

    def test_anchor_after_verbatim_block_is_a_destination(self):
        result = self.check_link("visible", "----\n[[hidden]]\n----\n[#visible]\n")
        self.assertEqual(0, result.returncode, result.stdout + result.stderr)

    def test_block_closes_only_on_its_own_delimiter(self):
        for definition in ("-----\n----\n[[hidden]]\ntext\n-----\n",
                           "----\n....\n[[hidden]]\ntext\n----\n"):
            with self.subTest(definition=definition):
                result = self.check_link("hidden", definition)
                self.assertEqual(1, result.returncode, result.stdout + result.stderr)
                self.assertIn("broken anchor", result.stdout)

    def test_anchor_between_two_verbatim_blocks_is_a_destination(self):
        result = self.check_link("visible", "----\na\n----\n\n[[visible]]\ntext\n\n----\nb\n----\n")
        self.assertEqual(0, result.returncode, result.stdout + result.stderr)

    def test_longer_and_padded_delimiters_open_and_close_blocks(self):
        for definition in ("-----\n[[hidden]]\ntext\n-----\n",
                           "----\n[[hidden]]\ntext\n----  \n",
                           "---- \n[[hidden]]\ntext\n----\t\n"):
            with self.subTest(definition=repr(definition)):
                result = self.check_link("hidden", definition)
                self.assertEqual(1, result.returncode, result.stdout + result.stderr)
                self.assertIn("broken anchor", result.stdout)


if __name__ == "__main__":
    unittest.main()
