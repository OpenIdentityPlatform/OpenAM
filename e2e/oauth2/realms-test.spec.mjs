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
 * Protocol cover for /oauth2 in a non-root realm. On master Restlet serves all of /oauth2 (OAuth2RouterProvider
 * under the ForgeRockRest servlet), and two Restlet routers resolve the realm: RestletRealmRouter the Host's, the
 * query's and the legacy segment's, RealmRoutingFactory's router the /realms/... segments. This spec pins that
 * black-box contract before /oauth2 moves to CHF. A flip may change a pinned value only as a divergence listed in
 * its PR.
 *
 * Four spellings name the same realm:
 *   nested  /oauth2/realms/root/realms/<sub>/...  the form the realm's discovery document advertises;
 *   legacy  /oauth2/<sub>/...                     the /{subrealm} route;
 *   flat    /oauth2/realms/<sub>/...
 *   query   /oauth2/...?realm=<sub>               the query override.
 *
 * The realm carries a real deployment: its own provider, a confidential client and its own user. A realm created
 * over REST copies root's data store, so its user lives in root's directory and outlives the realm. Both are unique
 * per worker, and afterAll deletes the user, then the realm.
 */

import { request as httpRequest } from "node:http";
import { request as httpsRequest } from "node:https";
import { test, expect, request as apiRequest } from "@playwright/test";
import { OPENAM_BASE, PASSWORD, USERNAME, getAdminToken } from "../common/openam-commons.mjs";
import { REDIRECT_URI, basicAuth, sessionContext } from "../common/oauth2-fixtures.mjs";

const BASE = new URL(OPENAM_BASE);
const HTTPS = BASE.protocol === "https:";
const OAUTH2 = `${OPENAM_BASE}/oauth2`;

/** A failed test restarts the worker, which re-imports this file: each worker builds and removes its own realm. */
const SUFFIX = Date.now().toString(36);
const SUB_REALM = `oauth2realms${SUFFIX}`;
const REALM_CONFIG = `${OPENAM_BASE}/json/realms/root/realms/${SUB_REALM}`;
/** The realm's issuer, and the base of every endpoint it advertises, whichever spelling asked. */
const CANONICAL = `${OAUTH2}/realms/root/realms/${SUB_REALM}`;

const CLIENT_ID = "realms_test_client";
/** At least 256 bits: OpenAM signs HS256 id_tokens with the client secret. */
const CLIENT_SECRET = "realms-secret-0123456789-0123456789-0123456789";
const SCOPE = "profile";
const STATE = "realmstate";

/** Unique like the realm: it is stored in root's directory, where a fixed name answers 409 on the next run. */
const SUB_USER = `realmuser${SUFFIX}`;
const SUB_PASS = "R3almUserPassw0rd!";

/** Content-Type of discovery. */
const JSON_TYPE = "application/json;charset=UTF-8";
/** Content-Type of the token endpoint's and tokeninfo's 200s and of every error answer: no charset. */
const BARE_JSON_TYPE = "application/json";

/** Members of a client_credentials token response, sorted. */
const CLIENT_TOKEN_KEYS = ["access_token", "expires_in", "scope", "token_type"];
/** Members of a password grant token response, sorted: the client may refresh. */
const PASSWORD_TOKEN_KEYS = ["access_token", "expires_in", "refresh_token", "scope", "token_type"];
/**
 * Members of tokeninfo's answer for a client_credentials token, sorted. Each granted scope is a member too, valued
 * from the client identity's attribute of that name (OpenAMScopeValidator.evaluateScope).
 */
const TOKENINFO_KEYS = [
  "access_token", "client_id", "expires_in", "grant_type", SCOPE, "realm", "scope", "token_type",
];

const INVALID_TOKEN = {
  error_description: "The access token provided is expired, revoked, malformed, or invalid for other reasons.",
  error: "invalid_token",
};

/** The Restlet realm routers' 404 for a realm that does not exist. */
const notFound = (identifier) => ({
  code: 404, reason: "Not Found", message: `No mapping organization found for organization identifier: ${identifier}`,
});

let adminToken;
let subUserToken;
let rootUserToken;
/** Set once created, so that afterAll removes only what this worker made. */
let subRealmId;
let subUserCreated = false;
/** A sub-realm client_credentials token for the tokeninfo test, minted through the query spelling. */
let subRealmToken;

