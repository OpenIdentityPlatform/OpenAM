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
 * Shared OAuth2/UMA fixtures for the e2e specs. Every helper is idempotent and safe when several spec files
 * call it in parallel -- Playwright runs spec FILES in parallel, so "create if absent, widen if present" is
 * the only safe shape here.
 */

import { OPENAM_BASE } from "./openam-commons.mjs";

export const REALM = "root";
export const SCOPE = "profile";
export const REDIRECT_URI = "http://app.invalid/cb";

/*
 * Client ids are PER SPEC FILE, not shared. Playwright runs spec files in parallel, and rewriting a client
 * invalidates the tokens already issued against it -- so two files sharing one client flake as soon as their
 * beforeAll hooks interleave. Each file owning its own clients removes the coupling entirely.
 */
export const RS_CLIENT_SECRET = "rs-secret-0123456789-0123456789-0123456789";

/** Scopes the /uma spec needs the provider to issue. */
const REQUIRED_SCOPES = ["uma_protection", "uma_authorization"];

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
 * Ensures the realm has an OAuth2 provider, and returns the response holding its configuration.
 *
 * This lives here rather than in a single spec file because Playwright runs spec FILES in parallel: if the
 * only creator were one spec's `beforeAll`, every other spec racing it on a cold container would read a 404.
 *
 * The payload holds the three settings that oauth2-test.spec.mjs and device-flow-test.spec.mjs create the
 * provider with, so the provider ends up the same whichever spec file creates it first.
 */
export async function ensureOAuth2Provider(adminToken, request) {
  const url = `${OPENAM_BASE}/json/realms/${REALM}/realm-config/services/oauth-oidc`;
  const existing = await request.get(url, { headers: adminHeaders(adminToken, CONFIG_VERSION) });
  if (existing.ok()) {
    return existing;
  }
  if (existing.status() !== 404) {
    throw new Error(`OAuth2 provider not readable: ${existing.status()} ${await existing.text()}`);
  }
  const created = await request.post(`${url}?_action=create`, {
    headers: adminHeaders(adminToken, CONFIG_VERSION),
    data: {
      advancedOAuth2Config: { clientsCanSkipConsent: true, supportedScopes: [SCOPE], defaultScopes: [SCOPE] },
    },
  });
  // A concurrent spec file may have created it between our GET and POST -- that is success, not failure.
  if (!created.ok() && created.status() !== 409) {
    throw new Error(`Failed to create OAuth2 provider: ${created.status()} ${await created.text()}`);
  }
  console.log("OAuth2 provider service created");
  return request.get(url, { headers: adminHeaders(adminToken, CONFIG_VERSION) });
}

/**
 * Brings the realm's OAuth2 provider up to what the specs need -- creating it first if absent -- and does
 * nothing at all once it is already there. Two invariants, checked independently:
 *
 *   1. `uma_protection` / `uma_authorization` are issuable (only ever ADDS scopes).
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
  if (!current.ok()) {
    throw new Error(`OAuth2 provider not readable: ${current.status()} ${await current.text()}`);
  }
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
  const url = `${OPENAM_BASE}/json/realms/${REALM}/realm-config/services/uma`;
  const existing = await request.get(url, { headers: adminHeaders(adminToken, CONFIG_VERSION) });
  if (existing.ok()) {
    console.log("UMA provider service already exists");
    return;
  }
  // Only a 404 means "absent". Falling through on a 401 or a 500 would report the same failure one call
  // later as `Failed to create UMA provider`, pointing the reader at creation instead of the real cause.
  if (existing.status() !== 404) {
    throw new Error(`UMA provider not readable: ${existing.status()} ${await existing.text()}`);
  }
  const created = await request.post(`${url}?_action=create`, {
    headers: adminHeaders(adminToken, CONFIG_VERSION),
    data: {},
  });
  // A concurrent spec file may have created it between our GET and POST -- that is success, not failure.
  if (!created.ok() && created.status() !== 409) {
    throw new Error(`Failed to create UMA provider: ${created.status()} ${await created.text()}`);
  }
  console.log("UMA provider service created");
}

/**
 * Writes an OAuth2 client. `data` is the full agent representation and the PUT is a full replace, so this
 * runs unconditionally rather than create-if-absent: a client left over from an earlier revision of these
 * fixtures (different secret, missing scope) would otherwise survive and fail the specs in confusing ways.
 * Repeating the same PUT concurrently from two spec files is harmless.
 */
async function ensureClient(adminToken, request, clientId, data) {
  const url = `${OPENAM_BASE}/json/realms/${REALM}/realm-config/agents/OAuth2Client/${clientId}`;
  const written = await request.put(url, { headers: adminHeaders(adminToken, AGENT_VERSION), data });
  if (!written.ok()) {
    throw new Error(`Failed to write client ${clientId}: ${written.status()} ${await written.text()}`);
  }
  console.log(`OAuth2 client "${clientId}" written (${written.status()})`);
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
 * this returns immediately.
 */
export async function warmUpResourceSetStore(request, pat) {
  const deadline = Date.now() + 30_000;
  for (let attempt = 1; ; attempt++) {
    const response = await request.post(`${OPENAM_BASE}/oauth2/resource_set`, {
      headers: { Authorization: `Bearer ${pat}`, "Content-Type": "application/json" },
      data: { name: uniqueName("warmup"), scopes: ["read"] },
    });
    if (response.status() === 201) {
      if (attempt > 1) {
        console.log(`[fixtures] resource-set store ready after ${attempt} attempts`);
      }
      await deleteResourceSet(request, pat, (await response.json())._id);
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
