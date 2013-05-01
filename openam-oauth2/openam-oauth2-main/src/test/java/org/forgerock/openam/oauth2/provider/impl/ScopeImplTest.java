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
import java.util.Map;
import java.util.Set;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.*;
import static org.powermock.api.support.membermodification.MemberMatcher.constructor;
import static org.powermock.api.support.membermodification.MemberModifier.suppress;

import com.sun.identity.idm.AMIdentity;
import org.forgerock.openam.ext.cts.repo.DefaultOAuthTokenStoreImpl;
import org.forgerock.openam.oauth2.model.CoreToken;
import org.forgerock.openam.oauth2.provider.OAuth2TokenStore;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.testng.PowerMockTestCase;
import org.testng.IObjectFactory;
import org.testng.annotations.ObjectFactory;
import org.testng.annotations.Test;

@PrepareForTest(AMIdentity.class)
public class ScopeImplTest extends PowerMockTestCase {

    ScopeImpl scopeImpl = null;

    /**
     * Test getting the default scope
     */
    @Test
    public void testGetDefaultScopeForScopeToPresentOnAuthorizationPage() {
        suppress(constructor(DefaultOAuthTokenStoreImpl.class));
        OAuth2TokenStore store = mock(DefaultOAuthTokenStoreImpl.class);
        AMIdentity id = PowerMockito.mock(AMIdentity.class);
        scopeImpl = new ScopeImpl(store, id);

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

        suppress(constructor(DefaultOAuthTokenStoreImpl.class));
        OAuth2TokenStore store = mock(DefaultOAuthTokenStoreImpl.class);
        AMIdentity id = PowerMockito.mock(AMIdentity.class);
        scopeImpl = new ScopeImpl(store, id);

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
        suppress(constructor(DefaultOAuthTokenStoreImpl.class));
        OAuth2TokenStore store = mock(DefaultOAuthTokenStoreImpl.class);
        AMIdentity id = PowerMockito.mock(AMIdentity.class);
        scopeImpl = new ScopeImpl(store, id);

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

    /**
     * Tests getting user info for the user info endpoint
     */
    @Test
    public void testGetUserInfo(){

        //mock objects
        suppress(constructor(DefaultOAuthTokenStoreImpl.class));
        OAuth2TokenStore store = mock(DefaultOAuthTokenStoreImpl.class);
        AMIdentity id = PowerMockito.mock(AMIdentity.class);
        CoreToken token = mock(CoreToken.class);
        scopeImpl = new ScopeImpl(store, id);

        //setup AMIdentity attribute return values
        Set idAttribute = new HashSet<String>();
        idAttribute.add("attributeValue");

        //setup token scope values
        Set scopeValue = new HashSet<String>();
        scopeValue.add("email");

        try {
            when(id.getAttribute(anyString())).thenReturn(idAttribute);
        } catch (Exception e){
            //do nothing
        }
        when(token.getUserID()).thenReturn("user");
        when(token.getScope()).thenReturn(scopeValue);
        when(token.getRealm()).thenReturn("/");

        Map<String, Object> returnValues = scopeImpl.getUserInfo(token);

        assert(returnValues.containsKey("sub") && returnValues.get("sub").equals("user"));
        assert(returnValues.containsKey("email") && returnValues.get("email").equals("attributeValue"));
    }

    @ObjectFactory
    public IObjectFactory getObjectFactory() {
        return new org.powermock.modules.testng.PowerMockObjectFactory();
    }
}
