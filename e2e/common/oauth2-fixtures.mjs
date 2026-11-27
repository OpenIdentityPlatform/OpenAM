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
 * Shared OAuth2/OIDC/UMA fixtures for the e2e specs. Every helper is idempotent and safe when several spec files
 * call it in parallel -- Playwright runs spec FILES in parallel, so "create if absent, widen if present" is
 * the only safe shape here.
 */

import { randomUUID } from "node:crypto";
import { MessageChannel, Worker } from "node:worker_threads";
import { getAdminToken, OPENAM_BASE, PASSWORD, USERNAME } from "./openam-commons.mjs";

export const REALM = "root";
export const SCOPE = "profile";
export const REDIRECT_URI = "http://app.invalid/cb";

/*
 * The post-logout redirect URIs registered on the OIDC client. endSession accepts a post_logout_redirect_uri
 * only when the client's registered set contains it exactly, so the variant with a query is registered on its own.
 */
export const POST_LOGOUT_REDIRECT_URI = "http://app.invalid/logout";
export const POST_LOGOUT_REDIRECT_URI_WITH_QUERY = "http://app.invalid/logout?ui=1";

/*
 * The clientSessionURI registered on the OIDC client: the origin the checkSession page accepts postMessage
 * calls from. For a client without one the page gets an empty origin and accepts no message.
 */
export const CLIENT_SESSION_URI = "http://rp.invalid";

/*
 * Client ids are PER SPEC FILE, not shared. Playwright runs spec files in parallel, and rewriting a client
 * invalidates the tokens already issued against it -- so two files sharing one client flake as soon as their
 * beforeAll hooks interleave. Each file owning its own clients removes the coupling entirely.
 *
 * The secrets are deliberately long. OpenAM signs id_tokens with HS256 (the default
 * idTokenSignedResponseAlg) using the client secret, and HMAC-SHA256 needs a >=256-bit key -- a short secret
 * makes the token endpoint fail with a bare `server_error` that says nothing about the cause.
 */
export const OIDC_CLIENT_SECRET = "oidc-secret-0123456789-0123456789-0123456789";
export const RS_CLIENT_SECRET = "rs-secret-0123456789-0123456789-0123456789";

/** Scopes the specs need the provider to issue. */
const REQUIRED_SCOPES = ["openid", "profile", "uma_protection", "uma_authorization"];

/**
 * Where the provider stores a consent decision. Any non-empty value keeps `isSaveConsentEnabled` true;
 * `description` is a real user attribute, so storing a decision against it actually works.
 */
const SAVED_CONSENT_ATTRIBUTE = "description";

const CONFIG_VERSION = { "Accept-API-Version": "protocol=1.0,resource=1.0" };
const AGENT_VERSION = { "Accept-API-Version": "protocol=2.0,resource=1.0" };

function adminHeaders(adminToken, version) {
  return { iPlanetDirectoryPro: adminToken, "Content-Type": "application/json", ...version };
}

/**
 * Creates the realm service if it is absent, and returns the response holding its configuration.
 *
 * Only a 404 means "absent". Falling through on a 401 or a 500 would report the same failure one call later as
 * a failed create, pointing the reader at creation instead of the real cause.
 *
 * A concurrent spec file may create the service between our GET and our POST, and that is success, not failure.
 * The losing create gets a 409 from the exists() check. When both creates pass that check before either writes
 * (SmsSingletonProvider checks, then creates), the answer depends on the configuration store: the embedded one
 * ignores the duplicate add and both answer 201, an external one answers 500. So the outcome is judged on a
 * re-read, not on the create's status.
 */
async function ensureRealmService(adminToken, request, service, data) {
  const url = `${OPENAM_BASE}/json/realms/${REALM}/realm-config/services/${service}`;
  const headers = adminHeaders(adminToken, CONFIG_VERSION);
  const existing = await request.get(url, { headers });
  if (existing.ok()) {
    return existing;
  }
  if (existing.status() !== 404) {
    throw new Error(`${service} service not readable: ${existing.status()} ${await existing.text()}`);
  }
  const created = await request.post(`${url}?_action=create`, { headers, data });
  const reread = await request.get(url, { headers });
  if (!reread.ok()) {
    throw new Error(`Failed to create the ${service} service: ${created.status()} ${await created.text()}`
      + ` (re-read: ${reread.status()})`);
  }
  console.log(created.ok() ? `${service} service created` : `${service} service created by a concurrent spec file`);
  return reread;
}

/**
 * Ensures the realm has an OAuth2 provider, and returns the response holding its configuration.
 *
 * This lives here rather than in a single spec file because Playwright runs spec FILES in parallel: if the
 * only creator were one spec's `beforeAll`, every other spec racing it on a cold container would read a 404.
 *
 * The payload holds the three settings that oauth2-test.spec.mjs and device-flow-test.spec.mjs create the
 * provider with, so the provider ends up the same whichever spec file creates it first.
 */
export async function ensureOAuth2Provider(adminToken, request) {
  return ensureRealmService(adminToken, request, "oauth-oidc", {
    advancedOAuth2Config: { clientsCanSkipConsent: true, supportedScopes: [SCOPE], defaultScopes: [SCOPE] },
  });
}

