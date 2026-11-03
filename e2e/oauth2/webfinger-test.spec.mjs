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

/*
 * Cover for /.well-known/webfinger (OIDC issuer discovery, RFC 7033).
 *
 * /.well-known/* has a servlet of its own in web.xml: the in-repo Restlet ServerServlet running the WebFinger
 * application. It answers since the servlet swap in #1201.
 *
 * The issuer answer does not depend on whether the account exists, because otherwise anyone could check which
 * accounts the realm holds.
 *
 * The spec pins the contract before the flip to CHF.
 */

import { test, expect } from "@playwright/test";
import { OPENAM_BASE, USERNAME } from "../common/openam-commons.mjs";

const ISSUER_REL = "http://openid.net/specs/connect/1.0/issuer";
const HOST = new URL(OPENAM_BASE).hostname;
const JSON_TYPE = "application/json;charset=UTF-8";
// The discovery resource renders its own 400 answers without a charset; every other answer carries one.
const JSON_TYPE_NO_CHARSET = "application/json";


// Parse via text, not response.json(): a stray HTML error page should fail as a readable diff naming the
// status and content-type, not as a bare SyntaxError.
async function readJson(response) {
  const text = await response.text();
  try {
    return JSON.parse(text);
  } catch {
    throw new Error(`expected JSON, got ${response.status()} `
      + `${response.headers()["content-type"]}: ${text.slice(0, 200)}`);
  }
}

/** The JRD WebFinger answers for any account: the subject echoed and one issuer link. */
function issuerJrd(resource) {
  return { subject: resource, links: [{ rel: ISSUER_REL, href: `${OPENAM_BASE}/oauth2` }] };
}

/** Pins the status, the Content-Type and the whole JSON body of an answer. */
async function expectJson(response, status, body, label, contentType = JSON_TYPE) {
  const json = await readJson(response);
  expect(response.status(), label).toBe(status);
  expect(response.headers()["content-type"], label).toBe(contentType);
  expect(json, label).toEqual(body);
}

test.describe("/.well-known/webfinger", () => {

  test("answers a JRD naming the issuer, with or without ?realm=/", async ({ request }) => {
    const resource = `acct:${USERNAME}@${HOST}`;
    const cases = { "no realm": {}, "realm=/": { realm: "/" } };
    for (const [name, extra] of Object.entries(cases)) {
      const response = await request.get(`${OPENAM_BASE}/.well-known/webfinger`, {
        params: { resource, rel: ISSUER_REL, ...extra },
      });
      await expectJson(response, 200, issuerJrd(resource), name);
    }
  });

  test("answers the same for an account that does not exist", async ({ request }) => {
    // The answer must not depend on the account, otherwise anyone could probe which accounts the realm holds.
    const resource = `acct:no-such-account@${HOST}`;
    const response = await request.get(`${OPENAM_BASE}/.well-known/webfinger`, {
      params: { resource, rel: ISSUER_REL },
    });
    await expectJson(response, 200, issuerJrd(resource));
  });

  test("answers 400 bad_request to a missing resource", async ({ request }) => {
    const response = await request.get(`${OPENAM_BASE}/.well-known/webfinger`, {
      params: { rel: ISSUER_REL },
    });
    await expectJson(response, 400,
      { error: "bad_request", error_description: "No resource provided in discovery." },
      "no resource", JSON_TYPE_NO_CHARSET);
  });

  test("answers 400 bad_request to a missing or wrong rel", async ({ request }) => {
    // Two inputs, one message: absent and non-issuer rel are treated identically, and the wrong-rel leg
    // is the one that proves the value is compared rather than merely required.
    const resource = `acct:${USERNAME}@${HOST}`;
    const cases = {
      "no rel": { resource },
      "wrong rel": { resource, rel: "http://example.com/not-the-issuer-rel" },
    };
    for (const [name, params] of Object.entries(cases)) {
      const response = await request.get(`${OPENAM_BASE}/.well-known/webfinger`, { params });
      await expectJson(response, 400,
        { error: "bad_request", error_description: "No or invalid rel provided in discovery." },
        name, JSON_TYPE_NO_CHARSET);
    }
  });

  test("answers 400 bad_request to a resource that is neither an account nor a URL", async ({ request }) => {
    // An acct: resource needs an @, and a URL resource must be a non-opaque http or https URI.
    const cases = {
      "acct without @": `acct:${USERNAME}`,
      "other scheme": `mailto:${USERNAME}@${HOST}`,
      "opaque http": `http:${USERNAME}@${HOST}`,
    };
    for (const [name, resource] of Object.entries(cases)) {
      const response = await request.get(`${OPENAM_BASE}/.well-known/webfinger`, {
        params: { resource, rel: ISSUER_REL },
      });
      await expectJson(response, 400, { error: "bad_request", error_description: "Invalid parameters." },
        name, JSON_TYPE_NO_CHARSET);
    }
  });

  test("answers a JRD to a URL resource", async ({ request }) => {
    const resource = `${OPENAM_BASE}/`;
    const response = await request.get(`${OPENAM_BASE}/.well-known/webfinger`, {
      params: { resource, rel: ISSUER_REL },
    });
    await expectJson(response, 200, issuerJrd(resource));
  });

  test("compares rel case-insensitively and echoes it as sent", async ({ request }) => {
    const resource = `acct:${USERNAME}@${HOST}`;
    const rel = "HTTP://OpenID.net/specs/connect/1.0/Issuer";
    const response = await request.get(`${OPENAM_BASE}/.well-known/webfinger`, { params: { resource, rel } });
    await expectJson(response, 200, { subject: resource, links: [{ rel, href: `${OPENAM_BASE}/oauth2` }] });
  });

  test("answers 404 to the realms/root path spelling", async ({ request }) => {
    // RestletRealmRouter takes `realms` as a sub-realm name, so this spelling is a 404 today. The target is 200
    // with the same JRD as the bare path, as for /xacml/realms/root in xacml-test.spec.mjs; a flip changes this.
    const response = await request.get(`${OPENAM_BASE}/.well-known/realms/root/webfinger`, {
      params: { resource: `acct:${USERNAME}@${HOST}`, rel: ISSUER_REL },
    });
    expect(response.status()).toBe(404);
  });

  test("answers 404 to an unrouted child of /.well-known", async ({ request }) => {
    // Framework body: only the status is pinned.
    const response = await request.get(`${OPENAM_BASE}/.well-known/nonsense`);
    expect(response.status()).toBe(404);
  });

  test("answers 404 for a realm that does not exist", async ({ request }) => {
    // The realm router answers before discovery runs. Contract as it stands, with no other target named.
    // An unknown realm silently falling back to the root realm would show up as a 200 JRD here.
    const response = await request.get(`${OPENAM_BASE}/.well-known/webfinger`, {
      params: { resource: `acct:${USERNAME}@${HOST}`, rel: ISSUER_REL, realm: "/bogus" },
    });
    await expectJson(response, 404, { error: "Not Found", error_description: 'Realm "/bogus" not found' });
  });

  test("answers 405 with Allow: GET to POST", async ({ request }) => {
    // RFC 7033 defines GET only, and the discovery resource has only a GET handler. A POST dispatched into it,
    // or a 405 without Allow, would break this.
    const response = await request.post(`${OPENAM_BASE}/.well-known/webfinger`, {
      params: { resource: `acct:${USERNAME}@${HOST}`, rel: ISSUER_REL, realm: "/" },
    });
    expect(response.status()).toBe(405);
    expect(response.headers()["allow"]).toBe("GET");
    expect(response.headers()["content-type"]).toBe(JSON_TYPE);
  });
});
