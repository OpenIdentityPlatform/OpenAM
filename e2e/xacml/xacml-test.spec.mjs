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
 * Characterization tests for the /xacml policy export and import endpoints, recorded against the
 * Restlet application that serves them. This is the only layer that exercises the real
 * authentication filter, the real delegation check and the real XML serialization together.
 *
 * The tests pin the black-box contract so that moving /xacml off Restlet can be checked for parity:
 * a test that passes unchanged after the move is the proof, and an expectation the move changes has
 * to be a deliberate divergence listed in that change. Where the current behaviour is a defect, the
 * test pins what the server does today and its comment names the target behaviour.
 */

import { test, expect, request as apiRequest } from "@playwright/test";
import { OPENAM_BASE, ADMIN_PASS, ADMIN_USER, getAuthToken, PASSWORD, USERNAME } from "../common/openam-commons.mjs";

// Unique per run: the dryrun case needs a realm that has never held the fixture policy.
const SUB_REALM = `xacmltest${Date.now().toString(36)}`;
const POLICY_NAME = "xacml-e2e-policy";
// A second root policy, so that a filter on POLICY_NAME has something to filter out.
const OTHER_POLICY_NAME = "xacml-e2e-policy-other";
const POLICY_DESCRIPTION = "Fixture policy owned by the XACML e2e spec";
const APPLICATION_NAME = "iPlanetAMWebAgentService";

let adminToken;
let demoToken;

/**
 * Authenticates in a throwaway context so the session cookie dies with it.
 *
 * /json/authenticate sets an iPlanetDirectoryPro cookie, and LocalSSOTokenSessionModule reads the
 * cookie before falling back to the header of the same name. Authenticating on the shared `request`
 * fixture would therefore leave a cookie that silently outranks every explicit token header this
 * spec sends: logging in as demo would downgrade the admin calls to demo (403), and any cookie at
 * all would stop the unauthenticated case from being unauthenticated. Every request below is
 * expected to carry exactly the identity named in its header, so the jar has to stay empty.
 */
async function tokenFor(username, password) {
  const context = await apiRequest.newContext();
  try {
    return await getAuthToken(context, username, password);
  } finally {
    await context.dispose();
  }
}

/**
 * Every PolicyId in an exported PolicySet. XACMLPrivilegeUtils.privilegeNameToPolicyId returns the
 * privilege name unchanged, so these are the policy names. Anchored so PolicySetId does not match.
 */
function policyIds(xml) {
  return [...xml.matchAll(/\bPolicyId="([^"]+)"/g)].map((match) => match[1]);
}

/**
 * Asserts that an error came back as the XML error document /xacml answers with instead of JSON.
 * The embedded code is passed in rather than taken from the response, so that a body whose code
 * disagrees with the HTTP status is caught as well.
 */
async function expectXmlError(response, expectedEmbeddedCode) {
  expect(response.headers()["content-type"]).toBe("application/xml;charset=UTF-8");
  const body = await response.text();
  expect(body).toContain("<error>");
  const code = body.match(/<code>(\d+)<\/code>/);
  expect(code, `expected an <error><code> element, got: ${body}`).not.toBeNull();
  expect(Number(code[1])).toBe(expectedEmbeddedCode);
}

function exportPolicies(request, { path = "/xacml/policies", token = adminToken, headers = {}, params } = {}) {
  const options = { headers: { ...headers } };
  if (token) {
    options.headers["iPlanetDirectoryPro"] = token;
  }
  if (params) {
    options.params = params;
  }
  return request.get(`${OPENAM_BASE}${path}`, options);
}

function importPolicies(request, body, params) {
  const options = {
    headers: { "iPlanetDirectoryPro": adminToken, "Content-Type": "application/xml" },
    data: body,
  };
  if (params) {
    options.params = params;
  }
  return request.post(`${OPENAM_BASE}/xacml/policies`, options);
}

async function exportedPolicyIds(request, realmPath) {
  const response = await exportPolicies(request, { path: `/xacml/${realmPath}/policies` });
  expect(response.status()).toBe(200);
  return policyIds(await response.text());
}

/**
 * Needed by the realm-style and import cases, and gives the non-root Content-Disposition filename
 * something to be derived from. The name is unique per run, so any failure is a real one.
 *
 * Realms are created through global-config, not through /json/realms/{realm}/realms: the latter is
 * the deprecated RealmResource, which takes the name in a "realm" field and rejects this body with
 * "No realm name provided". global-config is what the XUI console itself uses, and is where
 * SmsRealmProvider is actually bound (SmsRequestHandler.addRealmHandler, SchemaType.GLOBAL).
 *
 * aliases is not optional: SmsRealmProvider.validateRealmAliases rejects a body without it.
 */
async function createSubRealm(request) {
  const response = await request.post(`${OPENAM_BASE}/json/global-config/realms?_action=create`, {
    headers: {
      "iPlanetDirectoryPro": adminToken,
      "Content-Type": "application/json",
      "Accept-API-Version": "protocol=1.0,resource=1.0",
    },
    data: { name: SUB_REALM, parentPath: "/", active: true, aliases: [] },
  });

  if (!response.ok()) {
    throw new Error(`Failed to create realm "${SUB_REALM}": ${response.status()} ${await response.text()}`);
  }
  console.log(`Realm "${SUB_REALM}" created`);
}