/**
 * Brings the realm's OAuth2 provider up to what the specs need -- creating it first if absent -- and does
 * nothing at all once it is already there. Two invariants, checked independently:
 *
 *   1. every scope of `REQUIRED_SCOPES` is issuable (only ever ADDS scopes).
 *   2. `coreOAuth2Config.savedConsentAttribute` is non-empty. The realm-config endpoint answers
 *      `501 Patch not supported`, so a full-object PUT is the only way to change one field, and that PUT
 *      stores a read-back `""` as absent. That would silently turn the consent page's save-consent control
 *      off for every spec on the server.
 *
 * They are checked separately on purpose: coupling (2) to (1) would mean a container that has the scopes but
 * lost the attribute could never be healed.
 *
 * Safe when several spec files call it in parallel: identical writers computing the same target state from
 * the same base, and a loser re-reads rather than failing.
 */
export async function ensureProviderConfig(adminToken, request) {
  const url = `${OPENAM_BASE}/json/realms/${REALM}/realm-config/services/oauth-oidc`;
  const current = await ensureOAuth2Provider(adminToken, request);
  const config = await current.json();
  const supported = config.advancedOAuth2Config?.supportedScopes ?? [];
  const missingScopes = REQUIRED_SCOPES.filter((scope) => !supported.includes(scope));
  const savedConsent = config.coreOAuth2Config?.savedConsentAttribute || SAVED_CONSENT_ATTRIBUTE;
  const needsSavedConsent = !config.coreOAuth2Config?.savedConsentAttribute;
  if (missingScopes.length === 0 && !needsSavedConsent) {
    console.log(`OAuth2 provider already supports ${REQUIRED_SCOPES.join(", ")}`);
    return;
  }
  const updated = await request.put(url, {
    headers: adminHeaders(adminToken, CONFIG_VERSION),
    data: {
      ...config,
      coreOAuth2Config: { ...config.coreOAuth2Config, savedConsentAttribute: savedConsent },
      advancedOAuth2Config: {
        ...config.advancedOAuth2Config,
        supportedScopes: [...new Set([...supported, ...REQUIRED_SCOPES])],
      },
    },
  });
  if (!updated.ok()) {
    // A concurrent spec file writing the same target state is success, not failure -- so re-read and judge
    // on the state, not on our own write's status.
    await assertProviderConfig(adminToken, request, `${updated.status()} ${await updated.text()}`);
    return;
  }
  console.log(`OAuth2 provider updated (scopes: ${missingScopes.join(", ") || "none"},`
    + ` savedConsentAttribute: ${needsSavedConsent ? savedConsent : "already set"})`);
}

/** Re-reads the provider and fails with the original write error unless someone else got us there. */
async function assertProviderConfig(adminToken, request, writeError) {
  const url = `${OPENAM_BASE}/json/realms/${REALM}/realm-config/services/oauth-oidc`;
  const reread = await request.get(url, { headers: adminHeaders(adminToken, CONFIG_VERSION) });
  const config = reread.ok() ? await reread.json() : {};
  const supported = config.advancedOAuth2Config?.supportedScopes ?? [];
  if (REQUIRED_SCOPES.every((scope) => supported.includes(scope))
      && config.coreOAuth2Config?.savedConsentAttribute) {
    console.log("OAuth2 provider already brought up to date by a concurrent spec file");
    return;
  }
  throw new Error(`Failed to update the OAuth2 provider: ${writeError}`);
}

/**
 * Ensures the realm has a UMA provider. Without it every /uma protocol endpoint answers
 * `404 No UMA provider for realm /`, and the resource_set registration endpoint has nothing to point at.
 */
export async function ensureUmaProvider(adminToken, request) {
  await ensureRealmService(adminToken, request, "uma", {});
}

/**
 * Writes an OAuth2 client. `data` is the full agent representation and the PUT is a full replace, so this
 * runs unconditionally rather than create-if-absent: a client left over from an earlier revision of these
 * fixtures (different secret, missing scope) would otherwise survive and fail the specs in confusing ways.
 * Repeating the same PUT concurrently from two spec files is harmless.
 */
export async function ensureClient(adminToken, request, clientId, data) {
  const url = `${OPENAM_BASE}/json/realms/${REALM}/realm-config/agents/OAuth2Client/${clientId}`;
  const written = await request.put(url, { headers: adminHeaders(adminToken, AGENT_VERSION), data });
  if (!written.ok()) {
    throw new Error(`Failed to write client ${clientId}: ${written.status()} ${await written.text()}`);
  }
  console.log(`OAuth2 client "${clientId}" written (${written.status()})`);
  return written;
}