const adminHeaders = (version) => ({
  iPlanetDirectoryPro: adminToken, "Content-Type": "application/json", "Accept-API-Version": version,
});

/** The four spellings as URL builders. The query spelling appends realm= after any query the caller passes. */
const SPELLINGS = [
  ["nested", (path, query = "") => `${CANONICAL}${path}${query}`],
  ["legacy", (path, query = "") => `${OAUTH2}/${SUB_REALM}${path}${query}`],
  ["flat", (path, query = "") => `${OAUTH2}/realms/${SUB_REALM}${path}${query}`],
  ["query", (path, query = "") => `${OAUTH2}${path}${query ? `${query}&` : "?"}realm=${SUB_REALM}`],
];
/** The spellings that name the realm in the path. */
const PATH_SPELLINGS = SPELLINGS.filter(([label]) => label !== "query");

/** `/oauth2…` as raw path bytes, context path included, for requests sent by raw(). */
const rawPath = (suffix) => `${BASE.pathname.replace(/\/$/, "")}/oauth2${suffix}`;

/**
 * A raw GET over node:http or node:https, following the scheme of OPENAM_BASE: Playwright cannot send a Host that
 * differs from the connection's. Unlike Playwright's requests (ignoreHTTPSErrors), it verifies the server
 * certificate: against a stand with a self-signed one, pass its CA in NODE_EXTRA_CA_CERTS.
 */
function raw(path, { hostHeader } = {}) {
  return new Promise((resolve, reject) => {
    const request = (HTTPS ? httpsRequest : httpRequest)({
      hostname: BASE.hostname, port: BASE.port || (HTTPS ? 443 : 80), path, method: "GET",
      headers: hostHeader ? { Host: hostHeader } : {},
      setHost: hostHeader === undefined,
      // Node takes the TLS server name from the Host header; the certificate is the stand's, whatever Host is sent.
      ...(HTTPS ? { servername: BASE.hostname } : {}),
    }, (response) => {
      let text = "";
      response.setEncoding("utf8");
      response.on("data", (chunk) => { text += chunk; });
      response.on("end", () => resolve({ status: response.statusCode, headers: response.headers, text }));
    });
    request.on("error", reject);
    request.end();
  });
}

/** Pins the status, the Content-Type and the whole JSON body of a raw error answer. */
function expectRawJson(answer, status, body, label) {
  console.log(`[realms] ${label} -> ${answer.status} ${answer.headers["content-type"]} ${answer.text}`);
  expect(answer.status, `${label} ${answer.text}`).toBe(status);
  expect(answer.headers["content-type"], label).toBe(BARE_JSON_TYPE);
  expect(JSON.parse(answer.text), label).toEqual(body);
}

/** Pins the status, the Content-Type and the whole JSON body of an error answer. */
async function expectJsonError(response, status, body, label) {
  const text = await response.text();
  console.log(`[realms] ${label} -> ${response.status()} ${response.headers()["content-type"]} ${text}`);
  expect(response.status(), `${label} ${text}`).toBe(status);
  expect(response.headers()["content-type"], label).toBe(BARE_JSON_TYPE);
  expect(JSON.parse(text), label).toEqual(body);
}

/** Pins a token endpoint 200: status, Content-Type, cache headers, member set, non-empty tokens, type and scope. */
async function expectToken(response, keys, label) {
  const text = await response.text();
  const body = response.status() === 200 ? JSON.parse(text) : {};
  console.log(`[realms] ${label} -> ${response.status()} ${response.headers()["content-type"]}`
    + ` keys=${Object.keys(body).sort()}`);
  expect(response.status(), `${label} ${text}`).toBe(200);
  expect(response.headers()["content-type"], label).toBe(BARE_JSON_TYPE);
  expect(response.headers()["cache-control"], label).toBe("no-store");
  expect(response.headers()["pragma"], label).toBe("no-cache");
  expect(Object.keys(body).sort(), label).toEqual(keys);
  expect(body.access_token, label).toEqual(expect.stringMatching(/.+/));
  if (keys.includes("refresh_token")) {
    expect(body.refresh_token, label).toEqual(expect.stringMatching(/.+/));
  }
  expect(body.token_type, label).toBe("Bearer");
  expect(body.scope, label).toBe(SCOPE);
}

