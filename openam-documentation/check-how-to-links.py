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

"""Check How-To xrefs against generated AsciiDoc, including service reference IDs."""

from pathlib import Path
import re
import sys


def main():
    source = (Path(sys.argv[1]) if len(sys.argv) > 1 else
              Path(__file__).parent / "openam-doc-source/target/asciidoc/source")
    articles = sorted((source / "how-to").glob("*.adoc"))
    if not articles:
        print(f"No generated How-To pages found in {source}; build the documentation first.")
        return 1

    errors = []
    checked = 0
    for article in articles:
        for match in re.finditer(r"xref:([^\s\[]+)\[", article.read_text(encoding="utf-8")):
            target = match.group(1)
            # Antora creates the component landing page; it has no source file here.
            if target == "ROOT:index.adoc":
                continue
            page, _, anchor = target.partition("#")
            checked += 1
            if anchor.startswith("++") and anchor.endswith("++"):
                anchor = anchor[2:-2]
            elif re.search(r"\w--\w", anchor):
                # Asciidoctor turns an intraword double hyphen into an em dash.
                errors.append(f"{article.name}: fragment needs ++...++: {target}")
                continue
            destination = article.parent / page if page else article
            if not destination.is_file():
                errors.append(f"{article.name}: missing page: {target}")
                continue
            if anchor:
                text = destination.read_text(encoding="utf-8")
                # Comment, listing and literal blocks do not define anchors.
                text = re.sub(r"^(/{4,}|-{4,}|\.{4,})[ \t]*\n.*?^\1[ \t]*$", "",
                              text, flags=re.MULTILINE | re.DOTALL)
                ids = re.findall(r"^\[\[([^\],]+)", text, re.MULTILINE)
                # In shorthand attributes, a dot starts a role and % starts an option.
                ids += re.findall(r"^\[#([^\].,%]+)", text, re.MULTILINE)
                if anchor not in ids:
                    errors.append(f"{article.name}: broken anchor: {target}")

    for error in errors:
        print(error)
    print(f"Checked {checked} How-To xrefs in {len(articles)} pages; {len(errors)} errors.")
    return 1 if errors else 0


if __name__ == "__main__":
    sys.exit(main())
