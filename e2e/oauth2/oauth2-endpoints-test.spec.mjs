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
  basicAuth, ensureProviderConfig, ensureResourceServerClient, ensureUmaProvider, protectionApiToken,
  registerResourceSet, RS_CLIENT_SECRET, uniqueName, unreportedRequest, warmUpResourceSetStore,
} from "../common/oauth2-fixtures.mjs";

const CLIENT_ID = "test_client_rs_b5";
const RESOURCE_SET = `${OPENAM_BASE}/oauth2/resource_set`;
const WEAK_DECIMAL_ETAG = /^W\/"-?\d+"$/;
const REALM = `${OPENAM_BASE}/json/realms/root`;
const RESOURCE_TYPES = `${REALM}/resourcetypes`;

test.use({ trace: "off", screenshot: "off" });

test.describe("/oauth2/resource_set", () => {
  const runPrefix = `B5 ${randomUUID()} `;
  const ownedIds = new Set();
  const labelOrderName = "label order ";
  const labelOrderPrefix = runPrefix + labelOrderName;
  const labelOrderIds = new Set();
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
          if (labelOrderIds.has(id) || resource?.name.startsWith(labelOrderPrefix)) {
            const failure = await clearLabelsForCleanup(id, resource).catch(() => "Request or JSON parsing failed");
            if (failure) failures.push(`${id}: Clear labels for cleanup: ${failure}`);
          }
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

  async function expectResourceSetAbsent(name) {
    const rejectedName = runPrefix + name;
    for (const id of await listResourceSetIds()) {
      const resource = await request.get(itemUrl(id), { headers: bearer() });
      if (resource.status() === 404) continue;
      expect(resource.status(), "Read resource set for absence check").toBe(200);
      expect((await resource.json()).name, `Rejected POST must not create resource set ${id}`).not.toBe(rejectedName);
    }
  }

  async function clearLabelsForCleanup(id, resource) {
    if (!resource) {
      const current = await request.get(itemUrl(id), { headers: bearer() });
      if (current.status() === 404) return;
      if (current.status() !== 200) return `GET returned ${current.status()}`;
      resource = await current.json();
    }
    const { name, scopes, labels } = resource;
    if (!labels?.length) return;

    // Label-order fixtures share labels. Clear their associations before deleting each resource.
    const updated = await request.put(itemUrl(id), {
      headers: bearer({ "If-Match": "*" }), data: { name, scopes },
    });
    if (updated.status() !== 200) return `PUT returned ${updated.status()}`;
    const after = await request.get(itemUrl(id), { headers: bearer() });
    if (after.status() !== 200) return `GET after PUT returned ${after.status()}`;
    const { labels: remaining } = await after.json();
    if (!Array.isArray(remaining) || remaining.length !== 0) return "GET after PUT did not return empty labels";
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
    if (registeredName.startsWith(labelOrderPrefix)) labelOrderIds.add(registration._id);
    return { id: registration._id, name: registeredName, registration, etag: response.headers()["etag"] };
  }

  async function conditionalPost(url, name, headers) {
    const response = await request.post(url, {
      headers: bearer(headers), data: { name: runPrefix + name, scopes: ["read"] },
    });
    await trackCreatedResourceSet(response);
    return response;
  }

  async function trackCreatedResourceSet(response) {
    if (response.status() === 201) {
      const { _id: id } = await response.json();
      expect(typeof id === "string" && id.trim().length > 0, "Created resource set ID").toBe(true);
      ownedIds.add(id);
    }
  }

  function requiredIfMatchError(action) {
    return {
      error: "server_error",
      error_description: expect.stringContaining(`Require If-Match header to ${action} Resource Set`),
    };
  }

  async function expectRequiredIfMatchError(response, action, scenario) {
    expect(await response.json(), scenario).toEqual(requiredIfMatchError(action));
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

  test("parses If-Match tags and lists on update", async () => {
    // The target is strong If-Match comparison (RFC 9110 §13.1.1) and a strong ETag clients can use for updates.
    const forms = [
      ["current weak tag", (etag) => etag, 200],
      ["strong form of the current tag", (etag) => etag.slice(2), 200],
      ["current tag first", (etag) => `${etag}, W/"unmatched"`, 200],
      ["current tag second", (etag) => `W/"unmatched", ${etag}`, 200],
      ["no space after the comma", (etag) => `W/"unmatched",${etag}`, 200],
      ["wildcard", () => "*", 200],
      ["nonmatching tags", () => 'W/"unmatched", W/"another-unmatched"', 412],
      ["quoted empty tag", () => '""', 412],
      ["unquoted tag", (etag) => etag.slice(3, -1), 400],
      ["empty header", () => "", 400],
      ["invalid header", () => "!!!", 400],
    ];
    const shared = await create("rejected If-Match forms");
    for (const [scenario, makeTag, status] of forms) {
      const { id, name } = status === 200 ? await create(`If-Match ${scenario}`) : shared;
      const etag = await currentEtag(id);
      expect(etag, scenario).toMatch(WEAK_DECIMAL_ETAG);
      const response = await request.put(itemUrl(id), {
        headers: bearer({ "If-Match": makeTag(etag) }),
        data: { name: `${name} updated`, scopes: ["read"] },
      });
      expect(response.status(), scenario).toBe(status);
      if (status === 412) {
        expect(await response.json(), scenario).toEqual({ error: "precondition_failed" });
      } else if (status === 400) {
        await expectRequiredIfMatchError(response, "update", scenario);
      }
    }
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

  test("uses label order when validating the registration ETag", async () => {
    // The target is a creation ETag that can update the same version regardless of label order.
    // Read the store's order on this server and keep the control's labels registered for both cases.
    const control = await create(`${labelOrderName}control`, { labels: ["alpha", "beta"] });
    const { labels: storeOrder } = await (await read(control.id)).json();
    expect([...storeOrder].sort()).toEqual(["alpha", "beta"]);

    for (const [scenario, labels, matches] of [
      ["matching", storeOrder, true],
      ["reversed", [...storeOrder].reverse(), false],
    ]) {
      const { id, name, etag: postEtag } = await create(labelOrderName + scenario, { labels });
      const resource = await read(id);
      expect((await resource.json()).labels, scenario).toEqual(storeOrder);
      const getEtag = resource.headers()["etag"];
      expect(postEtag, `${scenario} POST ETag`).toMatch(WEAK_DECIMAL_ETAG);
      expect(getEtag, `${scenario} GET ETag`).toMatch(WEAK_DECIMAL_ETAG);
      if (matches) {
        expect(getEtag, scenario).toBe(postEtag);
      } else {
        expect(getEtag, scenario).not.toBe(postEtag);
      }

      const updated = await request.put(itemUrl(id), {
        headers: bearer({ "If-Match": postEtag }), data: { name: `${name} updated`, scopes: ["read"] },
      });
      expect(updated.status(), scenario).toBe(matches ? 200 : 412);
      if (!matches) expect(await updated.json()).toEqual({ error: "precondition_failed" });
    }
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

  test("requires If-Match on update", async () => {
    const { id, name } = await create("update without If-Match");
    const response = await request.put(itemUrl(id), {
      headers: bearer(), data: { name: `${name} updated`, scopes: ["read"] },
    });
    expect(response.status()).toBe(400);
    await expectRequiredIfMatchError(response, "update");
    expect(await (await read(id)).json()).toMatchObject({ name, scopes: ["read", "write"] });
  });

  test("returns weak decimal ETags for resource representations", async () => {
    const { id, name, etag: postEtag } = await create("weak decimal ETags");
    const getEtag = (await read(id)).headers()["etag"];
    const updated = await request.put(itemUrl(id), {
      headers: bearer({ "If-Match": "*" }), data: { name: `${name} updated`, scopes: ["read"] },
    });
    expect(updated.status()).toBe(200);
    expect(postEtag, "POST ETag").toMatch(WEAK_DECIMAL_ETAG);
    expect(getEtag, "GET ETag").toMatch(WEAK_DECIMAL_ETAG);
    expect(updated.headers()["etag"], "PUT ETag").toMatch(WEAK_DECIMAL_ETAG);
    expect(getEtag).toBe(postEtag);

    const list = await request.get(`${RESOURCE_SET}/?_queryId=*`, { headers: bearer() });
    expect(list.status()).toBe(200);
    expect(list.headers()["etag"]).toBeUndefined();
  });

  test("requires If-Match on delete", async () => {
    const { id } = await create("delete without If-Match");
    const response = await request.delete(itemUrl(id), { headers: bearer() });
    expect(response.status()).toBe(400);
    await expectRequiredIfMatchError(response, "delete");
    await read(id);
  });

  test("evaluates If-None-Match on update and delete", async () => {
    const { id, name } = await create("conditional write with If-None-Match");
    const url = itemUrl(id);
    const before = await read(id);
    const etag = before.headers()["etag"];
    expect(etag).toBeTruthy();
    const data = { name: `${name} updated`, scopes: ["read"] };
    const current = { "If-None-Match": etag };

    const refused = await request.put(url, { headers: bearer(current), data });
    expect(refused.status()).toBe(412);
    expect(await refused.json()).toEqual({ error: "precondition_failed" });

    const rejected = await request.put(url, {
      headers: bearer({ "If-None-Match": `W/${staleTag(etag)}` }), data,
    });
    expect(rejected.status()).toBe(400);
    await expectRequiredIfMatchError(rejected, "update");

    const undeleted = await request.delete(url, { headers: bearer(current) });
    expect(undeleted.status()).toBe(412);
    expect(await undeleted.json()).toEqual({ error: "precondition_failed" });

    const conflicted = await request.put(url, { headers: bearer({ ...current, "If-Match": etag }), data });
    expect(conflicted.status()).toBe(412);
    expect(await conflicted.json()).toEqual({ error: "precondition_failed" });

    const after = await read(id);
    expect(after.headers()["etag"]).toBe(etag);
    expect(await after.json()).toEqual(await before.json());
  });

  test("evaluates If-Match before creating a resource set", async () => {
    const star = await conditionalPost(RESOURCE_SET, "create with If-Match wildcard", { "If-Match": "*" });
    expect(star.status()).toBe(201);
    const rejectedName = "create with concrete If-Match";
    const concrete = await conditionalPost(RESOURCE_SET, rejectedName, { "If-Match": 'W/"0"' });
    expect(concrete.status()).toBe(412);
    expect(await concrete.json()).toEqual({ error: "precondition_failed" });
    await expectResourceSetAbsent(rejectedName);

    const { id } = await create("host for conditional create");
    const etag = await currentEtag(id);
    const atItem = await conditionalPost(itemUrl(id), "create at item with current If-Match", { "If-Match": etag });
    expect(atItem.status()).toBe(201);
    const { _id: createdId } = await atItem.json();
    expect(createdId).not.toBe(id);
    expect(createdId).not.toBe((await star.json())._id);

    const staleName = "create at item with stale If-Match";
    const stale = await conditionalPost(itemUrl(id), staleName, {
      "If-Match": `W/${staleTag(etag)}`,
    });
    expect(stale.status()).toBe(412);
    await expectResourceSetAbsent(staleName);
  });

  test("evaluates conditions on the collection without an ETag", async () => {
    const { id } = await create("conditional collection member");
    const etag = await currentEtag(id);
    const stale = await request.get(RESOURCE_SET, { headers: bearer({ "If-Match": etag }) });
    expect(stale.status()).toBe(412);
    expect(stale.headers()["content-type"]?.split(";")[0].trim()).toBe("application/json");
    const ids = await stale.json();
    expect(Array.isArray(ids)).toBe(true);
    expect(ids).toContain(id);
    expect(stale.headers()["etag"]).toBeUndefined();

    const star = await request.get(RESOURCE_SET, { headers: bearer({ "If-None-Match": "*" }) });
    expect(star.status()).toBe(304);
    expect(star.headers()["etag"]).toBeUndefined();
    expect(star.headers()["content-type"]).toBeUndefined();

    const concrete = await request.get(RESOURCE_SET, { headers: bearer({ "If-None-Match": etag }) });
    expect(concrete.status()).toBe(200);

    const rejectedName = "create with If-None-Match wildcard";
    const createIfAbsent = await conditionalPost(RESOURCE_SET, rejectedName, { "If-None-Match": "*" });
    expect(createIfAbsent.status()).toBe(412);
    expect(await createIfAbsent.json()).toEqual({ error: "precondition_failed" });
    await expectResourceSetAbsent(rejectedName);
  });

  test("compares If-None-Match tags by request method", async () => {
    // The target is weak comparison (RFC 9110 §13.1.2), with 304 on matching GET and 412 on matching PUT.
    const { id, name } = await create("If-None-Match tag comparison");
    const weak = await currentEtag(id);
    expect(weak).toMatch(WEAK_DECIMAL_ETAG);
    const headers = bearer({ "If-None-Match": weak.slice(2) });

    const resource = await request.get(itemUrl(id), { headers });
    expect(resource.status()).toBe(200);
    expect((await resource.json())._id).toBe(id);

    const updated = await request.put(itemUrl(id), {
      headers, data: { name: `${name} updated`, scopes: ["read"] },
    });
    expect(updated.status()).toBe(412);
    expect(await updated.json()).toEqual({ error: "precondition_failed" });

    // The target wildcard syntax is "*". Restlet still allows creation with "W/*".
    const weakStar = await conditionalPost(RESOURCE_SET, "create with weak wildcard", { "If-None-Match": "W/*" });
    expect(weakStar.status()).toBe(201);
  });

  test("rejects a duplicate resource set name", async () => {
    const name = "duplicate name";
    const first = await conditionalPost(RESOURCE_SET, name);
    expect(first.status(), "Initial resource set creation").toBe(201);

    const duplicate = await conditionalPost(RESOURCE_SET, name);
    expect(duplicate.status()).toBe(400);
    expect(duplicate.headers()["content-type"]?.split(";")[0].trim()).toBe("application/json");
    const body = await duplicate.json();
    expect(body.error).toBe("Bad Request");
    expect(body.error_description).toBe(`A shared item with the name '${runPrefix + name}' already exists`);
  });

  test("validates required resource set attributes", async () => {
    const cases = [
      ["missing name", { scopes: ["read"] },
        "Invalid Resource Set Description. Missing required attribute, 'name'."],
      ["missing scopes", { name: runPrefix + "missing scopes" },
        "Invalid Resource Set Description. Missing required attribute, 'scopes'."],
      ["non-array scopes", { name: runPrefix + "non-array scopes", scopes: "read" },
        "Invalid Resource Set Description. Required attribute, 'scopes', must be an array of Strings."],
      ["empty object", {},
        "Invalid Resource Set Description. Missing required attribute, 'name'."],
    ];
    for (const [scenario, data, description] of cases) {
      const response = await request.post(RESOURCE_SET, { headers: bearer(), data });
      await trackCreatedResourceSet(response);
      expect(response.status(), scenario).toBe(400);
      const body = await response.json();
      expect(body.error, scenario).toBe("bad_request");
      expect(body.error_description, scenario).toBe(description);
    }
  });

  test("applies If-Match to collection writes", async () => {
    const { id } = await create("control for collection writes");
    const before = await (await read(id)).json();
    const data = { name: runPrefix + "collection write", scopes: ["read"] };

    // A concrete tag fails against the untagged collection. The wildcard reaches the write with no item ID.
    for (const suffix of ["", "/"]) {
      const url = RESOURCE_SET + suffix;
      for (const [method, action] of [["put", "update"], ["delete", "delete"]]) {
        const scenario = `${method.toUpperCase()} /resource_set${suffix}`;
        const wildcard = await request[method](url, { headers: bearer({ "If-Match": "*" }), data });
        const wildcardScenario = `${scenario} with wildcard`;
        expect(wildcard.status(), wildcardScenario).toBe(400);
        const wildcardBody = await wildcard.json();
        expect(wildcardBody.error, wildcardScenario).toBe("server_error");
        expect(wildcardBody, wildcardScenario).not.toMatchObject(requiredIfMatchError(action));

        const tagged = await request[method](url, { headers: bearer({ "If-Match": '"12345"' }), data });
        expect(tagged.status(), `${scenario} with concrete tag`).toBe(412);
        expect(await tagged.json(), `${scenario} with concrete tag`).toEqual({ error: "precondition_failed" });

        const missing = await request[method](url, { headers: bearer(), data });
        expect(missing.status(), `${scenario} without If-Match`).toBe(400);
        await expectRequiredIfMatchError(missing, action, `${scenario} without If-Match`);
      }
    }
    expect(await (await read(id)).json()).toEqual(before);
  });

  test("isolates resource sets by owner", async ({ ignoreHTTPSErrors }) => {
    const username = `b5-owner-${randomUUID()}`;
    const password = `B5a-${randomUUID()}`;
    const usersUrl = `${REALM}/users`;
    const headers = { iPlanetDirectoryPro: adminToken, "Accept-API-Version": "resource=3.0, protocol=2.1" };
    const clientHeaders = { Authorization: basicAuth(CLIENT_ID, RS_CLIENT_SECRET) };
    const otherRequest = unreportedRequest.newContext({ ignoreHTTPSErrors, maxRedirects: 0 });
    let tokens;
    try {
      const created = await otherRequest.post(`${usersUrl}?_action=create`, {
        headers,
        data: {
          username, userpassword: password, mail: `${username}@example.com`,
          sn: "Owner", givenName: "Resource", cn: "Resource Owner",
        },
      });
      expect(created.ok(), `Create second resource owner (HTTP ${created.status()})`).toBe(true);

      const tokenResponse = await otherRequest.post(`${OPENAM_BASE}/oauth2/access_token`, {
        headers: clientHeaders,
        form: { grant_type: "password", username, password, scope: "uma_protection" },
      });
      expect(tokenResponse.status(), "Obtain second resource owner PAT").toBe(200);
      tokens = await tokenResponse.json();
      const otherPat = tokens?.access_token;
      expect(typeof otherPat === "string" && otherPat.trim().length > 0,
        "Second resource owner's PAT must be a nonempty string").toBe(true);
      expect(otherPat !== pat, "Second resource owner must receive a distinct PAT").toBe(true);

      const { id } = await create("first owner's resource set");
      const before = await (await read(id)).json();
      const otherHeaders = { Authorization: `Bearer ${otherPat}` };
      const list = await otherRequest.get(`${RESOURCE_SET}/?_queryId=*`, { headers: otherHeaders });
      expect(list.status(), "List as second resource owner").toBe(200);
      expect(await list.json(), "Second owner's resource set list must be empty").toEqual([]);

      const writeHeaders = { ...otherHeaders, "If-Match": "*" };
      const update = { name: runPrefix + "second owner's update", scopes: ["read"] };
      for (const [method, options] of [
        ["get", { headers: otherHeaders }],
        ["put", { headers: writeHeaders, data: update }],
        ["delete", { headers: writeHeaders }],
      ]) {
        const response = await otherRequest[method](itemUrl(id), options);
        const scenario = `${method.toUpperCase()} as second resource owner`;
        expect(response.status(), scenario).toBe(404);
        const body = await response.json();
        expect(body.error, scenario).toBe("not_found");
        expect(body.error_description, scenario).toBe(`Resource set does not exist with id ${id}`);
      }
      expect(await (await read(id)).json()).toEqual(before);
    } finally {
      try {
        // Revoking a refresh token also revokes its associated access token.
        const token = tokens?.refresh_token || tokens?.access_token;
        if (tokens?.access_token !== pat && typeof token === "string" && token) {
          const revoked = await otherRequest.post(`${OPENAM_BASE}/oauth2/token/revoke`, {
            headers: clientHeaders, form: { token },
          });
          expect(revoked.status(), "Revoke second resource owner grant").toBe(200);
        }
      } finally {
        try {
          // The server may create the user even if the request loses its response.
          const deleted = await otherRequest.delete(`${usersUrl}/${encodeURIComponent(username)}`, { headers }).catch(() => {
            throw new Error("Delete second resource owner failed (HTTP status unavailable)");
          });
          expect(deleted.ok() || deleted.status() === 404,
            `Delete second resource owner (HTTP ${deleted.status()})`).toBe(true);
        } finally {
          await otherRequest.dispose();
        }
      }
    }
  });

  test("creates a new resource set when posted to an item URL", async () => {
    const { id, name } = await create("original item for POST");
    const newName = "created at item URL";
    const response = await conditionalPost(itemUrl(id), newName);
    expect(response.status()).toBe(201);
    const { _id: createdId } = await response.json();
    expect(createdId).not.toBe(id);
    expect((await (await read(id)).json()).name).toBe(name);
    expect((await (await read(createdId)).json()).name).toBe(runPrefix + newName);
  });

  test("routes an extra resource set path segment to the realm error", async () => {
    const invalidBearer = { Authorization: "Bearer not-a-real-token" };

    // The realm router handles the extra segment before the protection filter can authenticate the request.
    for (const [scenario, headers] of [
      ["valid PAT", bearer()],
      ["invalid PAT", invalidBearer],
      ["missing PAT", {}],
    ]) {
      const response = await request.get(`${RESOURCE_SET}/a/b`, { headers });
      expect(response.status(), scenario).toBe(404);
      const body = await response.json();
      expect(body.message, scenario).toBe("No mapping organization found for organization identifier: /resource_set");
      expect(body.error, scenario).toBeUndefined();
    }

    const guarded = await request.get(`${RESOURCE_SET}/`, { headers: invalidBearer });
    expect(guarded.status()).toBe(401);
    expect((await guarded.json()).error).toBe("invalid_token");
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