export async function ensureOidcClient(adminToken, request, clientId) {
  await ensureClient(adminToken, request, clientId, {
    "userpassword": OIDC_CLIENT_SECRET,
    "com.forgerock.openam.oauth2provider.clientType": "Confidential",
    "com.forgerock.openam.oauth2provider.redirectionURIs": [`[0]=${REDIRECT_URI}`],
    "com.forgerock.openam.oauth2provider.postLogoutRedirectURI": [
      `[0]=${POST_LOGOUT_REDIRECT_URI}`, `[1]=${POST_LOGOUT_REDIRECT_URI_WITH_QUERY}`,
    ],
    "com.forgerock.openam.oauth2provider.clientSessionURI": CLIENT_SESSION_URI,
    "com.forgerock.openam.oauth2provider.scopes": ["[0]=openid", "[1]=profile"],
    "com.forgerock.openam.oauth2provider.defaultScopes": ["[0]=openid"],
    "com.forgerock.openam.oauth2provider.grantTypes": [
      "[0]=authorization_code", "[1]=implicit", "[2]=refresh_token",
      "[3]=urn:ietf:params:oauth:grant-type:device_code",
    ],
    "com.forgerock.openam.oauth2provider.responseTypes": [
      "[0]=code", "[1]=token", "[2]=id_token", "[3]=code token", "[4]=token id_token",
    ],
    "com.forgerock.openam.oauth2provider.tokenEndPointAuthMethod": "client_secret_basic",
    // HS256 signs the id_token with the client secret, so idtokeninfo can verify it without a client JWKS.
    // With RS256 idtokeninfo verifies with the CLIENT's registered public key; this client registers none,
    // so idtokeninfo rejects the id_token with 400 "invalid id_token".
    "com.forgerock.openam.oauth2provider.idTokenSignedResponseAlg": "HS256",
    "isConsentImplied": true,
    "sunIdentityServerDeviceStatus": "Active",
  });
}

/**
 * A confidential OIDC client without a clientSessionURI, as the admin API creates one by default. It shares
 * OIDC_CLIENT_SECRET, so a test can sign an HS256 id_token for it that passes checkSession's signature check.
 */
export async function ensureNoSessionUriOidcClient(adminToken, request, clientId) {
  await ensureClient(adminToken, request, clientId, {
    "userpassword": OIDC_CLIENT_SECRET,
    "com.forgerock.openam.oauth2provider.clientType": "Confidential",
    "com.forgerock.openam.oauth2provider.redirectionURIs": [`[0]=${REDIRECT_URI}`],
    "com.forgerock.openam.oauth2provider.scopes": ["[0]=openid", "[1]=profile"],
    "com.forgerock.openam.oauth2provider.defaultScopes": ["[0]=openid"],
    "com.forgerock.openam.oauth2provider.idTokenSignedResponseAlg": "HS256",
    "sunIdentityServerDeviceStatus": "Active",
  });
}

/**
 * Give each worker/run its own provider. No root-realm provider GET/PUT is performed,
 * so this setup cannot overwrite settings being changed by the other OAuth/UMA specs.
 */
export async function createDevicePkceRealm(adminToken, request, realm, clientId, owner) {
  const realmUrl = `${OPENAM_BASE}/json/realms/root/realms/${realm}`;
  const oauthUrl = `${OPENAM_BASE}/oauth2/realms/root/realms/${realm}`;
  const headers = adminHeaders(adminToken, CONFIG_VERSION);
  async function requireSuccess(response, operation) {
    if (!response.ok()) throw new Error(`${operation}: ${response.status()} ${await response.text()}`);
  }
  await requireSuccess(await request.post(`${OPENAM_BASE}/json/global-config/realms?_action=create`, {
    headers, data: { name: realm, parentPath: "/", active: true, aliases: [] },
  }), "create PKCE realm");
  await requireSuccess(await request.post(`${realmUrl}/realm-config/services/oauth-oidc?_action=create`, {
    headers,
    data: {
      advancedOAuth2Config: { clientsCanSkipConsent: true, supportedScopes: [SCOPE], defaultScopes: [SCOPE] },
      advancedOIDCConfig: { allowDynamicRegistration: true },
    },
  }), "create PKCE provider");
  // A new realm has no AgentService container. Dynamic registration creates it through
  // the product's supported registration path before the admin API writes our client.
  await requireSuccess(await request.post(`${oauthUrl}/connect/register`, {
    data: { redirect_uris: [REDIRECT_URI], client_name: "PKCE realm bootstrap" },
  }), "bootstrap PKCE AgentService");
  await requireSuccess(await request.put(`${realmUrl}/realm-config/agents/OAuth2Client/${clientId}`, {
    headers: adminHeaders(adminToken, AGENT_VERSION),
    data: {
      "com.forgerock.openam.oauth2provider.clientType": "Public",
      "com.forgerock.openam.oauth2provider.scopes": [`[0]=${SCOPE}`],
      "com.forgerock.openam.oauth2provider.defaultScopes": [`[0]=${SCOPE}`],
      "com.forgerock.openam.oauth2provider.grantTypes": ["[0]=urn:ietf:params:oauth:grant-type:device_code"],
      "com.forgerock.openam.oauth2provider.responseTypes": ["[0]=device_code"],
      "com.forgerock.openam.oauth2provider.tokenEndPointAuthMethod": "none",
      isConsentImplied: true,
      sunIdentityServerDeviceStatus: "Active",
    },
  }), "create PKCE client");
  // A fresh realm must provision its resource owner rather than rely on a root demo account.
  await requireSuccess(await request.post(`${realmUrl}/users?_action=create`, {
    headers: adminHeaders(adminToken, { "Accept-API-Version": "resource=3.0,protocol=2.1" }),
    data: {
      username: owner.username, userpassword: owner.password,
      sn: "PKCE", cn: "PKCE Test User", givenName: "PKCE",
    },
  }), "create PKCE resource owner");
  return { realmUrl, oauthUrl };
}

