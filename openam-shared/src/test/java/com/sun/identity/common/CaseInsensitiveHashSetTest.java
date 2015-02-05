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
 * Copyright 2015 ForgeRock AS.
 */

package com.sun.identity.common;

import java.util.Set;

import org.forgerock.openam.utils.CollectionUtils;
import org.testng.annotations.Test;
import static org.fest.assertions.Assertions.*;

public class CaseInsensitiveHashSetTest {

    @Test
    public void shouldRemoveTwoEntriesUsingHashSet() {

        // Test removing elements using a standard HashSet
        Set<String> setToRemove = CollectionUtils.asSet("inetuser", "iplanetpreferences", "three", "four", "five");
        Set<String> setToInitialise = CollectionUtils.asSet("inetUser", "sunfederationmanagerdatastore",
                "forgerock-am-dashboard-service", "iplanetpreferences");

        Set<Object> ciHashSet = new CaseInsensitiveHashSet();

        ciHashSet.addAll(setToInitialise);
        ciHashSet.removeAll(setToRemove);

        assertThat(ciHashSet).hasSize(2).contains("sunfederationmanagerdatastore", "forgerock-am-dashboard-service");
    }

    @Test
    public void shouldRemoveTwoEntriesUsingciHashSet() {

        // Test removing elements using a CaseInsensitiveHashSet
        Set<String> ciSetToRemove = CollectionUtils.asCaseInsensitiveHashSet("inetuser", "iplanetpreferences", "three",
                "four", "five");
        Set<String> setToInitialise = CollectionUtils.asSet("inetUser", "sunfederationmanagerdatastore",
                "forgerock-am-dashboard-service", "iplanetpreferences");
        Set<Object> ciHashSet = new CaseInsensitiveHashSet();

        ciHashSet.addAll(setToInitialise);
        ciHashSet.removeAll(ciSetToRemove);

        assertThat(ciHashSet).hasSize(2).contains("sunfederationmanagerdatastore", "forgerock-am-dashboard-service");

    }

}