/**
 * Pins the 301 to the login page: realm names the sub-realm, and goto is the authorize URL exactly as requested,
 * in the spelling it was made with. ResourceOwnerSessionValidator builds goto from the request.
 */
function expectLoginRedirect(response, target, label) {
  const location = response.headers()["location"];
  console.log(`[realms] login redirect ${label} -> ${response.status()} ${location}`);
  expect(response.status(), label).toBe(301);
  const login = new URL(location);
  expect(login.origin + login.pathname, label).toBe(`${OPENAM_BASE}/UI/Login`);
  expect([...login.searchParams.keys()], label).toEqual(["realm", "goto"]);
  expect(login.searchParams.get("realm"), label).toBe(`/${SUB_REALM}`);
  expect(login.searchParams.get("goto"), label).toBe(target);
}

/** The authorize query in a fixed order. Its values survive the server's re-encoding of goto unchanged. */
function authorizeQuery() {
  return [
    "response_type=code", `client_id=${CLIENT_ID}`, `redirect_uri=${encodeURIComponent(REDIRECT_URI)}`,
    `scope=${SCOPE}`, `state=${STATE}`,
  ].join("&");
}

/** Authenticates through /json in a throwaway context, so that no session cookie reaches a test's context. */
async function authenticate(realmPath, username, password) {
  const context = await apiRequest.newContext();
  try {
    const response = await context.post(`${OPENAM_BASE}/json${realmPath}/authenticate`, {
      headers: {
        "Content-Type": "application/json", "X-OpenAM-Username": username,
        "X-OpenAM-Password": password, "Accept-API-Version": "resource=2.0, protocol=1.0",
      },
    });
    if (!response.ok()) {
      throw new Error(`login of ${username} in ${realmPath} failed: ${response.status()} ${await response.text()}`);
    }
    return (await response.json()).tokenId;
  } finally {
    await context.dispose();
  }
}

/** Builds the realm's deployment: realm, provider, AgentService, client, user. */
async function buildSubRealm(request) {
  const created = await request.post(`${OPENAM_BASE}/json/global-config/realms?_action=create`, {
    headers: adminHeaders("protocol=1.0,resource=1.0"),
    // SmsRealmProvider rejects a body without aliases.
    data: { name: SUB_REALM, parentPath: "/", active: true, aliases: [] },
  });
  if (!created.ok()) {
    throw new Error(`realm create failed: ${created.status()} ${await created.text()}`);
  }
  subRealmId = (await created.json())._id;

  const providerUrl = `${REALM_CONFIG}/realm-config/services/oauth-oidc`;
  const provider = await request.post(`${providerUrl}?_action=create`, {
    headers: adminHeaders("protocol=1.0,resource=1.0"),
    data: {
      coreOAuth2Config: { savedConsentAttribute: "description" },
      advancedOAuth2Config: {
        // Root's provider enforces PKCE. These tests are about the realm, so their authorize requests carry no
        // challenge.
        clientsCanSkipConsent: true, codeVerifierEnforced: false,
        supportedScopes: [SCOPE, "openid"], defaultScopes: [SCOPE],
      },
    },
  });
  if (!provider.ok()) {
    throw new Error(`provider create failed: ${provider.status()} ${await provider.text()}`);
  }

  // A realm created over REST has no AgentService configuration, so the admin API cannot write a client into it.
  // Dynamic client registration creates that configuration: register one throwaway client first.
  const current = await request.get(providerUrl, { headers: adminHeaders("protocol=1.0,resource=1.0") });
  const config = await current.json();
  config.advancedOIDCConfig.allowDynamicRegistration = true;
  const enabled = await request.put(providerUrl, { headers: adminHeaders("protocol=1.0,resource=1.0"), data: config });
  if (!enabled.ok()) {
    throw new Error(`enabling dynamic registration failed: ${enabled.status()} ${await enabled.text()}`);
  }
  const bootstrap = await request.post(`${CANONICAL}/connect/register`, {
    headers: { "Content-Type": "application/json" },
    data: { redirect_uris: [REDIRECT_URI], client_name: "agent service bootstrap" },
  });
  if (bootstrap.status() !== 201) {
    throw new Error(`AgentService bootstrap failed: ${bootstrap.status()} ${await bootstrap.text()}`);
  }

  const client = await request.put(`${REALM_CONFIG}/realm-config/agents/OAuth2Client/${CLIENT_ID}`, {
    headers: adminHeaders("protocol=2.0,resource=1.0"),
    data: {
      userpassword: CLIENT_SECRET,
      "com.forgerock.openam.oauth2provider.clientType": "Confidential",
      "com.forgerock.openam.oauth2provider.redirectionURIs": [`[0]=${REDIRECT_URI}`],
      "com.forgerock.openam.oauth2provider.scopes": [`[0]=${SCOPE}`, "[1]=openid"],
      "com.forgerock.openam.oauth2provider.defaultScopes": [`[0]=${SCOPE}`],
      "com.forgerock.openam.oauth2provider.grantTypes": [
        "[0]=authorization_code", "[1]=client_credentials", "[2]=refresh_token", "[3]=password",
      ],
      "com.forgerock.openam.oauth2provider.responseTypes": ["[0]=code", "[1]=token"],
      "com.forgerock.openam.oauth2provider.tokenEndPointAuthMethod": "client_secret_basic",
      isConsentImplied: true,
      sunIdentityServerDeviceStatus: "Active",
    },
  });
  if (!client.ok()) {
    throw new Error(`client write failed: ${client.status()} ${await client.text()}`);
  }

  const user = await request.post(`${REALM_CONFIG}/users?_action=create`, {
    headers: adminHeaders("resource=3.0, protocol=2.1"),
    data: {
      username: SUB_USER, userpassword: SUB_PASS, mail: `${SUB_USER}@example.com`,
      sn: "Realm", givenName: "Realm", cn: "Realm User",
    },
  });
  if (!user.ok()) {
    throw new Error(`sub-realm user create failed: ${user.status()} ${await user.text()}`);
  }
  subUserCreated = true;
}

