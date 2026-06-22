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
import { resolve } from "path";
import { fileURLToPath } from "url";

// ─── Configuration ────────────────────────────────────────────────────────────
const BASE_URL = process.env.OPENAM_BASE_URL ?? "http://openam.example.org:8080/openam";
const ADMIN_USER = process.env.OPENAM_ADMIN_USER ?? "amadmin";
const ADMIN_PASS = process.env.OPENAM_ADMIN_PASS ?? "ampassword";

const STS_INSTANCE_NAME = "openam-to-saml-sts";
const SP_ENTITY_ID = "https://sp.example.com";
const SP_ACS_URL = "https://sp.example.com/acs";

const __filename = fileURLToPath(import.meta.url);
const __dirname = resolve(__filename, "..");

// Get admin token
async function getAuthToken(request, username, password) {
  const resp = await request.post(`${BASE_URL}/json/authenticate`, {
    headers: { 
      "Content-Type": "application/json",
      "X-OpenAM-Username": username,
      "X-OpenAM-Password": password,
      "Content-Type": "application/json",
      "Accept-API-Version": "resource=2.0, protocol=1.0",
    }
  });
  const json = await resp.json();
  return json.tokenId;
}

async function setupSts(request) {
  
}

// ─── Tests ────────────────────────────────────────────────────────────────────
test.describe("REST STS - OpenAM Token → SAML2", () => {

  let adminToken;

  
  async function stsExists(request) {
    const response = await request.get(`${BASE_URL}/sts-publish/rest`, {
      headers: {
        "Content-Type": "application/json",
        "iPlanetDirectoryPro": adminToken
      },
    });

    if(!response.ok()) {
      return false;
    }
    const json = await response.json();
    return !!json[STS_INSTANCE_NAME]
  }

  async function setupSts(request) {
     const payload = {
      invocation_context: "invocation_context_client_sdk",
      instance_state: {
        "deployment-config": {
          "deployment-url-element": STS_INSTANCE_NAME,
          "deployment-realm": "/",
          "deployment-auth-target-mappings": {}
        },
        "saml2-config": {
          "issuer-name": `${BASE_URL}`,
          "saml2-sp-entity-id": SP_ENTITY_ID,
          "saml2-sp-acs-url": SP_ACS_URL,
          "saml2-signature-key-alias": "test",  
          "saml2-sign-assertion": "false",
          "saml2-encrypt-assertion": "false",
          "saml2-encrypt-attributes": "false",
          "saml2-encrypt-nameid": "false",
          "saml2-name-id-format": "urn:oasis:names:tc:SAML:2.0:nameid-format:persistent",
          "saml2-token-lifetime-seconds": "600",
          "saml2-encryption-algorithm-strength": "128",
          "saml2-attribute-map": {
            "email": "mail"
          }
        },
        "persist-issued-tokens-in-cts": "false",
        "supported-token-transforms": [
          {
            "inputTokenType": "OPENAM",
            "outputTokenType": "SAML2",
            "invalidateInterimOpenAMSession": false
          }
        ],
        "token-lifetime": 600
      }
    };

    const response = await request.post(`${BASE_URL}/sts-publish/rest?_action=create`, {
      headers: {
        "Content-Type": "application/json",
        "iPlanetDirectoryPro": adminToken
      },
      data: payload
    });

    expect(response.ok()).toBeTruthy();
    const body = await response.json();
    console.log("REST STS instance created:", body);
    expect(body).toHaveProperty("_id");
  }

  test.beforeAll(async ({ request }) => {
    adminToken = await getAuthToken(request, ADMIN_USER, ADMIN_PASS);
    expect(adminToken).toBeTruthy();
    console.log(`Admin token obtained: ${adminToken.slice(0, 20)}...`);
    const haveSts = await stsExists(request)
    if(!haveSts) {
        await setupSts(request);
    }
  });


  test("should translate OpenAM token to SAML2 assertion", async ({ request }) => {
    
    const userSession = await getAuthToken(request, "demo", "changeit");

    const translatePayload = {
      input_token_state: {
        token_type: "OPENAM",
        session_id: userSession
      },
      output_token_state: {
        token_type: "SAML2",
        subject_confirmation: "BEARER"
      }
    };

    const response = await request.post(
      `${BASE_URL}/rest-sts/${STS_INSTANCE_NAME}?_action=translate`,
      {
        headers: {
          "Content-Type": "application/json",
          "iPlanetDirectoryPro": userSession
        },
        data: translatePayload
      }
    );

    expect(response.ok()).toBeTruthy();
    const result = await response.json();

    expect(result).toHaveProperty("issued_token");
    const assertion = result.issued_token;

    console.log(`SAML Assertion received: ${assertion.substring(0, 300)}...`,);

    // Basic XML validation
    expect(assertion).toContain("<saml:Assertion");
    expect(assertion).toContain(`<saml:Issuer>${BASE_URL}`);
    expect(assertion).toContain(SP_ENTITY_ID);
  });

});