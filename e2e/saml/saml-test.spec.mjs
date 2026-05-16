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


// openam.spec.mjs – ESM edition
import { test, expect } from "@playwright/test";
import { execSync } from "child_process";
import { resolve } from "path";
import { fileURLToPath } from "url";

/**
 * OpenAM XUI Login Test Suite
 *
 * Configuration (override via environment variables):
 *   OPENAM_USERNAME   – login username                 (default: demo)
 *   OPENAM_PASSWORD   – login password                 (default: changeit)
 *   BOOTSTRAP_SCRIPT  – path to the startup script     (default: ./bootstrap.sh)
 */

// ─── __dirname equivalent in ESM ──────────────────────────────────────────────
const __filename = fileURLToPath(import.meta.url);
const __dirname = fileURLToPath(new URL(".", import.meta.url));

// ─── Configuration ────────────────────────────────────────────────────────────
const USERNAME = process.env.OPENAM_USERNAME ?? "demo";
const PASSWORD = process.env.OPENAM_PASSWORD ?? "changeit";
const BOOTSTRAP_SCRIPT = process.env.BOOTSTRAP_SCRIPT ?? "./bootstrap.sh";

// Derived URLs
const LOGIN_URL = "http://sp.mycompany.org:8081/openam/spssoinit?metaAlias=/sp&idpEntityID=http%3A//openam.example.org%3A8080/openam&RelayState=http%3A//sp.mycompany.org%3A8081/openam";
const EXPECTED_IDP_URL_PATTERN = /openam\.example\.org/;
const EXPECTED_SP_URL_PATTERN = /sp\.mycompany\.org/;

// ─── Selectors (XUI / LESS-based OpenAM UI) ───────────────────────────────────
const SEL = {
  usernameInput: "#idToken1",                          // <input id="idToken1">
  passwordInput: "#idToken2",                          // <input id="idToken2">
  loginButton:   'input[type="submit"]',               // <input id="loginButton" type="submit">
  userIdElement: "#input-username",
};

const execScript = (scriptPath) => {
  try {
    execSync(`bash "${scriptPath}"`, {
      encoding: "utf-8",
      timeout: 300_000,   // 5 minutes max
      stdio: "inherit",
    });
  } catch (err) {
    throw new Error(
      `${scriptPath} exited with code ${err.status}: ${err.message}`
    );
  }
}

// ─── Bootstrap – run once before all tests ────────────────────────────────────
test.beforeAll(() => {
  const scriptPath = resolve(__dirname, BOOTSTRAP_SCRIPT);
  console.log(`\n▶ Running bootstrap script: ${scriptPath}`);
  execScript(scriptPath);

});

// ─── Tests ────────────────────────────────────────────────────────────────────
test.describe("OpenAM XUI - Login flow", () => {
  test("should log in as demo and reach the authenticated page", async ({ page }) => {
    // ── 1. Open the login page ──────────────────────────────────────────────
    console.log(`Navigating to: ${LOGIN_URL}`);
    await page.goto(LOGIN_URL);

    await page.waitForURL(EXPECTED_IDP_URL_PATTERN, {
      timeout: 20_000,
      waitUntil: "networkidle",
    });

    await expect(
      page.locator(SEL.usernameInput),
      "Username input should be visible"
    ).toBeVisible({ timeout: 15_000 });

    // ── 2. Enter credentials ────────────────────────────────────────────────
    await page.fill(SEL.usernameInput, USERNAME);
    await page.fill(SEL.passwordInput, PASSWORD);

    // ── 3. Click Login ──────────────────────────────────────────────────────
    await page.click(SEL.loginButton);

    // ── 4. Wait for the post-login redirect ─────────────────────────────────
    await page.waitForURL(EXPECTED_SP_URL_PATTERN, {
      timeout: 20_000,
      waitUntil: "networkidle",
    });

    // ── 5. Assert target URL ─────────────────────────────────────────────────
    const currentUrl = page.url();
    console.log(`Redirected to: ${currentUrl}`);
    expect(currentUrl, "URL should match the post-login pattern").toMatch(EXPECTED_SP_URL_PATTERN);

    // ── 6. Assert user-id element is present ────────────────────────────────
    await expect(
      page.locator(SEL.userIdElement).first(),
      "Authenticated user-id element should be visible after login"
    ).toBeVisible({ timeout: 10_000 });
  });
});
