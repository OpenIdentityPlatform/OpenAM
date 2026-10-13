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

/**
 * OpenAM WindowsDesktopSSO (Kerberos / SPNEGO) e2e test, without Windows.
 *
 * bootstrap.sh starts a KDC in a container (provider selected by KRB_PROVIDER, MIT by default),
 * installs the service keytab into OpenAM, configures the WindowsDesktopSSO module and a chain
 * "module SUFFICIENT -> DataStore REQUIRED", and obtains a TGT for the test user into .work/krb5cc.
 * The browser and curl use that ticket through the system GSSAPI library (libgssapi_krb5),
 * pointed at .work/krb5.conf and .work/krb5cc by KRB5_CONFIG and KRB5CCNAME.
 *
 * Requires MIT Kerberos client tools (Debian/Ubuntu package krb5-user); skipped without them.
 * Runs before HttpOnly mode is enabled: once #1170 is fixed, the curl case reads tokenId from the response body.
 *
 * /UI/Login is not covered: with the XUI enabled (the default) XUIFilter redirects it to the XUI.
 */

import { test, expect, chromium } from "@playwright/test";
import { execFileSync } from "child_process";
import { readFileSync } from "fs";
import { createServer } from "http";
import { resolve } from "path";
import { fileURLToPath } from "url";
import { OPENAM_BASE, USERNAME, PASSWORD, submitXuiLogin, idFromSession } from "../common/openam-commons.mjs";

const __dirname = fileURLToPath(new URL(".", import.meta.url));

const BOOTSTRAP_SCRIPT = resolve(__dirname, "bootstrap.sh");
const WORK_DIR = resolve(__dirname, ".work");
const OPENAM_HOST = new URL(OPENAM_BASE).hostname;

// Absolute paths: the browser process does not run in this directory.
const KRB5_CONFIG = resolve(WORK_DIR, "krb5.conf");
const KRB5CCNAME = `FILE:${resolve(WORK_DIR, "krb5cc")}`;
const KRB_ENV = { ...process.env, KRB5_CONFIG, KRB5CCNAME };

const BOOTSTRAP_ENV = {
  ...process.env,
  KRB_SPN_HOST: process.env.KRB_SPN_HOST ?? OPENAM_HOST,
  KRB_USER: process.env.KRB_USER ?? USERNAME,
};

// /json/authenticate answers 400 when the Negotiate header arrives with the request that starts the
// login: the module's script TextOutputCallback cannot be filled from the request, so
// RestAuthCallbackHandlerManager converts every callback to JSON and
// RestAuthHttpCallbackHandler.convertToJson rejects the HttpCallback. The browser resends the XUI
// request with the header after the 401 challenge, so the XUI hits the same error.
// The curl and XUI cases assert that 400. Once the endpoint is fixed they turn red: assert the session instead.
const REST_NEGOTIATE_BUG = "/json/authenticate rejects a Negotiate header with 400: "
  + "https://github.com/OpenIdentityPlatform/OpenAM/issues/1170";
// A request without the Authorization header gets the module's 401 first, so only one with the header
// reaches this message.
const HTTP_CALLBACK_REJECTED = "HttpCallback Authorization Header must be specified in the initial request";

// The browser may answer Negotiate challenges from the OpenAM host only.
test.use({
  launchOptions: {
    args: [`--auth-server-allowlist=${OPENAM_HOST}`],
    env: KRB_ENV,
  },
});

/** Values resolved by bootstrap.sh: provider, realm, user, module, chain. */
let krb;
let kdcStarted = false;

function hasMitKerberosClient() {
  try {
    return execFileSync("klist", ["-V"], { encoding: "utf-8", stdio: ["ignore", "pipe", "ignore"] })
      .startsWith("Kerberos 5 version");
  } catch {
    return false;
  }
}

function runBootstrap(args = [], options = {}) {
  // No shell: the script path is handed to bash as an argument, not interpolated.
  return execFileSync("bash", [BOOTSTRAP_SCRIPT, ...args], { env: BOOTSTRAP_ENV, ...options });
}

