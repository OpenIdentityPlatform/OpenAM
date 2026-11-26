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

"""Exercise the upgrade CLI from an unpacked distribution ZIP."""

from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
import subprocess
import sys
from tempfile import TemporaryDirectory
from threading import Thread
from urllib.parse import parse_qs, urlsplit
from zipfile import ZipFile


REDIRECT_TARGET = "/redirected-upgrade"


class UpgradeHandler(BaseHTTPRequestHandler):
    def do_GET(self):
        request = urlsplit(self.path)
        if request.path == "/openam/config/upgrade/upgrade.htm":
            self.server.upgrade_requests += 1
            if parse_qs(request.query) == {"actionLink": ["doUpgrade"], "acceptLicense": ["true"]}:
                status, body = self.server.upgrade_result
            else:
                status, body = 400, b"License not accepted"
        elif request.path == REDIRECT_TARGET:
            self.server.redirect_requests += 1
            status, body = 200, b"true"
        else:
            status, body = 200, b""
        self.send_response(status)
        if status == 302:
            self.send_header("Location", REDIRECT_TARGET)
        self.end_headers()
        self.wfile.write(body)

    def log_message(self, *_):
        pass


def check(command, directory, expected_success, label, answer=None):
    completed = subprocess.run(command, cwd=directory, input=answer, capture_output=True,
                               text=True, timeout=30)
    if (completed.returncode == 0) != expected_success:
        raise AssertionError(
            f"{label}: unexpected exit status {completed.returncode}\n"
            f"{completed.stdout}{completed.stderr}"
        )
    print(f"{label}: exit status {completed.returncode}", flush=True)


def main(archive):
    with TemporaryDirectory() as tmp, ZipFile(archive) as distribution:
        directory = Path(tmp)
        distribution.extractall(directory)
        jar, = directory.glob("openam-upgrade-tool-*.jar")
        command = ["java", "-jar", jar.name]
        check([*command, "--help"], directory, True, "packaged helper starts")

        with ThreadingHTTPServer(("127.0.0.1", 0), UpgradeHandler) as server:
            server.upgrade_requests = 0
            server.redirect_requests = 0
            worker = Thread(target=server.serve_forever, daemon=True)
            worker.start()
            try:
                properties = directory / "upgrade.properties"
                settings = f"SERVER_URL=http://127.0.0.1:{server.server_port}\nDEPLOYMENT_URI=/openam\n"
                properties.write_text(settings + "ACCEPT_LICENSES=true\n", encoding="ascii")
                cases = [
                    ("--file", 200, b"true", True),
                    ("-f", 200, b"true\n", True),
                    ("--f", 200, b"true", True),
                    ("--file", 200, b"false", False),
                    ("--file", 200, b"", False),
                    ("--file", 200, b"true\nfalse\n", False),
                    ("--file", 503, b"", False),
                ]
                for option, status, body, successful in cases:
                    server.upgrade_result = status, body
                    check([*command, option, "upgrade.properties"], directory, successful,
                          f"{option} HTTP {status} {body!r}")

                server.upgrade_result = 302, b""
                check([*command, "--file", "upgrade.properties"], directory, False, "upgrade redirect")
                if server.redirect_requests:
                    raise AssertionError("The CLI must not follow the upgrade redirect")

                properties.write_text(settings + "ACCEPT_LICENSES=false\n", encoding="ascii")
                server.upgrade_result = 200, b"true"
                interactive = [*command, "--file", "upgrade.properties"]
                check(interactive, directory, True, "interactive acceptance", answer="yes\n")
                requests = server.upgrade_requests
                check([*interactive, "--acceptLicense"], directory, False,
                      "properties file requires a prompt", answer="no\n")
                if server.upgrade_requests != requests:
                    raise AssertionError("License rejection must not reach the upgrade endpoint")

                properties.write_text(settings, encoding="ascii")
                check([*interactive, "--acceptLicense"], directory, True,
                      "command-line acceptance", answer="")
            finally:
                server.shutdown()
                worker.join()


if __name__ == "__main__":
    main(sys.argv[1])