test.beforeAll(async ({ request }) => {
  adminToken = await getAdminToken(request);
  expect(adminToken, "amadmin login failed").toBeTruthy();
  await buildSubRealm(request);
  subUserToken = await authenticate(`/realms/root/realms/${SUB_REALM}`, SUB_USER, SUB_PASS);
  rootUserToken = await authenticate("/realms/root", USERNAME, PASSWORD);

  const token = await request.post(`${OAUTH2}/access_token?realm=${SUB_REALM}`, {
    headers: { Authorization: basicAuth(CLIENT_ID, CLIENT_SECRET) },
    form: { grant_type: "client_credentials", scope: SCOPE },
  });
  expect(token.status(), "sub-realm client_credentials token").toBe(200);
  subRealmToken = (await token.json()).access_token;
});

test.afterAll(async ({ request }) => {
  if (!adminToken) {
    return;
  }
  const failures = [];
  // The user first: it lives in root's directory and would outlive the realm.
  if (subUserCreated) {
    const user = await request.delete(`${REALM_CONFIG}/users/${SUB_USER}`, {
      headers: { iPlanetDirectoryPro: adminToken, "Accept-API-Version": "resource=3.0, protocol=2.1" },
    });
    console.log(`[realms] user ${SUB_USER} teardown -> ${user.status()}`);
    if (!user.ok()) {
      failures.push(`user ${SUB_USER}: ${user.status()} ${await user.text()}`);
    }
  }
  if (subRealmId) {
    const realm = await request.delete(`${OPENAM_BASE}/json/global-config/realms/${subRealmId}`, {
      headers: { iPlanetDirectoryPro: adminToken, "Accept-API-Version": "protocol=1.0,resource=1.0" },
    });
    console.log(`[realms] realm ${SUB_REALM} teardown -> ${realm.status()}`);
    if (!realm.ok()) {
      failures.push(`realm ${SUB_REALM}: ${realm.status()} ${await realm.text()}`);
    }
  }
  if (failures.length) {
    throw new Error(`teardown failed: ${failures.join("; ")}`);
  }
});

