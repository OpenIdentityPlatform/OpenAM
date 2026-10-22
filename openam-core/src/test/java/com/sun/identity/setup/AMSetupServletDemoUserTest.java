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
package com.sun.identity.setup;

import static org.assertj.core.api.Assertions.assertThat;

import org.testng.annotations.Test;

/**
 * The advisory rests on the {@code demo} user being created inactive on a new installation. The e2e
 * spec runs against a CI image that configures an external user store, so {@code createDemoUser()}
 * is never invoked there and the workflow then creates its own active {@code demo}. Without a unit
 * test, reverting {@code inetuserstatus} to {@code Active} would leave every test green.
 */
public class AMSetupServletDemoUserTest {

    @Test
    public void shouldCreateTheDemoUserInactive() {
        assertThat(AMSetupServlet.demoUserAttributes())
                .containsEntry("inetuserstatus", java.util.Collections.singleton("Inactive"));
    }

    @Test
    public void shouldGiveTheDemoUserItsInstallationPassword() {
        assertThat(AMSetupServlet.demoUserAttributes())
                .containsEntry("userpassword", java.util.Collections.singleton("changeit"));
    }
}
