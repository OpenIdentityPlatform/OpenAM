/*
 * DO NOT REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 ForgeRock Inc. All rights reserved.
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions copyright [year] [name of copyright owner]"
 */

package org.forgerock.openam.provider.impl;

import com.sun.identity.idm.AMIdentity;
import org.forgerock.openam.ext.cts.repo.DefaultOAuthTokenStoreImpl;
import org.forgerock.openam.oauth2.model.CoreToken;
import org.forgerock.openam.oauth2.provider.OAuth2TokenStore;
import org.forgerock.openam.oauth2.provider.impl.ScopeImpl;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.testng.PowerMockTestCase;
import org.testng.IObjectFactory;
import org.testng.annotations.ObjectFactory;
import org.testng.annotations.Test;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.mockito.Mockito.*;

@PrepareForTest(AMIdentity.class)
public class ScopeImplTest extends PowerMockTestCase {

    private ScopeImpl scope = null;

    @Test
    public void test_GetUserInfo(){

        //mock objects
        OAuth2TokenStore store = mock(DefaultOAuthTokenStoreImpl.class);
        AMIdentity id = PowerMockito.mock(AMIdentity.class);
        CoreToken token = mock(CoreToken.class);
        scope = new ScopeImpl(store, id);

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

        Map<String, Object> returnValues = scope.getUserInfo(token);

        assert(returnValues.containsKey("sub") && returnValues.get("sub").equals("user"));
        assert(returnValues.containsKey("email") && returnValues.get("email").equals("attributeValue"));
    }

    @ObjectFactory
    public IObjectFactory getObjectFactory() {
        return new org.powermock.modules.testng.PowerMockObjectFactory();
    }

}
