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
package org.forgerock.openam.oauth2.openid;

import com.sun.identity.idm.AMIdentity;
import org.forgerock.openam.oauth2.utils.OAuth2Utils;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.testng.PowerMockTestCase;
import org.restlet.Request;
import org.restlet.ext.json.JsonRepresentation;
import org.restlet.representation.Representation;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.powermock.api.mockito.PowerMockito.when;

@PrepareForTest({AMIdentity.class, OAuth2Utils.class})
public class OpenIDConnectDiscoveryTest extends PowerMockTestCase {

    @Test
    public void testGetServerUsingEmailAddress() {
        AMIdentity id = PowerMockito.mock(AMIdentity.class);
        PowerMockito.mockStatic(OAuth2Utils.class);
        //resource=demo@example.com&rel=http://openid.net/specs/connect/1.0/issuer
        when(OAuth2Utils.getRequestParameter(any(Request.class), eq("resource"), any(Class.class)))
                .thenReturn("acct:demo@example.com");
        when(OAuth2Utils.getRequestParameter(any(Request.class), eq("rel"), any(Class.class)))
                .thenReturn("http://openid.net/specs/connect/1.0/issuer");

        when(OAuth2Utils.getIdentity(any(String.class), any(String.class))).thenReturn(id);
        when(OAuth2Utils.getRealm(any(Request.class))).thenReturn("/");
        when(OAuth2Utils.getDeploymentURL(any(Request.class)))
                .thenReturn("http://example.com/openam");

        OpenIDConnectDiscovery test = new OpenIDConnectDiscovery();
        Representation r = test.discovery();

        //expected result
        Map<String, Object> response = new HashMap<String, Object>();
        response.put("subject", "acct:demo@example.com");
        Set<Object> set = new HashSet<Object>();
        Map<String, Object> objectMap = new HashMap<String, Object>();
        objectMap.put("rel", "http://openid.net/specs/connect/1.0/issuer");
        objectMap.put("href", "http://example.com/openam");
        set.add(objectMap);
        response.put("links",set);

        JsonRepresentation expected = new JsonRepresentation(response);

        assert(r.equals(expected));

    }

    @Test
    public void testGetServerUsingURL() {
        AMIdentity id = PowerMockito.mock(AMIdentity.class);
        PowerMockito.mockStatic(OAuth2Utils.class);
        //resource=http://example.com/demo&rel=http://openid.net/specs/connect/1.0/issuer
        when(OAuth2Utils.getRequestParameter(any(Request.class), eq("resource"), any(Class.class)))
                .thenReturn("http://example.com/demo");
        when(OAuth2Utils.getRequestParameter(any(Request.class), eq("rel"), any(Class.class)))
                .thenReturn("http://openid.net/specs/connect/1.0/issuer");

        when(OAuth2Utils.getIdentity(any(String.class), any(String.class))).thenReturn(id);
        when(OAuth2Utils.getRealm(any(Request.class))).thenReturn("/");
        when(OAuth2Utils.getDeploymentURL(any(Request.class)))
                .thenReturn("http://example.com/openam");

        OpenIDConnectDiscovery test = new OpenIDConnectDiscovery();
        Representation r = test.discovery();

        //expected result
        Map<String, Object> response = new HashMap<String, Object>();
        response.put("subject", "http://example.com/demo");
        Set<Object> set = new HashSet<Object>();
        Map<String, Object> objectMap = new HashMap<String, Object>();
        objectMap.put("rel", "http://openid.net/specs/connect/1.0/issuer");
        objectMap.put("href", "http://example.com/openam");
        set.add(objectMap);
        response.put("links",set);

        JsonRepresentation expected = new JsonRepresentation(response);

        assert(r.equals(expected));

    }
}