/**
 * A fresh install's root realm holds no policies, so without these every export would return an
 * empty PolicySet and the filter and import cases would pass while proving nothing. Tolerates an
 * existing policy so the suite can be re-run against a live server.
 *
 * Uses resource=1.0 deliberately: PolicyV1Filter derives resourceTypeUuid from applicationName, so
 * the body does not have to name the realm's generated resource type. Later versions require it.
 */
async function ensurePolicyExists(request, name) {
  const url = `${OPENAM_BASE}/json/realms/root/policies/${name}`;
  const headers = {
    "iPlanetDirectoryPro": adminToken,
    "Content-Type": "application/json",
    "Accept-API-Version": "resource=1.0",
  };

  const existing = await request.get(url, { headers });
  if (existing.ok()) {
    console.log(`Policy "${name}" already exists`);
    return;
  }
  if (existing.status() !== 404) {
    throw new Error(`Failed to check policy "${name}": ${existing.status()} ${await existing.text()}`);
  }

  const created = await request.put(url, {
    headers: { ...headers, "If-None-Match": "*" },
    data: {
      name,
      active: true,
      description: POLICY_DESCRIPTION,
      applicationName: APPLICATION_NAME,
      actionValues: { GET: true },
      resources: [`http://${name}.example.com:8080/*`],
      subject: { type: "AuthenticatedUsers" },
    },
  });

  if (!created.ok()) {
    throw new Error(`Failed to create policy "${name}": ${created.status()} ${await created.text()}`);
  }
  console.log(`Policy "${name}" created`);
}

test.beforeAll(async ({ request }) => {
  // Asserted rather than skipped on: a failed login must turn the file red, not into a run of
  // skipped tests, and an undefined token would make exportPolicies fall back to the admin token.
  adminToken = await tokenFor(ADMIN_USER, ADMIN_PASS);
  expect(adminToken, "admin login failed").toBeTruthy();
  demoToken = await tokenFor(USERNAME, PASSWORD);
  expect(demoToken, "demo login failed").toBeTruthy();

  await createSubRealm(request);
  await ensurePolicyExists(request, POLICY_NAME);
  await ensurePolicyExists(request, OTHER_POLICY_NAME);
});

test.describe("XACML policy export", () => {
  test("exports the root realm as an XACML 3.0 attachment", async ({ request }) => {
    const response = await exportPolicies(request);

    expect(response.status()).toBe(200);
    expect(response.headers()["content-type"]).toBe("application/xacml+xml; version=3.0");
    expect(response.headers()["content-disposition"]).toBe("attachment; filename=realm-policies.xml");

    const body = await response.text();
    expect(body).toMatch(/<(\w+:)?PolicySet[\s>]/);
    // If either fixture is missing here, the filter and import cases below are vacuous.
    expect(policyIds(body)).toEqual(expect.arrayContaining([POLICY_NAME, OTHER_POLICY_NAME]));
  });

  test("answers an unauthenticated export with 500", async ({ request }) => {
    const response = await exportPolicies(request, { token: null });

    // Defect; the target is 401 with <code>401. The /xacml authentication filter admits a request
    // without a session, and the handler then fails building an SSO token from a null id.
    expect(response.status()).toBe(500);
    await expectXmlError(response, 500);
  });

  test("rejects an export by a user without delegation permission with 403", async ({ request }) => {
    const response = await exportPolicies(request, { token: demoToken });

    expect(response.status()).toBe(403);
    await expectXmlError(response, 403);
  });

  test("answers 405 to PUT", async ({ request }) => {
    const response = await request.put(`${OPENAM_BASE}/xacml/policies`, {
      headers: { "iPlanetDirectoryPro": adminToken },
      data: "",
    });

    // The body is the framework's generic page, so only the status and Allow are pinned.
    expect(response.status()).toBe(405);
    expect(response.headers()["allow"]?.split(/\s*,\s*/).sort()).toEqual(["GET", "POST"]);
  });
});

/*
 * /xacml/policies exists in resource version 1.0 only. A request without Accept-API-Version gets the
 * latest version, so XACML clients that never send the header (ssoadm and the CLI-era exporters)
 * keep working, while a version that does not exist is not served.
 */
test.describe("XACML export versioning", () => {
  test("exports when no Accept-API-Version header is sent", async ({ request }) => {
    const response = await exportPolicies(request);

    expect(response.status()).toBe(200);
  });

  test("answers a known Accept-API-Version with Content-API-Version", async ({ request }) => {
    const response = await exportPolicies(request, { headers: { "Accept-API-Version": "resource=1.0" } });

    expect(response.status()).toBe(200);
    // Defect in the value; the target is "protocol=1.0,resource=1.0", the form /json sends. The
    // version filter writes the header object's string form, header name included, as the value.
    expect(response.headers()["content-api-version"])
      .toBe("[Content-API-Version: protocol=1.0,resource=1.0]");
  });

  test("answers 404 for a resource version that does not exist", async ({ request }) => {
    const response = await exportPolicies(request, { headers: { "Accept-API-Version": "resource=2.0" } });

    // The body is the framework's generic not-found page, so only the status is pinned.
    expect(response.status()).toBe(404);
  });
});

