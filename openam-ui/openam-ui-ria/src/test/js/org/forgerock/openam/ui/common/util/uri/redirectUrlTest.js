/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2026 3A Systems, LLC.
 */

define([
    "org/forgerock/openam/ui/common/util/uri/redirectUrl"
], (redirectUrl) => {
    const BASE = "https://openam.example.com/openam/XUI/";
    // Built rather than written out, so that no linter takes the test data for a script URL.
    const SCRIPT_URL = `${"javascript"}:alert(document.domain)`;

    describe("org/forgerock/openam/ui/common/util/uri/redirectUrl", () => {
        describe("#isHttpUrl", () => {
            it("accepts an absolute http(s) URL of any origin", () => {
                expect(redirectUrl.isHttpUrl("https://app.example.com/", BASE)).to.be.true;
                expect(redirectUrl.isHttpUrl("http://app.example.com/", BASE)).to.be.true;
            });

            it("accepts a relative URL, which stays on the page's scheme", () => {
                expect(redirectUrl.isHttpUrl("/openam/console", BASE)).to.be.true;
            });

            it("refuses a script URL, whatever its case or leading whitespace", () => {
                expect(redirectUrl.isHttpUrl(SCRIPT_URL, BASE)).to.be.false;
                expect(redirectUrl.isHttpUrl(SCRIPT_URL.toUpperCase(), BASE)).to.be.false;
                expect(redirectUrl.isHttpUrl(` \t${SCRIPT_URL}`, BASE)).to.be.false;
            });

            it("refuses other schemes", () => {
                expect(redirectUrl.isHttpUrl("data:text/html,<script>alert(1)</script>", BASE)).to.be.false;
                expect(redirectUrl.isHttpUrl("vbscript:msgbox(1)", BASE)).to.be.false;
            });

            it("refuses what is not a string", () => {
                expect(redirectUrl.isHttpUrl(undefined, BASE)).to.be.false;
                expect(redirectUrl.isHttpUrl(null, BASE)).to.be.false;
            });
        });

        describe("#isSameOriginUrl", () => {
            it("accepts a relative path", () => {
                expect(redirectUrl.isSameOriginUrl("/openam/XUI/#profile/", BASE)).to.be.true;
            });

            it("accepts an absolute URL of the page's own origin", () => {
                expect(redirectUrl.isSameOriginUrl("https://openam.example.com/app", BASE)).to.be.true;
            });

            it("refuses another origin", () => {
                expect(redirectUrl.isSameOriginUrl("https://evil.example/", BASE)).to.be.false;
                expect(redirectUrl.isSameOriginUrl("http://openam.example.com/", BASE)).to.be.false;
                expect(redirectUrl.isSameOriginUrl("https://openam.example.com:8443/", BASE)).to.be.false;
            });

            it("refuses the relative forms a browser resolves to another origin", () => {
                expect(redirectUrl.isSameOriginUrl("//evil.example/", BASE)).to.be.false;
                expect(redirectUrl.isSameOriginUrl("/\\evil.example/", BASE)).to.be.false;
                expect(redirectUrl.isSameOriginUrl("\\\\evil.example/", BASE)).to.be.false;
                expect(redirectUrl.isSameOriginUrl("https:evil.example/", "http://openam.example.com/")).to.be
                    .false;
            });

            it("refuses a script URL", () => {
                expect(redirectUrl.isSameOriginUrl(SCRIPT_URL, BASE)).to.be.false;
            });

            it("refuses userinfo that only looks like this server", () => {
                expect(redirectUrl.isSameOriginUrl("https://openam.example.com@evil.example/", BASE)).to.be.false;
            });

            it("judges against the page's own location when no base is given", () => {
                expect(redirectUrl.isSameOriginUrl(`${window.location.origin}/openam/`)).to.be.true;
                expect(redirectUrl.isSameOriginUrl("https://evil.example/")).to.be.false;
            });
        });
    });
});