test.describe("/oauth2 in a non-root realm", () => {

  test("discovery answers the realm's document on every spelling, in the nested form", async ({ request }) => {
    const documents = [];
    for (const [label, url] of SPELLINGS) {
      const response = await request.get(url("/.well-known/openid-configuration"));
      console.log(`[realms] discovery ${label} -> ${response.status()} ${response.headers()["content-type"]}`);
      expect(response.status(), label).toBe(200);
      expect(response.headers()["content-type"], label).toBe(JSON_TYPE);
      documents.push([label, await response.json()]);
    }
    const [[, nested], ...others] = documents;
    // The realm's own provider answered: root's names root's issuer and advertises root's scopes.
    expect(nested.issuer).toBe(CANONICAL);
    expect(nested.authorization_endpoint).toBe(`${CANONICAL}/authorize`);
    expect(nested.token_endpoint).toBe(`${CANONICAL}/access_token`);
    expect(nested.userinfo_endpoint).toBe(`${CANONICAL}/userinfo`);
    expect(nested.jwks_uri).toBe(`${CANONICAL}/connect/jwk_uri`);
    expect(nested.registration_endpoint).toBe(`${CANONICAL}/connect/register`);
    expect([...nested.scopes_supported].sort()).toEqual(["openid", SCOPE]);
    // Whichever spelling asked, the document is the same, with every endpoint in the nested form.
    for (const [label, document] of others) {
      expect(document, label).toEqual(nested);
    }
  });

  test("the token endpoint issues a client_credentials token through ?realm=", async ({ request }) => {
    // A leading slash in the realm value is optional.
    for (const realm of [SUB_REALM, `/${SUB_REALM}`]) {
      const response = await request.post(`${OAUTH2}/access_token?realm=${realm}`, {
        headers: { Authorization: basicAuth(CLIENT_ID, CLIENT_SECRET) },
        form: { grant_type: "client_credentials", scope: SCOPE },
      });
      await expectToken(response, CLIENT_TOKEN_KEYS, `?realm=${realm} client_credentials`);
    }
  });

  test("the token endpoint authenticates a confidential client on the path spellings", async ({ request }) => {
    // The client's login runs in the sub-realm and checks that realm against the request's
    // (AMLoginContext, through AuthClientUtils.getDomainNameByRequest). On a path spelling the request's realm is
    // a servlet request attribute with two writers: on the /realms/... spellings (nested, flat) the Restlet router
    // of RealmRoutingFactory, on the legacy /{subrealm} spelling RestletRealmRouter. Without it the check falls
    // back to the query, then to the host's realm, root, and the client gets 401 invalid_client while ?realm= keeps
    // working. A flip that resolves the realm elsewhere has to hand it to the login. The check runs only without
    // a valid session, so this test's context never carries one.
    for (const [label, url] of PATH_SPELLINGS) {
      const response = await request.post(url("/access_token"), {
        headers: { Authorization: basicAuth(CLIENT_ID, CLIENT_SECRET) },
        form: { grant_type: "client_credentials", scope: SCOPE },
      });
      await expectToken(response, CLIENT_TOKEN_KEYS, `${label} client_credentials`);
    }
  });

  test("tokeninfo resolves a sub-realm token on every spelling, and at root", async ({ request }) => {
    const check = async (label, response) => {
      const text = await response.text();
      const body = response.status() === 200 ? JSON.parse(text) : {};
      console.log(`[realms] tokeninfo ${label} -> ${response.status()} ${response.headers()["content-type"]}`
        + ` keys=${Object.keys(body).sort()} realm=${body.realm}`);
      expect(response.status(), `${label} ${text}`).toBe(200);
      expect(response.headers()["content-type"], label).toBe(BARE_JSON_TYPE);
      expect(response.headers()["cache-control"], label).toBe("no-cache, no-store");
      expect(Object.keys(body).sort(), label).toEqual(TOKENINFO_KEYS);
      // The answer names the realm the token belongs to.
      expect(body.realm, label).toBe(`/${SUB_REALM}`);
      expect(body.client_id, label).toBe(CLIENT_ID);
      expect(body.grant_type, label).toBe("client_credentials");
      expect(body.scope, label).toEqual([SCOPE]);
    };
    for (const [label, url] of SPELLINGS) {
      await check(label, await request.get(url("/tokeninfo", `?access_token=${subRealmToken}`)));
    }
    // tokeninfo is not realm-scoped: root resolves a sub-realm token and reports the token's realm.
    await check("root", await request.get(`${OAUTH2}/tokeninfo?access_token=${subRealmToken}`));
  });

  test("an unauthenticated authorize redirects to the sub-realm login on every spelling", async () => {
    const anonymous = await apiRequest.newContext();
    try {
      for (const [label, url] of SPELLINGS) {
        const target = url("/authorize", `?${authorizeQuery()}`);
        const response = await anonymous.get(target, { maxRedirects: 0 });
        expectLoginRedirect(response, target, label);
        expect(response.headers()["cache-control"], label).toBe("no-store");
      }
    } finally {
      await anonymous.dispose();
    }
  });

  test("authorize issues a code on every spelling for a sub-realm session", async () => {
    // authorize authenticates by the session cookie only, never by header.
    const subRealmSession = await sessionContext(apiRequest, subUserToken);
    try {
      const codes = new Set();
      for (const [label, url] of SPELLINGS) {
        const response = await subRealmSession.get(url("/authorize", `?${authorizeQuery()}`), { maxRedirects: 0 });
        // The status first, so an answer without a Location fails on it. A 302 carries the code: no body of it is read.
        const text = response.status() === 302 ? "" : await response.text();
        expect(response.status(), `${label} ${text}`).toBe(302);
        const location = new URL(response.headers()["location"]);
        console.log(`[realms] authorize ${label} -> ${response.status()} ${location.origin}${location.pathname}`
          + ` keys=${[...location.searchParams.keys()]}`);
        expect(location.origin + location.pathname, label).toBe(REDIRECT_URI);
        expect([...location.searchParams.keys()].sort(), label)
          .toEqual(["client_id", "code", "iss", "scope", "state"]);
        expect(location.searchParams.get("code"), label).toBeTruthy();
        expect(location.searchParams.get("scope"), label).toBe(SCOPE);
        expect(location.searchParams.get("state"), label).toBe(STATE);
        expect(location.searchParams.get("client_id"), label).toBe(CLIENT_ID);
        // The realm's issuer in the nested form, whichever spelling asked: the value discovery advertises.
        expect(location.searchParams.get("iss"), label).toBe(CANONICAL);
        codes.add(location.searchParams.get("code"));
      }
      // One fresh code per request.
      expect(codes.size).toBe(SPELLINGS.length);
    } finally {
      await subRealmSession.dispose();
    }
  });

  test("a root-realm session does not authorize in the sub-realm", async () => {
    // Realm isolation: the session belongs to root, so the sub-realm sends the browser to its own login instead of
    // issuing a code for it.
    const rootSession = await sessionContext(apiRequest, rootUserToken);
    try {
      for (const [label, url] of SPELLINGS) {
        const target = url("/authorize", `?${authorizeQuery()}`);
        expectLoginRedirect(await rootSession.get(target, { maxRedirects: 0 }), target, label);
      }
    } finally {
      await rootSession.dispose();
    }
  });

  test("an unknown realm, an unrouted path and an unknown Host under the sub-realm", async () => {
    // An unknown realm in the path is a 404 naming the realm with a leading slash, whichever router looked it up:
    // RealmRoutingFactory's on the nested and the flat form, RestletRealmRouter on the legacy one.
    for (const path of [
      "/realms/root/realms/nosuchsub/tokeninfo", "/realms/nosuchsub/tokeninfo", "/nosuchsub/tokeninfo",
    ]) {
      expectRawJson(await raw(rawPath(`${path}?access_token=x`)), 404, notFound("/nosuchsub"), path);
    }

    // An unrouted path below a realm that resolves is a 404. Its body is the framework's, so only the status is
    // pinned.
    for (const path of [`/${SUB_REALM}/nosuchendpoint`, `/realms/root/realms/${SUB_REALM}/nosuchendpoint`]) {
      const answer = await raw(rawPath(path));
      console.log(`[realms] unrouted ${path} -> ${answer.status}`);
      expect(answer.status, path).toBe(404);
    }

    // A ?realm= naming an unknown realm replaces the path's realm and is a 404 too. The value is reported as given,
    // an unknown child of a real realm included.
    for (const [path, realm] of [
      [`/${SUB_REALM}/tokeninfo?realm=bogus&access_token=x`, "bogus"],
      [`/tokeninfo?realm=${SUB_REALM}%2Fnosuch&access_token=x`, `${SUB_REALM}/nosuch`],
    ]) {
      expectRawJson(await raw(rawPath(path)), 404, notFound(realm), path);
    }

    // An unknown Host: RestletRealmRouter resolves the Host's realm before the path's, and the failed lookup
    // surfaces as a 500 whatever the spelling. That is a defect: the target is 400 invalid_request, a client error.
    for (const path of [
      `/${SUB_REALM}/tokeninfo`, `/realms/root/realms/${SUB_REALM}/tokeninfo`, `/tokeninfo?realm=${SUB_REALM}`,
    ]) {
      expectRawJson(await raw(rawPath(path), { hostHeader: "not-a-real-host.invalid" }), 500, {
        code: 500, reason: "Internal Server Error",
        message: "No mapping organization found for organization identifier: not-a-real-host.invalid",
      }, `unknown Host ${path}`);
    }
  });

  test("the realm segment of the path is matched case-insensitively", async ({ request }) => {
    // Unlike the endpoint segment (/oauth2/Tokeninfo is a 404), on every path spelling: RestletRealmRouter matches
    // the legacy segment, RealmRoutingFactory's router the nested and flat ones, root included.
    const UPPER = SUB_REALM.toUpperCase();
    for (const [label, prefix] of [
      ["legacy", `/${UPPER}`], ["nested", `/realms/ROOT/realms/${UPPER}`], ["flat", `/realms/${UPPER}`],
    ]) {
      // The upper-cased realm resolves, and tokeninfo then rejects the bogus token. tokeninfo is not realm-scoped,
      // so this leg alone would also pass if the segment fell back to root.
      expectRawJson(await raw(rawPath(`${prefix}/tokeninfo?access_token=not-a-token`)), 401, INVALID_TOKEN,
        `upper-cased ${label} realm`);

      // The token endpoint is realm-scoped: the client exists only in the sub-realm, so a token proves the
      // upper-cased segment resolved to the sub-realm. This test's context never carries a session.
      const token = await request.post(`${OAUTH2}${prefix}/access_token`, {
        headers: { Authorization: basicAuth(CLIENT_ID, CLIENT_SECRET) },
        form: { grant_type: "client_credentials", scope: SCOPE },
      });
      await expectToken(token, CLIENT_TOKEN_KEYS, `upper-cased ${label} realm client_credentials`);
    }
  });

  test("the password grant authenticates a sub-realm resource owner on every spelling", async ({ request }) => {
    // Two logins run in the sub-realm, the client's and the resource owner's (ResourceOwnerAuthenticator), so every
    // spelling has to carry the realm to both. This test's context never carries a session.
    for (const [label, url] of SPELLINGS) {
      const response = await request.post(url("/access_token"), {
        headers: { Authorization: basicAuth(CLIENT_ID, CLIENT_SECRET) },
        form: { grant_type: "password", username: SUB_USER, password: SUB_PASS, scope: SCOPE },
      });
      await expectToken(response, PASSWORD_TOKEN_KEYS, `${label} password grant`);
    }

    // A wrong password is a bad grant, 400 (RFC 6749 §5.2); a bad client credential would be 401.
    const wrong = await request.post(`${CANONICAL}/access_token`, {
      headers: { Authorization: basicAuth(CLIENT_ID, CLIENT_SECRET) },
      form: { grant_type: "password", username: SUB_USER, password: "not-the-password", scope: SCOPE },
    });
    await expectJsonError(wrong, 400, {
      error_description: "The provided access grant is invalid, expired, or revoked.", error: "invalid_grant",
    }, "wrong password");

    // A root user gets a token here too: the realm copied root's data store, so its login reaches root's directory.
    const rootUser = await request.post(`${CANONICAL}/access_token`, {
      headers: { Authorization: basicAuth(CLIENT_ID, CLIENT_SECRET) },
      form: { grant_type: "password", username: USERNAME, password: PASSWORD, scope: SCOPE },
    });
    await expectToken(rootUser, PASSWORD_TOKEN_KEYS, "root user password grant");
  });

});