export async function deleteDevicePkceRealm(adminToken, request, realm, username) {
  // Realm deletion does not remove entries from its shared LDAP identity store.
  if (username) {
    const user = await request.delete(`${OPENAM_BASE}/json/realms/root/realms/${realm}/users/${encodeURIComponent(username)}`, {
      headers: adminHeaders(adminToken, { "Accept-API-Version": "resource=3.0,protocol=2.1" }),
    });
    if (!user.ok() && user.status() !== 404) {
      throw new Error(`delete PKCE resource owner: ${user.status()} ${await user.text()}`);
    }
  }
  const id = Buffer.from(`/${realm}`).toString("base64url");
  const response = await request.delete(`${OPENAM_BASE}/json/global-config/realms/${id}`, {
    headers: adminHeaders(adminToken, CONFIG_VERSION),
  });
  if (!response.ok() && response.status() !== 404) {
    throw new Error(`delete PKCE realm: ${response.status()} ${await response.text()}`);
  }
}

export async function ensureResourceServerClient(adminToken, request, clientId) {
  await ensureClient(adminToken, request, clientId, {
    "userpassword": RS_CLIENT_SECRET,
    "com.forgerock.openam.oauth2provider.clientType": "Confidential",
    "com.forgerock.openam.oauth2provider.redirectionURIs": [`[0]=${REDIRECT_URI}`],
    // uma_authorization is what an AAT carries -- /uma/authz_request refuses anything else with a 403.
    "com.forgerock.openam.oauth2provider.scopes": [
      "[0]=uma_protection", "[1]=openid", "[2]=uma_authorization",
    ],
    "com.forgerock.openam.oauth2provider.defaultScopes": ["[0]=uma_protection"],
    "com.forgerock.openam.oauth2provider.grantTypes": [
      "[0]=password", "[1]=authorization_code", "[2]=client_credentials",
    ],
    "com.forgerock.openam.oauth2provider.responseTypes": ["[0]=code", "[1]=token"],
    "com.forgerock.openam.oauth2provider.tokenEndPointAuthMethod": "client_secret_basic",
    "isConsentImplied": true,
    "sunIdentityServerDeviceStatus": "Active",
  });
}

export function basicAuth(clientId, secret) {
  return `Basic ${Buffer.from(`${clientId}:${secret}`).toString("base64")}`;
}

/**
 * A name no earlier run can have taken. Resource-set names are unique per resource owner -- registering a
 * duplicate answers `400 A shared item with the name '...' already exists` -- so a fixed name works exactly
 * once against a given container and then fails forever.
 */
export function uniqueName(prefix) {
  return `${prefix} ${Math.random().toString(36).slice(2, 10)}`;
}

/**
 * Registers a resource set. Returns `{response, name}` -- the name matters because a retry uses a different
 * one, so callers that assert on the stored name must use what was actually sent.
 *
 * Retries a `server_error` a couple of times. On a container where the UMA provider service has only just
 * been created, the first registration can fail while the realm's UMA policy model initialises lazily; a
 * second attempt succeeds. Note the retry keys on the BODY, not the status: Restlet maps `ServerException`
 * to HTTP 400 with `{"error":"server_error","error_description":"Internal Server Error (500) - ..."}`, so a
 * status-only check never fires. Every other 4xx is a real answer and is returned untouched.
 */
export async function registerResourceSet(request, pat, name, extra = {}) {
  let response;
  let used = name;
  for (let attempt = 0; attempt < 3; attempt++) {
    // A distinct name per attempt: a failed registration can still have taken the name, and names are
    // unique per resource owner, so reusing it turns the retry into "already exists".
    used = attempt === 0 ? name : `${name} r${attempt}`;
    response = await request.post(`${OPENAM_BASE}/oauth2/resource_set`, {
      headers: { Authorization: `Bearer ${pat}`, "Content-Type": "application/json" },
      data: { name: used, scopes: ["read", "write"], ...extra },
    });
    if (response.status() === 201 || !(await isServerError(response))) {
      break;
    }
    if (attempt < 2) {
      console.log(`[fixtures] resource_set registration attempt ${attempt + 1} -> server_error, retrying`);
      await new Promise((resolve) => setTimeout(resolve, 750));
    }
  }
  return { response, name: used };
}

async function isServerError(response) {
  if (response.status() >= 500) {
    return true;
  }
  try {
    return (await response.json()).error === "server_error";
  } catch (notJson) {
    return false;
  }
}

/**
 * Blocks until resource-set registration actually works, then cleans up after itself.
 *
 * Creating the UMA provider service kicks off asynchronous provisioning of the realm's UMA policy model
 * (watch the `/internal/policySet` notifications in the Entitlement debug log). Until that settles, every
 * registration answers `server_error` -- for well over a second, so a per-request retry is not enough. Doing
 * the waiting once in setup keeps it out of the tests, and on a warm container the first attempt succeeds and
 * this returns immediately. A run-specific name prefix also identifies failed attempts during teardown.
 */
