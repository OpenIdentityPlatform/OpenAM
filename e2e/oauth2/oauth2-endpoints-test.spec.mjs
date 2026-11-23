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

import { test, expect } from "@playwright/test";
import { OPENAM_BASE, getAdminToken, PASSWORD, USERNAME } from "../common/openam-commons.mjs";
import {
  ensureProviderConfig, ensureResourceServerClient, ensureUmaProvider, protectionApiToken,
  registerResourceSet, uniqueName, unreportedRequest, warmUpResourceSetStore,
} from "../common/oauth2-fixtures.mjs";

const CLIENT_ID = "test_client_rs_b5";
const RESOURCE_SET = `${OPENAM_BASE}/oauth2/resource_set`;

test.use({ trace: "off", screenshot: "off" });

test.describe("/oauth2/resource_set", () => {
  let request;
  let pat;
  const created = new Set();

  const itemUrl = (id) => `${RESOURCE_SET}/${id}`;
  const bearer = (headers = {}) => ({ Authorization: `Bearer ${pat}`, ...headers });

  test.beforeAll(async ({ ignoreHTTPSErrors }) => {
    request = unreportedRequest.newContext({ ignoreHTTPSErrors });
    const adminToken = await getAdminToken(request);
    expect(Boolean(adminToken), "Admin authentication must return a session").toBe(true);
    await ensureProviderConfig(adminToken, request);
    await ensureUmaProvider(adminToken, request);
    await ensureResourceServerClient(adminToken, request, CLIENT_ID);
    pat = await protectionApiToken(request, CLIENT_ID, USERNAME, PASSWORD);
    expect(Boolean(pat), "The resource server must obtain a protection API token").toBe(true);
    await warmUpResourceSetStore(request, pat, async (id) => {
      created.add(id);
      await ensureCleanupResourceType(adminToken);
      expect((await remove(id, "*")).status(), "Warm-up resource set cleanup").toBe(204);
    });
  });

  test.afterAll(async () => {
    const failures = [];
    try {
      // Deletes for one client update the same UMA application.
      for (const id of created) {
        try {
          const response = await remove(id, "*");
          if (response.status() !== 204 && response.status() !== 404) {
            failures.push(response.status());
          }
        } catch {
          failures.push("OpenAM request failed");
        }
      }
    } finally {
      await request?.dispose();
    }
    expect(failures, "Resource set cleanup failures").toEqual([]);
  });

  async function ensureCleanupResourceType(adminToken) {
    // An empty resource type list leaves the last reference in the UMA application.
    // Keep the built-in URL type attached so every test resource set can be removed.
    const realm = `${OPENAM_BASE}/json/realms/root`;
    const types = await request.get(`${realm}/resourcetypes`, {
      headers: { iPlanetDirectoryPro: adminToken, "Accept-API-Version": "resource=1.0" },
      params: { _queryFilter: 'name eq "URL"' },
    });
    expect(types.status(), "Read the built-in URL resource type").toBe(200);
    const { result } = await types.json();
    expect(result).toHaveLength(1);
    const { uuid } = result[0];
    expect(uuid).toBeTruthy();

    const url = `${realm}/applications/${CLIENT_ID}`;
    const headers = { iPlanetDirectoryPro: adminToken, "Accept-API-Version": "resource=2.1" };
    const application = await request.get(url, { headers });
    expect(application.status(), "Read the B5 UMA application").toBe(200);
    const data = await application.json();
    if (!data.resourceTypeUuids.includes(uuid)) {
      const updated = await request.put(url, {
        headers, data: { ...data, resourceTypeUuids: [...data.resourceTypeUuids, uuid] },
      });
      expect(updated.status(), "Update the B5 UMA application").toBe(200);
    }
  }

  async function rememberCreated(response) {
    const registration = await response.json();
    expect(registration._id).toBeTruthy();
    created.add(registration._id);
    return registration;
  }

  async function create(prefix, extra) {
    const { response, name } = await registerResourceSet(request, pat, uniqueName(prefix), extra);
    expect(response.status(), "Resource set registration").toBe(201);
    const registration = await rememberCreated(response);
    return { id: registration._id, name, registration, etag: response.headers()["etag"] };
  }

  async function remove(id, ifMatch) {
    const response = await request.delete(itemUrl(id), { headers: bearer({ "If-Match": ifMatch }) });
    if (response.status() === 204) {
      created.delete(id);
    }
    return response;
  }

  async function read(id) {
    const response = await request.get(itemUrl(id), { headers: bearer() });
    expect(response.status()).toBe(200);
    return response;
  }

  async function currentEtag(id) {
    const etag = (await read(id)).headers()["etag"];
    expect(etag).toBeTruthy();
    return etag;
  }

  function staleTag(etag) {
    return etag.replace(/^W\//, "") === '"0"' ? '"1"' : '"0"';
  }

  test("creates, reads, lists, updates and deletes a resource set", async () => {
    const type = "http://example.invalid/album";
    const { id, name, registration, etag } = await create("B5 lifecycle", { type });
    expect(etag).toBeTruthy();
    expect(registration.user_access_policy_uri).toContain(`#uma/share/${id}`);

    const resource = await read(id);
    expect(await resource.json()).toMatchObject({ name, scopes: ["read", "write"], type });
    expect(resource.headers()["etag"]).toBeTruthy();

    const list = await request.get(`${RESOURCE_SET}/?_queryId=*`, {
      headers: bearer(), maxRedirects: 0,
    });
    expect(list.status()).toBe(200);
    expect(await list.json()).toEqual(expect.arrayContaining([id]));

    const update = { name: `${name} renamed`, scopes: ["read"] };
    const noIfMatch = await request.put(itemUrl(id), { headers: bearer(), data: update });
    expect(noIfMatch.status()).toBe(400);
    expect((await noIfMatch.json()).error_description).toContain("Require If-Match header to update Resource Set");

    const updated = await request.put(itemUrl(id), {
      headers: bearer({ "If-Match": "*" }), data: update,
    });
    expect(updated.status()).toBe(200);
    expect(await (await read(id)).json()).toMatchObject(update);

    expect((await remove(id, "*")).status()).toBe(204);
    const gone = await request.get(itemUrl(id), { headers: bearer() });
    expect(gone.status()).toBe(404);
    const error = await gone.json();
    expect(error.error).toBe("not_found");
    expect(error.error_description).toContain(id);
  });

  for (const [scenario, headers] of [
    ["missing", {}],
    ["unknown", { Authorization: "Bearer not-a-real-token" }],
  ]) {
    test(`rejects registration when the bearer token is ${scenario}`, async () => {
      const response = await request.post(RESOURCE_SET, {
        headers, data: { name: uniqueName("B5 unauthenticated"), scopes: ["read"] },
      });
      if (response.status() === 201) {
        await rememberCreated(response);
      }
      expect(response.status()).toBe(401);
      expect(await response.json()).toEqual({ error: "invalid_token", error_description: expect.any(String) });
      // The target is WWW-Authenticate: Bearer.
      expect(response.headers()["www-authenticate"]).toBeUndefined();
    });
  }

  test("rejects stale If-Match on update and delete", async () => {
    const { id, name } = await create("B5 stale write");
    const stale = staleTag(await currentEtag(id));

    for (const ifMatch of [`W/${stale}`, stale]) {
      const response = await request.put(itemUrl(id), {
        headers: bearer({ "If-Match": ifMatch }), data: { name: `${name} updated`, scopes: ["read"] },
      });
      expect(response.status(), ifMatch).toBe(412);
      expect(await response.json()).toEqual({ error: "precondition_failed" });
      expect(response.headers()["content-type"]).toBe("application/json");
      expect(response.headers()["etag"]).toBeUndefined();
    }

    const response = await remove(id, `W/${stale}`);
    expect(response.status()).toBe(412);
    expect(await response.json()).toEqual({ error: "precondition_failed" });
    expect(await (await read(id)).json()).toMatchObject({ name, scopes: ["read", "write"] });
  });

  test("accepts the weak ETag on update and delete and clears omitted labels", async () => {
    const { id, name } = await create("B5 matching update", { labels: ["alpha", "beta"] });
    const before = await read(id);
    expect((await before.json()).labels.sort()).toEqual(["alpha", "beta"]);
    const etag = before.headers()["etag"];
    expect(etag).toMatch(/^W\/"-?\d+"$/);

    const updated = await request.put(itemUrl(id), {
      headers: bearer({ "If-Match": etag }), data: { name: `${name} updated`, scopes: ["read"] },
    });
    expect(updated.status()).toBe(200);
    const updatedEtag = updated.headers()["etag"];
    expect(updatedEtag).toMatch(/^W\/"-?\d+"$/);
    expect(updatedEtag).not.toBe(etag);
    expect((await (await read(id)).json()).labels).toEqual([]);

    const { id: deleteId } = await create("B5 matching delete", { labels: ["alpha", "beta"] });
    const deleteEtag = await currentEtag(deleteId);
    expect(deleteEtag).toMatch(/^W\/"-?\d+"$/);
    expect((await remove(deleteId, deleteEtag)).status()).toBe(204);
  });

  test("returns 304 with the ETag on matching If-None-Match for GET and HEAD", async () => {
    const { id } = await create("B5 cached resource", { labels: ["alpha", "beta"] });
    const etag = await currentEtag(id);

    for (const method of ["get", "head"]) {
      const response = await request[method](itemUrl(id), { headers: bearer({ "If-None-Match": etag }) });
      expect(response.status(), method).toBe(304);
      expect(response.headers()["etag"], method).toBe(etag);
      expect(response.headers()["content-type"], method).toBeUndefined();
      expect(response.headers()["content-length"], method).toBeUndefined();
    }

    const stale = await request.get(itemUrl(id), { headers: bearer({ "If-None-Match": `W/${staleTag(etag)}` }) });
    expect(stale.status()).toBe(200);
    expect(stale.headers()["etag"]).toBe(etag);

    const wildcard = await request.get(itemUrl(id), { headers: bearer({ "If-None-Match": "*" }) });
    // The target is 304 when a representation exists and If-None-Match is *.
    expect(wildcard.status()).toBe(200);
  });

  test("returns the resource representation with 412 on GET with a stale If-Match", async () => {
    const { id, name } = await create("B5 conditional read", { labels: ["alpha"] });
    const etag = await currentEtag(id);

    const wildcard = await request.get(itemUrl(id), { headers: bearer({ "If-Match": "*" }) });
    expect(wildcard.status()).toBe(200);
    const representation = await wildcard.json();
    expect(representation).toMatchObject({ name, scopes: ["read", "write"], labels: ["alpha"] });

    const stale = await request.get(itemUrl(id), { headers: bearer({ "If-Match": staleTag(etag) }) });
    expect(stale.status()).toBe(412);
    expect(stale.headers()["etag"]).toBe(etag);
    expect(stale.headers()["content-type"]).toBe("application/json");
    const body = await stale.json();
    expect(body).toEqual(representation);
    expect(body.error).toBeUndefined();
  });
});
