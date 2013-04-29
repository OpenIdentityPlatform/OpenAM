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
 * Copyright 2013 ForgeRock Inc.
 */
package org.forgerock.openam.oauth2.provider.impl;

import java.util.HashSet;
import java.util.Set;
import static org.testng.Assert.*;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

public class ScopeImplTest {

    ScopeImpl scopeImpl = null;

    @BeforeTest
    public void setup() {
        this.scopeImpl = new ScopeImpl();
    }

    /**
     * Test getting the default scope
     */
    @Test
    public void testGetDefaultScopeForScopeToPresentOnAuthorizationPage() {
        Set<String> requestedScope = new HashSet<String>();
        Set<String> availableScope = new HashSet<String>();
        Set<String> defaultScope = new HashSet<String>();

        defaultScope.add("scope1");
        defaultScope.add("scope2");

        Set<String> returnedScope =
                scopeImpl.scopeToPresentOnAuthorizationPage(requestedScope, availableScope, defaultScope);

        assertEquals(returnedScope, defaultScope);
    }

    /**
     * Test requested scope that is a subset of the available scope
     */
    @Test
    public void testGetRequestedScopeForScopeToPresentOnAuthorizationPage() {
        Set<String> requestedScope = new HashSet<String>();
        Set<String> availableScope = new HashSet<String>();
        Set<String> defaultScope = new HashSet<String>();

        defaultScope.add("scope1");
        defaultScope.add("scope2");

        requestedScope.add("scope3");
        requestedScope.add("scope4");

        availableScope.add("scope3");
        availableScope.add("scope4");
        availableScope.add("scope5");


        Set<String> returnedScope =
                scopeImpl.scopeToPresentOnAuthorizationPage(requestedScope, availableScope, defaultScope);

        assertEquals(returnedScope, requestedScope);
    }

    /**
     * Test requested scope that is a subset of the available scope plus asks
     * for scope not available
     */
    @Test
    public void testGetRequestedScopeForScopeToPresentOnAuthorizationPageWithExtraScopeRequest() {
        Set<String> requestedScope = new HashSet<String>();
        Set<String> availableScope = new HashSet<String>();
        Set<String> defaultScope = new HashSet<String>();

        defaultScope.add("scope1");
        defaultScope.add("scope2");

        requestedScope.add("scope3");
        requestedScope.add("scope4");
        requestedScope.add("scope6");

        availableScope.add("scope3");
        availableScope.add("scope4");
        availableScope.add("scope5");

        Set<String> returnedScope =
                scopeImpl.scopeToPresentOnAuthorizationPage(requestedScope, availableScope, defaultScope);

        Set<String> expectedScope = new HashSet<String>(requestedScope);
        expectedScope.remove("scope6");

        assertEquals(returnedScope, expectedScope);
    }
}