export async function warmUpResourceSetStore(
  request, pat, cleanup = (id) => deleteResourceSet(request, pat, id), namePrefix = "warmup",
) {
  const deadline = Date.now() + 30_000;
  for (let attempt = 1; ; attempt++) {
    const response = await request.post(`${OPENAM_BASE}/oauth2/resource_set`, {
      headers: { Authorization: `Bearer ${pat}`, "Content-Type": "application/json" },
      data: { name: uniqueName(namePrefix), scopes: ["read"] },
    });
    if (response.status() === 201) {
      if (attempt > 1) {
        console.log(`[fixtures] resource-set store ready after ${attempt} attempts`);
      }
      await cleanup((await response.json())._id);
      return;
    }
    const body = await response.text();
    if (!(await isServerError(response))) {
      // Not a warm-up problem: registration reached a real decision (bad token, missing UMA provider...).
      // Let the tests report it against their own assertions rather than masking it here.
      console.log(`[fixtures] resource-set warm-up saw a real answer: ${response.status()} ${body}`);
      return;
    }
    if (Date.now() > deadline) {
      // Failing here rather than returning: the whole point of this helper is to absorb the wait in setup,
      // so exhausting the deadline is a setup failure, not something to rediscover one assertion at a time.
      throw new Error(`resource-set store never became ready: ${response.status()} ${body}`);
    }
    await new Promise((resolve) => setTimeout(resolve, 1000));
  }
}

/** Best-effort cleanup so a container survives many runs; failures here must not fail a test. */
export async function deleteResourceSet(request, pat, id) {
  try {
    await request.delete(`${OPENAM_BASE}/oauth2/resource_set/${id}`, {
      headers: { Authorization: `Bearer ${pat}`, "If-Match": "*" },
    });
  } catch (ignored) {
    // The suite already reported whatever went wrong; leaking one resource set is not worth a red run.
  }
}

/**
 * A value the templates interpolate into the `pageData` object literal, scraped out of the rendered page.
 *
 * Anchored on a preceding delimiter: unanchored, `isplayName` would also match inside `displayName`, and the touch
 * tests could no longer tell the two keys apart.
 */
export function pageDataValue(html, key) {
  const match = new RegExp(`(?:^|[{,\\s])${key}:\\s*"([^"]*)"`).exec(html);
  return match && match[1];
}

/** ESAPI HTML-encodes the name before FreeMarker interpolates it; decode once as inert text, never markup. */
export async function userNameValue(page, html) {
  return page.evaluate((encoded) => {
    if (encoded === null) return null;
    const text = document.createElement("textarea");
    text.innerHTML = encoded;
    return text.value;
  }, pageDataValue(html, "userName"));
}

/** Reads the resource owner's display name from the default provider attribute, cn. */
export async function readUserCommonName(adminToken, request, username) {
  const response = await request.get(`${OPENAM_BASE}/json/realms/${REALM}/users/${username}`, {
    headers: { iPlanetDirectoryPro: adminToken, "Accept-API-Version": "resource=3.0, protocol=2.1" },
  });
  if (!response.ok()) {
    throw new Error(`Failed to read ${username}: ${response.status()} ${response.statusText()} ${await response.text()}`);
  }
  const commonName = (await response.json()).cn?.[0];
  if (!commonName) {
    throw new Error(`${username} has no cn`);
  }
  return commonName;
}

/**
 * A fixed PKCE pair. `authorizationCodeTokens` always sends the S256 challenge, so the flow works whether or
 * not the provider enforces PKCE. The provider's `codeVerifierEnforced` defaults to true and no fixture
 * changes it, so an authorization request without a challenge is rejected.
 */
export async function pkce() {
  const verifier = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-._~";
  const digest = await crypto.subtle.digest("SHA-256", new TextEncoder().encode(verifier));
  return { verifier, challenge: Buffer.from(digest).toString("base64url") };
}

/**
 * Playwright includes outgoing headers in API call errors, even with tracing disabled. Create credentialed
 * request contexts in a worker without the test runner's reporter instrumentation.
 */
export const unreportedRequest = {
  newContext(options) {
    const source = `
      import { parentPort, workerData } from "node:worker_threads";
      const { request } = await import(workerData.playwright);
      const context = await request.newContext(workerData.options);

      parentPort.on("message", async ({ url, options, reply }) => {
        try {
          const response = await context.fetch(url, options);
          let result;
          try {
            result = {
              status: response.status(),
              statusText: response.statusText(),
              headers: response.headers(),
              headersArray: response.headersArray(),
              text: await response.text(),
            };
          } finally {
            await response.dispose();
          }
          reply.postMessage(result);
        } catch {
          reply.postMessage(null);
        } finally {
          reply.close();
        }
      });
    `;
    const worker = new Worker(new URL("data:text/javascript," + encodeURIComponent(source)), {
      workerData: { playwright: import.meta.resolve("playwright"), options },
      stdout: true,
      stderr: true,
    });
    worker.stdout.resume();
    worker.stderr.resume();
    const stopped = new Promise((resolve) => {
      worker.on("error", () => resolve(null));
      worker.once("exit", () => resolve(null));
    });

    async function fetch(url, requestOptions) {
      const { port1, port2 } = new MessageChannel();
      try {
        const result = await Promise.race([
          new Promise((resolve) => {
            port1.once("message", resolve);
            worker.postMessage({ url, options: requestOptions, reply: port2 }, [port2]);
          }),
          stopped,
        ]);
        if (result === null) {
          throw new Error("OpenAM request failed");
        }
        return {
          ok: () => result.status >= 200 && result.status < 300,
          status: () => result.status,
          statusText: () => result.statusText,
          headers: () => result.headers,
          headersArray: () => result.headersArray,
          text: async () => result.text,
          json: async () => {
            try {
              return JSON.parse(result.text);
            } catch {
              throw new Error(`OpenAM returned invalid JSON (status ${result.status})`);
            }
          },
        };
      } finally {
        port1.close();
      }
    }

    return {
      get: (url, options) => fetch(url, { ...options, method: "GET" }),
      delete: (url, options) => fetch(url, { ...options, method: "DELETE" }),
      head: (url, options) => fetch(url, { ...options, method: "HEAD" }),
      post: (url, options) => fetch(url, { ...options, method: "POST" }),
      put: (url, options) => fetch(url, { ...options, method: "PUT" }),
      dispose: () => worker.terminate(),
    };
  },
};

