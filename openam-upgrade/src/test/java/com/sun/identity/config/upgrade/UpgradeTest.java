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
 * information: "Portions Copyrighted [year] [name of copyright owner]".
 *
 * Copyright 2026 3A Systems LLC.
 */
package com.sun.identity.config.upgrade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.openidentityplatform.openam.config.servlet.ConfiguratorContext;
import org.testng.annotations.Test;

/**
 * {@code Upgrade}'s constructor calls {@code AdminTokenAction.getInstance()} /
 * {@code UpgradeServices.getInstance()}, both of which need a fully bootstrapped OpenAM
 * environment (real SSOToken infrastructure, SMS-backed config) to succeed - unavailable in a bare
 * unit-test JVM, same category of environment-coupling gap as
 * {@code OptionsTest}/{@code Step2Test} document elsewhere in this migration. The constructor
 * catches this broadly and sets {@code error = true}, exactly like the old Click page did, so that
 * branch - and the real, pre-existing (not introduced by this port) NPE that follows from it if
 * {@code doUpgrade()}/{@code saveReport()} are ever invoked anyway - is exactly what's testable
 * here without a real environment.
 */
public class UpgradeTest {

    @Test
    public void onGetAddsErrorToModelWhenUpgradeSubsystemFailedToInitialize() {
        // No real OpenAM bootstrap in this test JVM, so the constructor's try/catch always takes
        // its failure branch here - see the class Javadoc.
        Upgrade page = new Upgrade();

        page.onGet();

        assertThat(page.getModel()).containsEntry("error", true);
        assertThat(page.getModel()).doesNotContainKeys("currentVersion", "newVersion", "changelist");
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void doUpgradeThrowsIfSubsystemFailedToInitialize() {
        // Pre-existing behavior, ported byte-for-byte: doUpgrade() calls upgrade.upgrade(...)
        // unconditionally, with no "error" guard - identical to the old Click Upgrade.java. Not
        // reachable through the real UI in this state, since upgrade.htm/upgrade.ftl never renders
        // the "doUpgrade" button when $error/${error??} is true - but a direct ?actionLink=doUpgrade
        // request would still hit this. Locked in with a dedicated test, same category as
        // Step7Test.onInitThrowsIfExtDataStoreNeverSet, so a future refactor doesn't silently "fix"
        // this without it being a deliberate, reviewed decision.
        Upgrade page = new Upgrade();
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        page.setContext(new ConfiguratorContext(request, response));

        page.doUpgrade();
    }
}
