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

import { randomUUID } from "node:crypto";
import { test, expect } from "@playwright/test";
import { OPENAM_BASE, getAdminToken, PASSWORD, USERNAME } from "../common/openam-commons.mjs";
import {
  ensureProviderConfig, ensureResourceServerClient, ensureUmaProvider, protectionApiToken,
  registerResourceSet, uniqueName, unreportedRequest, warmUpResourceSetStore,
} from "../common/oauth2-fixtures.mjs";

const CLIENT_ID = "test_client_rs_b5";
const RESOURCE_SET = `${OPENAM_BASE}/oauth2/resource_set`;
const REALM = `${OPENAM_BASE}/json/realms/root`;
const RESOURCE_TYPES = `${REALM}/resourcetypes`;

test.use({ trace: "off", screenshot: "off" });

test.describe("/oauth2/resource_set", () => {
  const runPrefix = `B5 ${randomUUID()} `;
  const ownedIds = new Set();
  let request;
  let adminToken;
  let pat;
  let cleanupResourceTypeReady = false;

  const itemUrl = (id) => `${RESOURCE_SET}/${id}`;
  const bearer = (headers = {}) => ({ Authorization: `Bearer ${pat}`, ...headers });
  const resourceTypeHeaders = () => ({ iPlanetDirectoryPro: adminToken, "Accept-API-Version": "resource=1.0" });

  test.beforeAll(async ({ ignoreHTTPSErrors }) => {
    request = unreportedRequest.newContext({ ignoreHTTPSErrors, maxRedirects: 0 });
    adminToken = await getAdminToken(request);
    expect(Boolean(adminToken), "Admin authentication must return a session").toBe(true);
    await ensureProviderConfig(adminToken, request);
    await ensureUmaProvider(adminToken, request);
    await ensureResourceServerClient(adminToken, request, CLIENT_ID);
    pat = await protectionApiToken(request, CLIENT_ID, USERNAME, PASSWORD);
    expect(Boolean(pat), "The resource server must obtain a protection API token").toBe(true);
    await warmUpResourceSetStore(request, pat, async (id) => {
      expect(id, "Warm-up resource set ID").toBeTruthy();
      ownedIds.add(id);
      await ensureCleanupResourceType();
      await deleteAndVerify(id, "*");
    }, `${runPrefix}warmup`);
  });

  test.afterAll(async () => {
    try {
      if (!pat) return;

      const failures = [];
      // A POST can leave a stored resource without returning its ID. Discover it by name.
      const listedIds = await listResourceSetIds().catch((error) => {
        failures.push(`List resource sets for cleanup: ${error.message}`);
        return [];
      });

      // Deletes for one client update the same UMA application, so perform them sequentially.
      for (const id of new Set([...ownedIds, ...listedIds])) {
        try {
          let resource;
          if (!ownedIds.has(id)) {
            const response = await request.get(itemUrl(id), { headers: bearer() });
            if (response.status() === 404) continue;
            expect(response.status(), "Read resource set for cleanup").toBe(200);
            resource = await response.json();
            if (!resource.name.startsWith(runPrefix)) continue;

            ownedIds.add(id);
          }

          await ensureCleanupResourceType().catch((error) => {
            failures.push(`${id}: Prepare UMA resource types for cleanup: ${error.message}`);
          });
          const deleted = await deleteForCleanup(id, resource);
          if (deleted.status() !== 204 && deleted.status() !== 404) {
            failures.push(`${id}: DELETE returned ${deleted.status()}`);
          }
        } catch (error) {
          failures.push(`${id}: ${error.message}`);
        }
      }

      if (ownedIds.size) {
        try {
          const remaining = (await listResourceSetIds()).filter((id) => ownedIds.has(id));
          expect.soft(remaining, "Resource sets remaining after cleanup").toEqual([]);
        } catch (error) {
          failures.push(`Verify resource set cleanup: ${error.message}`);
        }
      }
      expect(failures, "Resource set cleanup failures").toEqual([]);
    } finally {
      await request?.dispose();
    }
  });

  async function listResourceSetIds() {
    const list = await request.get(`${RESOURCE_SET}/?_queryId=*`, { headers: bearer() });
    expect(list.status(), "List resource sets").toBe(200);
    const ids = await list.json();
    expect(Array.isArray(ids), "Resource set list must be a JSON array").toBe(true);
    return ids;
  }

  async function deleteForCleanup(id, resource) {
    const response = await remove(id, "*");
    if (response.status() < 500 && (response.status() !== 400 || (await response.json()).error !== "server_error")) {
      return response;
    }

    // A failed registration hook may leave no resource type for the deletion hook to remove.
    const headers = resourceTypeHeaders();
    const type = await request.get(`${RESOURCE_TYPES}/${id}`, { headers });
    if (type.status() !== 404) return response;

    const { name, scopes } = resource ?? await (await read(id)).json();
    const restored = await request.post(`${RESOURCE_TYPES}?_action=create`, {
      headers,
      data: {
        uuid: id, name: `${name} - ${id}`, patterns: ["uma://-*-"],
        actions: Object.fromEntries(scopes.map((scope) => [scope, true])),
      },
    });
    expect(restored.status(), "Restore the missing UMA resource type for cleanup").toBe(201);
    return remove(id, "*");
  }

  async function ensureCleanupResourceType() {
    if (cleanupResourceTypeReady) return;

    // An empty resource type list leaves the last reference in the UMA application.
    // Keep the built-in URL type attached so every test resource set can be removed.
    const types = await request.get(RESOURCE_TYPES, {
      headers: resourceTypeHeaders(),
      params: { _queryFilter: 'name eq "URL"' },
    });
    expect(types.status(), "Read the built-in URL resource type").toBe(200);
    const { result } = await types.json();
    expect(result).toHaveLength(1);
    const { uuid } = result[0];
    expect(uuid).toBeTruthy();

    const url = `${REALM}/applications/${CLIENT_ID}`;
    const headers = { iPlanetDirectoryPro: adminToken, "Accept-API-Version": "resource=2.1" };
    const application = await request.get(url, { headers });
    expect(application.status(), "Read the B5 UMA application").toBe(200);
    const data = await application.json();
    const resourceTypeUuids = data.resourceTypeUuids ?? [];
    if (!resourceTypeUuids.includes(uuid)) {
      const updated = await request.put(url, {
        headers, data: { ...data, resourceTypeUuids: [...resourceTypeUuids, uuid] },
      });
      expect(updated.status(), "Update the B5 UMA application").toBe(200);
    }
    cleanupResourceTypeReady = true;
  }

  async function create(name, extra) {
    const { response, name: registeredName } = await registerResourceSet(request, pat, runPrefix + name, extra);
    expect(response.status(), "Resource set registration").toBe(201);
    const registration = await response.json();
    expect(registration._id).toBeTruthy();
    ownedIds.add(registration._id);
    return { id: registration._id, name: registeredName, registration, etag: response.headers()["etag"] };
  }

  async function remove(id, ifMatch) {
    return request.delete(itemUrl(id), { headers: bearer({ "If-Match": ifMatch }) });
  }

  async function deleteAndVerify(id, ifMatch) {
    expect((await remove(id, ifMatch)).status(), "Resource set deletion").toBe(204);
    const gone = await request.get(itemUrl(id), { headers: bearer() });
    expect(gone.status(), "Read the deleted resource set").toBe(404);
    const error = await gone.json();
    expect(error.error).toBe("not_found");
    expect(error.error_description).toContain(id);
    ownedIds.delete(id);
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
    const { id, name, registration, etag } = await create("lifecycle", { type });
    expect(etag).toBeTruthy();
    expect(registration.user_access_policy_uri).toContain(`#uma/share/${id}`);

    const resource = await read(id);
    expect(await resource.json()).toMatchObject({ name, scopes: ["read", "write"], type });
    expect(resource.headers()["etag"]).toBeTruthy();

    expect(await listResourceSetIds()).toContain(id);

    const update = { name: `${name} renamed`, scopes: ["read"] };
    const noIfMatch = await request.put(itemUrl(id), { headers: bearer(), data: update });
    expect(noIfMatch.status()).toBe(400);
    expect((await noIfMatch.json()).error_description).toContain("Require If-Match header to update Resource Set");

    const updated = await request.put(itemUrl(id), {
      headers: bearer({ "If-Match": "*" }), data: update,
    });
    expect(updated.status()).toBe(200);
    expect(await (await read(id)).json()).toMatchObject(update);

    await deleteAndVerify(id, "*");
  });

  test("rejects stale If-Match on update and delete", async () => {
    const { id, name } = await create("stale write");
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
    const { id, name } = await create("matching update", { labels: ["alpha", "beta"] });
    const before = await read(id);
    expect((await before.json()).labels.sort()).toEqual(["alpha", "beta"]);
    const etag = before.headers()["etag"];
    expect(etag).toMatch(/^W\/"-?\d+"$/);

    const update = { name: `${name} updated`, scopes: ["read"] };
    const updated = await request.put(itemUrl(id), {
      headers: bearer({ "If-Match": etag }), data: update,
    });
    expect(updated.status()).toBe(200);
    const updatedEtag = updated.headers()["etag"];
    expect(updatedEtag).toMatch(/^W\/"-?\d+"$/);
    expect(updatedEtag).not.toBe(etag);
    const afterUpdate = await read(id);
    expect(afterUpdate.headers()["etag"]).toBe(updatedEtag);
    expect(await afterUpdate.json()).toMatchObject({ ...update, labels: [] });

    const { id: deleteId } = await create("matching delete", { labels: ["alpha", "beta"] });
    const deleteEtag = await currentEtag(deleteId);
    expect(deleteEtag).toMatch(/^W\/"-?\d+"$/);
    await deleteAndVerify(deleteId, deleteEtag);
  });

  test("returns 304 with the ETag on matching If-None-Match for GET and HEAD", async () => {
    const { id } = await create("cached resource", { labels: ["alpha", "beta"] });
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
    const { id, name } = await create("conditional read", { labels: ["alpha"] });
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

test.describe("/oauth2/resource_set bearer authentication", () => {
  for (const [scenario, headers] of [
    ["missing", {}],
    ["unknown", { Authorization: "Bearer not-a-real-token" }],
  ]) {
    test(`rejects registration when the bearer token is ${scenario}`, async ({ ignoreHTTPSErrors }) => {
      const request = unreportedRequest.newContext({ ignoreHTTPSErrors, maxRedirects: 0 });
      try {
        const response = await request.post(RESOURCE_SET, {
          headers, data: { name: uniqueName("B5 unauthenticated"), scopes: ["read"] },
        });
        expect(response.status()).toBe(401);
        expect(await response.json()).toEqual({ error: "invalid_token", error_description: expect.any(String) });
        // The target is WWW-Authenticate: Bearer.
        expect(response.headers()["www-authenticate"]).toBeUndefined();
      } finally {
        await request.dispose();
      }
    });
  }
});
