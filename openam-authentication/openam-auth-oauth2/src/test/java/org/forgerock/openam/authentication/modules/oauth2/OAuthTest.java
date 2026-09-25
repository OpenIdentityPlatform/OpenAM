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
package org.forgerock.openam.authentication.modules.oauth2;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertTrue;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.testng.annotations.Test;

public class OAuthTest {

    @Test
    public void shouldRemoveCredentialAndAccountStatusAttributesFromUpdates() {
        Map<String, Set<String>> attributes = new HashMap<>();
        attributes.put("userPassword", Collections.singleton("password"));
        attributes.put("UserPassword", Collections.singleton("password"));
        attributes.put("userpassword", Collections.singleton("password"));
        attributes.put("inetuserstatus", Collections.singleton("Active"));
        attributes.put("inetUserStatus", Collections.singleton("Active"));
        attributes.put("INETUSERSTATUS", Collections.singleton("Active"));
        attributes.put("cn", Collections.singleton("Alice User"));

        OAuth.removeRestrictedAccountUpdateAttributes(attributes);

        assertEquals(attributes.keySet(), Collections.singleton("cn"));
        assertFalse(attributes.containsKey("userPassword"));
        assertFalse(attributes.containsKey("inetuserstatus"));
    }

    @Test
    public void csrfStateTokenIdIsThirtyTwoAlphanumericCharacters() {
        String id = OAuth.newCsrfStateTokenId();

        assertTrue(id.matches("[A-Za-z0-9]{32}"), id);
        assertNotEquals(id, OAuth.newCsrfStateTokenId());
    }
}

