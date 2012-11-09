/*
 * DO NOT REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 ForgeRock Inc. All rights reserved.
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
 * "Portions Copyrighted [2012] [ForgeRock Inc]"
 */

package org.forgerock.openam.oauth2.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.forgerock.openam.oauth2.model.ClientApplication;
import org.forgerock.restlet.ext.oauth2.provider.OAuth2Client;
import org.forgerock.restlet.ext.openam.OpenAMParameters;

import com.iplanet.sso.SSOToken;
import com.sun.identity.authentication.AuthContext;

/**
 * A NAME does ...
 * 
 * @author Laszlo Hordos
 */
public class ClientIdentityVerifier extends AbstractIdentityVerifier<OAuth2Client> {

    private List<String> redirectURI;

    /**
     * Constructor.
     * <p/>
     * 
     * @param parameters
     *            OpenAM boot properties
     * @param redirects
     *            list of redirect URLs
     */
    public ClientIdentityVerifier(OpenAMParameters parameters, List<String> redirects) {
        super(parameters);
        redirectURI = new ArrayList<String>(redirects);
        redirectURI.add("http://local.identitas.no:9085/openam/oauth2test/code-token.html");
    }

    @Override
    protected OAuth2Client createUser(AuthContext authContext) throws Exception {
        ClientApplicationIdentity client = new ClientApplicationIdentity();
        SSOToken token = authContext.getSSOToken();
        client.put("id", token.getProperty("UserId"));
        client.put("clientType", ClientApplication.ClientType.CONFIDENTIAL.name());
        client.put("redirectionURIs", redirectURI);
        client.put("allowedGrantScopes", Arrays.asList("read", "write", "delete"));
        client.put("defaultGrantScopes", Arrays.asList("read"));
        return new OAuth2Client(client);
    }
}