/*
 * Runs before the import cases, so SUB_REALM holds no policy yet while root always holds POLICY_NAME:
 * the content check tells the sub realm's export from root's, whatever the filename says.
 */
test.describe("XACML export realm styles", () => {
  test("answers 404 to the realms/root path spelling", async ({ request }) => {
    const response = await exportPolicies(request, { path: "/xacml/realms/root/policies" });

    // Defect; the target is 200 with the root realm's realm-policies.xml attachment, as /json
    // accepts this spelling. The /xacml realm router takes every segment before /policies as a
    // sub-realm name, so it looks up a realm called "realms" and does not find it.
    expect(response.status()).toBe(404);
  });

  test("resolves a legacy path realm", async ({ request }) => {
    const response = await exportPolicies(request, { path: `/xacml/${SUB_REALM}/policies` });

    expect(response.status()).toBe(200);
    expect(response.headers()["content-disposition"])
      .toBe(`attachment; filename=${SUB_REALM}-realm-policies.xml`);
    expect(policyIds(await response.text())).not.toContain(POLICY_NAME);
  });

  test("honours the realm query parameter", async ({ request }) => {
    const response = await exportPolicies(request, { params: { realm: `/${SUB_REALM}` } });

    expect(response.status()).toBe(200);
    expect(response.headers()["content-disposition"])
      .toBe(`attachment; filename=${SUB_REALM}-realm-policies.xml`);
    expect(policyIds(await response.text())).not.toContain(POLICY_NAME);
  });
});

test.describe("XACML export filters", () => {
  test("restricts the export to a matching filter", async ({ request }) => {
    const query = new URLSearchParams();
    query.append("filter", `name=${POLICY_NAME}`);

    const response = await request.get(`${OPENAM_BASE}/xacml/policies?${query}`, {
      headers: { "iPlanetDirectoryPro": adminToken },
    });

    expect(response.status()).toBe(200);
    // name=X is an equality match, so OTHER_POLICY_NAME, which starts with POLICY_NAME, stays out.
    expect(policyIds(await response.text())).toEqual([POLICY_NAME]);
  });

  test("applies every value of a repeated filter parameter", async ({ request }) => {
    // Filters are ANDed, so a second, unsatisfiable filter must empty the result. Both orders are
    // sent: reading only the first value, or only the last, would leave the policy in one of them.
    for (const values of [[`name=${POLICY_NAME}`, "name=no-such-policy"], ["name=no-such-policy", `name=${POLICY_NAME}`]]) {
      const query = new URLSearchParams();
      values.forEach((value) => query.append("filter", value));

      const response = await request.get(`${OPENAM_BASE}/xacml/policies?${query}`, {
        headers: { "iPlanetDirectoryPro": adminToken },
      });

      expect(response.status()).toBe(200);
      expect(policyIds(await response.text()), `filters ${values.join(" & ")}`).toEqual([]);
    }
  });
});

test.describe("XACML policy import", () => {
  // Runs before the round-trip case, which imports the same policy into the same realm for real.
  test("reports the steps of a dryrun without persisting them", async ({ request }) => {
    const exported = await (await exportPolicies(request)).text();
    expect(await exportedPolicyIds(request, SUB_REALM)).not.toContain(POLICY_NAME);

    const response = await importPolicies(request, exported, { realm: `/${SUB_REALM}`, dryrun: "true" });

    expect(response.status()).toBe(200);
    expect(response.headers()["content-type"]).toBe("application/json");
    const steps = await response.json();
    expect(steps).toContainEqual({ name: POLICY_NAME, type: "Privilege", status: "A" });

    expect(await exportedPolicyIds(request, SUB_REALM)).not.toContain(POLICY_NAME);
  });

  test("round-trips an export from the root realm into a sub realm", async ({ request }) => {
    const exported = await (await exportPolicies(request)).text();

    const response = await importPolicies(request, exported, { realm: `/${SUB_REALM}` });

    expect(response.status()).toBe(200);
    const steps = await response.json();
    expect(steps.length).toBeGreaterThan(0);

    // The load-bearing assertion of the suite: export serialization, import parsing, both realm
    // resolutions and both permission checks all have to be right for the policy to land here.
    expect(await exportedPolicyIds(request, SUB_REALM)).toContain(POLICY_NAME);
  });

  test("answers an empty document with 500", async ({ request }) => {
    const response = await importPolicies(request, "");

    // Defect; the target is 400 with <code>400, as for a document that holds no policies. The empty
    // body reaches the import handler as a null entity and fails with a NullPointerException.
    expect(response.status()).toBe(500);
    await expectXmlError(response, 500);
  });
});
