/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009 Sun Microsystems Inc. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * https://opensso.dev.java.net/public/CDDLv1.0.html or
 * opensso/legal/CDDLv1.0.txt
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at opensso/legal/CDDLv1.0.txt.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * $Id: RestNoSubjectHeaderTest.java,v 1.2 2009/11/25 18:09:51 veiming Exp $
 */

package com.sun.identity.rest;

import com.iplanet.am.util.SystemProperties;
import com.iplanet.sso.SSOToken;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.encode.Hash;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import java.net.URLEncoder;
import java.security.AccessController;
import javax.ws.rs.core.Cookie;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 *
 * @author dennis
 */
public class RestNoSubjectHeaderTest {
    private static final SSOToken adminToken = (SSOToken)
        AccessController.doPrivileged(AdminTokenAction.getInstance());
    private WebResource webClient;
    private String hashedTokenId;
    private Cookie cookie;

    @BeforeClass
    public void setup() throws Exception {
        String tokenId = adminToken.getTokenID().toString();
        hashedTokenId = Hash.hash(tokenId);
        String cookieValue = tokenId;

        if (Boolean.parseBoolean(
            SystemProperties.get(Constants.AM_COOKIE_ENCODE, "false"))) {
            cookieValue = URLEncoder.encode(tokenId, "UTF-8");
        }
        cookie = new Cookie(SystemProperties.get(Constants.AM_COOKIE_NAME),
            cookieValue);
        webClient = Client.create().resource(
            SystemProperties.getServerInstanceName() +
            "/ws/1/entitlement/privilege");
    }

    @Test
    public void search() throws Exception {
        String result = webClient
            .path("/")
            .queryParam("subject", hashedTokenId)
            .cookie(cookie)
            .get(String.class);
        // no subject header is set, this should not throw any exception
    }

}