const INTROSPECTION_OAUTH2 = OPENAM_BASE + "/oauth2";
const OAUTH2_CLIENTS = OPENAM_BASE + "/json/realms/" + REALM + "/realm-config/agents/OAuth2Client";
const CLIENT_ATTR = "com.forgerock.openam.oauth2provider.";
const CLIENT_NAME = CLIENT_ATTR + "clientName";

function nonemptyString(value) {
  return typeof value === "string" && value.trim().length > 0;
}

function isJsonObject(value) {
  return value !== null && typeof value === "object" && !Array.isArray(value);
}

export function isInactiveIntrospection(body) {
  return isJsonObject(body) && body.active === false && Object.keys(body).length === 1;
}

function clientNames(record) {
  return [record?.[CLIENT_NAME]].flat().filter(nonemptyString).map((name) => name.replace(/^\[\d+\]=/, ""));
}

function isTransientFailure(response) {
  const status = response?.status();
  return !response || status === 408 || status === 429 || status >= 500;
}

function requireStatus(response, expected, operation) {
  if (response.status() !== expected) {
    throw new Error(`${operation}: expected HTTP ${expected}, received ${response.status()}`);
  }
}

function requireJsonMediaType(response, operation) {
  const mediaType = response.headers()["content-type"]?.split(";")[0].trim().toLowerCase();
  if (mediaType !== "application/json") {
    throw new Error(`${operation}: expected application/json (HTTP ${response.status()})`);
  }
}

// The shared setup helpers can include response bodies in errors.
async function runClientSetup(request, operation, run) {
  const statuses = [];
  const safeRequest = Object.fromEntries(["get", "post", "put"].map((method) => [
    method, async (url, options) => {
      const response = await request[method](url, options);
      statuses.push(response.status());
      return {
        ...response,
        text: async () => "",
        json: async () => {
          if (!response.ok()) throw new Error("Setup request failed");
          return response.json();
        },
      };
    },
  ]));
  try {
    return await run(safeRequest);
  } catch {
    const status = statuses.length ? " (HTTP " + statuses.join(", ") + ")" : "";
    throw new Error(operation + " failed" + status);
  }
}