function authenticateUrl(indexValue, indexType = "module") {
  return `${OPENAM_BASE}/json/authenticate?authIndexType=${indexType}&authIndexValue=${encodeURIComponent(indexValue)}`;
}

/**
 * Let the browser answer a Negotiate challenge from a local server on the OpenAM host name and
 * return the Authorization header it sent. The port is not part of the SPN, so the token is for the
 * OpenAM service principal. Skips the test when the OpenAM host name is not an address of this machine.
 */
async function negotiateHeaderFromBrowser(page) {
  let authorization;
  const server = createServer((req, res) => {
    if (req.headers.authorization?.startsWith("Negotiate ")) {
      authorization = req.headers.authorization;
      res.end("ok");
    } else {
      res.writeHead(401, { "WWW-Authenticate": "Negotiate" });
      res.end();
    }
  });
  // On the address the OpenAM host name resolves to, which is where the browser connects.
  try {
    await new Promise((listening, failed) => server.once("error", failed).listen(0, OPENAM_HOST, listening));
  } catch (err) {
    test.skip(err.code === "EADDRNOTAVAIL", `${OPENAM_HOST} resolves to ${err.address}, not to this machine: `
      + "the browser cannot reach a local server on that name");
    throw err;
  }
  try {
    await page.goto(`http://${OPENAM_HOST}:${server.address().port}/`);
  } finally {
    server.closeAllConnections();
    server.close();
  }
  return authorization;
}

test.beforeAll(() => {
  // docker build, KDC start, several ssoadm runs and kinit do not fit the default 180 s.
  test.setTimeout(600_000);
  test.skip(!hasMitKerberosClient(), "MIT Kerberos client tools (kinit, klist) are not installed");

  console.log(`\n▶ Running bootstrap script: ${BOOTSTRAP_SCRIPT}`);
  kdcStarted = true;
  try {
    runBootstrap([], { stdio: "inherit", timeout: 540_000 });
  } catch (err) {
    // A failed beforeAll skips afterEach, and afterAll removes the KDC container: print its logs now.
    try {
      console.log(runBootstrap(["logs"], { encoding: "utf-8", timeout: 60_000 }));
    } catch (logsErr) {
      console.log(`bootstrap.sh logs failed: ${logsErr.message}`);
    }
    throw new Error(`${BOOTSTRAP_SCRIPT} exited with code ${err.status}: ${err.message}`);
  }
  krb = JSON.parse(readFileSync(resolve(WORK_DIR, "kerberos.json"), "utf-8"));
});

test.afterAll(() => {
  // The KDC must not outlive the spec: CI removes the Docker network it is attached to.
  if (kdcStarted) {
    runBootstrap(["stop"], { stdio: "inherit", timeout: 60_000 });
  }
});

test.afterEach(async ({}, testInfo) => {
  if (testInfo.status === testInfo.expectedStatus) {
    return;
  }
  let logs;
  try {
    logs = runBootstrap(["logs"], { encoding: "utf-8", timeout: 60_000 });
  } catch (err) {
    logs = `bootstrap.sh logs failed: ${err.message}`;
  }
  await testInfo.attach("kerberos-logs", { body: logs, contentType: "text/plain" });
});