/** Own one introspection client and any tokens issued during the test. */
export async function withIntrospectionClient({ ignoreHTTPSErrors, grantType }, run) {
  const id = "s7a-" + randomUUID();
  const name = id + "-name";
  const secret = randomUUID();
  const url = OAUTH2_CLIENTS + "/" + id;
  const credentials = { Authorization: basicAuth(id, secret) };
  const options = { ignoreHTTPSErrors, maxRedirects: 0 };
  const failures = [];
  let admin, oauth, headers, accessToken, refreshToken;
  let attemptedWrite = false;
  let confirmedOwnership = false;
  let primaryError;

  async function attempt(operation, action) {
    try {
      return await action();
    } catch (error) {
      failures.push(operation + ": " + error.message);
    }
  }

  async function issue() {
    const form = { grant_type: grantType, scope: SCOPE };
    if (grantType === "password") {
      Object.assign(form, { username: USERNAME, password: PASSWORD });
    }
    const response = await oauth.post(INTROSPECTION_OAUTH2 + "/access_token", { headers: credentials, form });
    const body = await response.json().catch(() => undefined);
    if (nonemptyString(body?.access_token)) accessToken = body.access_token;
    if (nonemptyString(body?.refresh_token)) refreshToken = body.refresh_token;
    requireStatus(response, 200, "Issue introspection fixture tokens");
    requireJsonMediaType(response, "Issue introspection fixture tokens");
    if (!accessToken) throw new Error("Issued access_token must be a nonempty string");
    if (grantType === "password") {
      if (!refreshToken) throw new Error("Issued refresh_token must be a nonempty string");
      if (refreshToken === accessToken) throw new Error("Issued access_token and refresh_token must differ");
    }
  }

  async function introspect(token, hint, method, operation) {
    const fields = { token };
    if (hint !== undefined) fields.token_type_hint = hint;
    const endpoint = INTROSPECTION_OAUTH2 + "/introspect";
    const response = method === "GET"
      ? await oauth.get(endpoint, { headers: credentials, params: fields })
      : await oauth.post(endpoint, { headers: credentials, form: fields });
    requireStatus(response, 200, operation);
    requireJsonMediaType(response, operation);
    return response.json();
  }

  const matchesClient = (record) => record?._id === id || clientNames(record).includes(name);
  const ownsClient = (record) => record?._id === id && clientNames(record).includes(name);

  async function listClients() {
    const response = await admin.get(OAUTH2_CLIENTS, { headers, params: { _queryFilter: "true" } });
    requireStatus(response, 200, "List OAuth2 clients");
    requireJsonMediaType(response, "List OAuth2 clients");
    const result = (await response.json())?.result;
    if (!Array.isArray(result) || !result.every((record) => nonemptyString(record?._id))) {
      throw new Error("OAuth2 client collection must contain a result array with client IDs");
    }
    return result;
  }

  async function readClientBeforeCleanup() {
    const read = () => admin.get(url, { headers });
    let response = await read().catch(() => undefined);
    if (isTransientFailure(response) || (response.status() === 404 && confirmedOwnership)) {
      response = await read();
    }
    if (response.status() === 404) return null;
    requireStatus(response, 200, "Read OAuth2 client before cleanup");
    requireJsonMediaType(response, "Read OAuth2 client before cleanup");
    const record = await response.json();
    if (!isJsonObject(record)) throw new Error("OAuth2 client read must return a JSON object");
    return record;
  }

  async function confirmClientOwnership() {
    const record = await attempt("Verify OAuth2 client ownership", readClientBeforeCleanup);
    if (record) {
      if (ownsClient(record)) return true;
      failures.push("Cleanup found an OAuth2 client with an unexpected ID or name");
      return false;
    }

    const matches = await attempt("Find OAuth2 client in collection", async () => (await listClients()).filter(matchesClient));
    if (matches?.length) {
      if (record === null) {
        failures.push("Cleanup GET returned HTTP 404 for a client still present in the OAuth2 client collection");
      }
      if (matches.every(ownsClient)) return true;
      failures.push("Cleanup found a collection entry with an unexpected OAuth2 client ID or name");
      return false;
    }
    if (matches && confirmedOwnership) {
      failures.push("Previously verified OAuth2 client is missing from the OAuth2 client collection before cleanup");
    }
    return confirmedOwnership;
  }

  async function readTokenState(token, type) {
    const body = await introspect(token, type, "POST", "Inspect " + type + " during cleanup");
    if (isInactiveIntrospection(body)) return false;
    if (body?.active === true && body.client_id === id && body.token_type === type) return true;
    throw new Error("Expected an inactive token or an active token issued to the test client");
  }

  async function checkTokenBeforeRevocation(token, type) {
    await attempt("Read " + type + " before cleanup", async () => {
      if (!(await readTokenState(token, type))) throw new Error("Issued token is already inactive");
    });
  }

  async function revokeAndVerify(token, type) {
    for (let retry = 0; retry < 2; retry++) {
      let response, revocationError;
      try {
        response = await oauth.post(INTROSPECTION_OAUTH2 + "/token/revoke", {
          form: { token, client_id: id, client_secret: secret },
        });
        requireStatus(response, 200, "Revoke token");
        requireJsonMediaType(response, "Revoke token");
        const body = await response.json();
        if (!isJsonObject(body) || Object.keys(body).length !== 0) {
          throw new Error("Revocation must return an empty JSON object");
        }
      } catch (error) {
        revocationError = error;
      }
      const active = await attempt("Verify " + type + " cleanup", () => readTokenState(token, type));
      if (retry === 0 && revocationError && isTransientFailure(response) && active === true) continue;
      if (revocationError) failures.push("Revoke " + type + ": " + revocationError.message);
      if (active === true) failures.push("Verify " + type + " cleanup: Token is still active");
      return;
    }
  }

  try {
    admin = unreportedRequest.newContext(options);
    oauth = unreportedRequest.newContext(options);
    const adminToken = await runClientSetup(admin, "Authenticate administrator", async (safeRequest) => {
      const token = await getAdminToken(safeRequest);
      if (!nonemptyString(token)) throw new Error("Administrator session is missing");
      return token;
    });
    headers = adminHeaders(adminToken, AGENT_VERSION);
    const provider = await runClientSetup(admin, "Ensure root OAuth2 provider",
      (safeRequest) => ensureOAuth2Provider(adminToken, safeRequest));
    if (grantType === "password" && (await provider.json())?.coreOAuth2Config?.issueRefreshToken !== true) {
      throw new Error(`Provider must enable issueRefreshToken for the password grant fixture (HTTP ${provider.status()})`);
    }
    attemptedWrite = true;
    const written = await runClientSetup(admin, "Write OAuth2 client", (safeRequest) => ensureClient(adminToken, safeRequest, id, {
      userpassword: secret,
      [CLIENT_NAME]: ["[0]=" + name],
      [CLIENT_ATTR + "clientType"]: "Confidential",
      [CLIENT_ATTR + "grantTypes"]: ["[0]=" + (grantType ?? "client_credentials")],
      [CLIENT_ATTR + "tokenEndPointAuthMethod"]: "client_secret_basic",
      [CLIENT_ATTR + "scopes"]: ["[0]=" + SCOPE],
      sunIdentityServerDeviceStatus: "Active",
    }));
    confirmedOwnership = ownsClient(await written.json());
    if (!confirmedOwnership) throw new Error("OAuth2 client write did not confirm the test ID and name");
    if (grantType) await issue();
    await run({ id, accessToken, refreshToken, introspect });
  } catch (error) {
    primaryError = error;
    throw error;
  } finally {
    if (attemptedWrite && await confirmClientOwnership()) {
      if (accessToken) await checkTokenBeforeRevocation(accessToken, "access_token");
      if (refreshToken) {
        await checkTokenBeforeRevocation(refreshToken, "refresh_token");
        await revokeAndVerify(refreshToken, "refresh_token");
      }
      // Refresh revocation can leave linked access tokens behind, and revoking an absent token is safe.
      if (accessToken) await revokeAndVerify(accessToken, "access_token");
      await attempt("Delete OAuth2 client", async () => {
        let response, deletionError;
        try {
          response = await admin.delete(url, { headers });
        } catch (error) {
          deletionError = error;
        }
        if (isTransientFailure(response)) {
          const record = await attempt("Verify OAuth2 client ownership before retry", readClientBeforeCleanup);
          if (record && ownsClient(record)) {
            response = await admin.delete(url, { headers });
            deletionError = undefined;
          } else if (record) {
            failures.push("Cleanup found an OAuth2 client with an unexpected ID or name before retry");
          }
        }
        if (deletionError) throw deletionError;
        if (!response.ok()) throw new Error("HTTP " + response.status());
      });
      await attempt("Read OAuth2 client after cleanup", async () => {
        requireStatus(await admin.get(url, { headers }), 404, "Read deleted OAuth2 client");
      });
      await attempt("Verify OAuth2 client collection after cleanup", async () => {
        if ((await listClients()).some(matchesClient)) throw new Error("Test OAuth2 client is still present");
      });
    }
    if (oauth) await attempt("Dispose OAuth2 request context", () => oauth.dispose());
    if (admin) await attempt("Dispose administrator request context", () => admin.dispose());
    if (failures.length) {
      throw new Error("OAuth2 fixture cleanup failed:\n" + failures.join("\n"), { cause: primaryError });
    }
  }
}