test.describe("OpenAM WindowsDesktopSSO - Kerberos SSO", () => {
  test("REST: the browser's Negotiate token completes a login started without it", async ({ page }) => {
    // The module reads the Negotiate header of whichever request continues the login, so a client
    // that first obtains authId and then sends the header together with it is not affected by the
    // one-step bug. This proves the KDC, the keytab, the module settings and the browser's Kerberos.
    const authorization = await negotiateHeaderFromBrowser(page);
    expect(authorization, "the browser should answer the Negotiate challenge").toBeTruthy();

    const headers = { "Accept-API-Version": "resource=2.0, protocol=1.0" };
    const start = await page.request.post(authenticateUrl(krb.module), { headers, data: {} });
    expect(start.status(), "the login should ask for Negotiate").toBe(401);
    const { authId } = await start.json();
    expect(authId, "the challenge should carry authId").toBeTruthy();

    const finish = await page.request.post(authenticateUrl(krb.module), {
      headers: { ...headers, Authorization: authorization },
      data: { authId },
    });
    expect(finish.status(), await finish.text()).toBe(200);
    // tokenId is absent in HttpOnly mode; the session cookie set by the response is used then.
    const { tokenId } = await finish.json();
    expect(String(await idFromSession(page.request, tokenId)).toLowerCase()).toBe(krb.user.toLowerCase());
  });

  test("REST: curl --negotiate is rejected with 400 until #1170 is fixed", async () => {
    const output = execFileSync("curl", [
      "-sS", "--negotiate", "-u", ":",
      "-H", "Content-Type: application/json",
      "-H", "Accept-API-Version: resource=2.0, protocol=1.0",
      "-d", "{}",
      "-w", "\n%{http_code}",
      authenticateUrl(krb.module),
    ], { encoding: "utf-8", env: KRB_ENV });
    const separator = output.lastIndexOf("\n");
    const body = output.slice(0, separator);
    const status = output.slice(separator + 1);
    console.log(`/json/authenticate answered ${status}: ${body}`);

    // #1170: when this turns red, assert 200, tokenId and the session from idFromSession instead.
    expect(status, REST_NEGOTIATE_BUG).toBe("400");
    expect(body, "the 400 should come from curl's Negotiate header").toContain(HTTP_CALLBACK_REJECTED);
  });

  test("XUI: the Negotiate login is rejected with 400 until #1170 is fixed", async ({ page }) => {
    page.on("response", (resp) => {
      if (new URL(resp.url()).pathname.endsWith("/authenticate")) {
        console.log(`XUI ${resp.request().method()} ${resp.url()} answered ${resp.status()}`);
      }
    });
    // The browser answers the 401 challenge in its network stack: the page sees only the resent request.
    const login = page.waitForResponse((resp) => resp.request().method() === "POST"
      && new URL(resp.url()).pathname.endsWith("/authenticate"));
    await page.goto(`${OPENAM_BASE}/XUI/#login/&authIndexType=module`
      + `&authIndexValue=${encodeURIComponent(krb.module)}`);
    const resp = await login;
    // #1170: when this turns red, poll idFromSession for the user and assert that no login form is shown instead.
    expect(resp.status(), REST_NEGOTIATE_BUG).toBe(400);
    expect(await resp.text(), "the 400 should come from the browser's Negotiate header")
      .toContain(HTTP_CALLBACK_REJECTED);
  });

  test("fallback: without a ticket the chain continues to the DataStore form", async ({ request }) => {
    // The chain must start with the Kerberos module's challenge, or the DataStore form below proves nothing.
    const start = await request.post(authenticateUrl(krb.chain, "service"), {
      headers: { "Accept-API-Version": "resource=2.0, protocol=1.0" },
      data: {},
    });
    expect(start.status(), "the chain should start with the Negotiate challenge").toBe(401);
    expect(start.headers()["www-authenticate"]).toBe("Negotiate");

    // No allowlist and no credential cache: the browser cannot answer the Negotiate challenge,
    // the XUI reports http-auth-failed, the Kerberos module is ignored and DataStore asks for a password.
    const noCache = `FILE:${resolve(WORK_DIR, "no-such-krb5cc")}`;
    const browser = await chromium.launch({ env: { ...KRB_ENV, KRB5CCNAME: noCache } });
    try {
      const page = await browser.newPage();
      await page.goto(`${OPENAM_BASE}/XUI/#login/&service=${encodeURIComponent(krb.chain)}`);
      await submitXuiLogin(page, USERNAME, PASSWORD);
      await expect.poll(async () => String(await idFromSession(page.request)).toLowerCase(), { timeout: 30_000 })
        .toBe(USERNAME.toLowerCase());
    } finally {
      await browser.close();
    }
  });
});