/**
 * A Playwright request context whose cookie jar holds the given session, and nothing else.
 *
 * On /oauth2/authorize the `iPlanetDirectoryPro` HEADER does not authenticate -- only the COOKIE does.
 */
export function sessionContext(apiRequest, ssoToken, extraCookies = []) {
  const domain = new URL(OPENAM_BASE).hostname;
  return apiRequest.newContext({
    ignoreHTTPSErrors: true,
    storageState: {
      cookies: [{ name: "iPlanetDirectoryPro", value: ssoToken }, ...extraCookies].map((cookie) => ({
        ...cookie, domain, path: "/", expires: -1, httpOnly: false, secure: false, sameSite: "Lax",
      })),
      origins: [],
    },
  });
}

/**
 * Runs a full authorization_code + PKCE exchange.
 *
 * Returns `{code, tokens}`: the authorization code the authorize step issued, and the token response. The code
 * is kept apart from the token response so a caller can compare that response's member set whole.
 *
 * @param context a request context whose jar carries the resource owner's session
 */
export async function authorizationCodeTokens(context, { clientId, clientSecret, scope, nonce } = {}) {
  const { verifier, challenge } = await pkce();
  const params = new URLSearchParams({
    response_type: "code",
    client_id: clientId,
    redirect_uri: REDIRECT_URI,
    scope,
    state: "fixture-state",
    code_challenge: challenge,
    code_challenge_method: "S256",
  });
  if (nonce) params.set("nonce", nonce);

  const authorize = await context.get(`${OPENAM_BASE}/oauth2/authorize?${params}`, { maxRedirects: 0 });
  if (authorize.status() !== 302) {
    throw new Error(`authorize did not redirect: ${authorize.status()} ${await authorize.text()}`);
  }
  // An authorize-side error is a 302 too: to redirect_uri?error=... or to the login page. Report its Location
  // here; without a code the token endpoint would only answer invalid_grant, which hides the cause.
  const location = authorize.headers()["location"];
  const code = new URL(location, OPENAM_BASE).searchParams.get("code");
  if (!code) {
    throw new Error(`authorize redirected without a code: ${location}`);
  }

  const token = await context.post(`${OPENAM_BASE}/oauth2/access_token`, {
    headers: { Authorization: basicAuth(clientId, clientSecret) },
    form: { grant_type: "authorization_code", code, redirect_uri: REDIRECT_URI, code_verifier: verifier },
  });
  if (!token.ok()) {
    throw new Error(`token exchange failed: ${token.status()} ${await token.text()}`);
  }
  return { code, tokens: await token.json() };
}

/** A token for the given resource-server client at the requested scope, via the ROPC grant. */
async function resourceServerToken(request, clientId, username, password, scope) {
  const response = await request.post(`${OPENAM_BASE}/oauth2/access_token`, {
    headers: { Authorization: basicAuth(clientId, RS_CLIENT_SECRET) },
    form: { grant_type: "password", username, password, scope },
  });
  if (!response.ok()) {
    throw new Error(`${scope} token request failed: ${response.status()} ${await response.text()}`);
  }
  return (await response.json()).access_token;
}

/** Protection API token -- what a resource server presents to /oauth2/resource_set and /uma/permission_request. */
export async function protectionApiToken(request, clientId, username, password) {
  return resourceServerToken(request, clientId, username, password, "uma_protection");
}

/** Authorization API token -- what a requesting party presents to /uma/authz_request. */
export async function authorizationApiToken(request, clientId, username, password) {
  return resourceServerToken(request, clientId, username, password, "uma_authorization");
}

