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

import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.forgerock.json.fluent.JsonValue;
import com.sun.identity.shared.OAuth2Constants;
import org.forgerock.openam.oauth2.model.ClientApplication;

/**
 * A NAME does ...
 * 
 * @author Laszlo Hordos
 */
public class ClientApplicationIdentity extends JsonValue implements ClientApplication {

    /**
     * Default Constructor.
     */
    public ClientApplicationIdentity() {
        super(new HashMap());
    }

    @Override
    public String getClientId() {
        return get("id").required().asString();
    }

    @Override
    public ClientType getClientType() {
        return get("clientType").required().asEnum(ClientType.class);
    }

    @Override
    public Set<URI> getRedirectionURIs() {
        Set<URI> result = new HashSet<URI>();
        for (JsonValue v : get("redirectionURIs")) {
            if (v.isString()) {
                result.add(v.asURI());
            }
        }
        return Collections.unmodifiableSet(result);
    }

    @Override
    public String getAccessTokenType() {
        return OAuth2Constants.Bearer.BEARER;
    }

    @Override
    public String getClientAuthenticationSchema() {
        return null;
    }

    @Override
    public Set<String> getAllowedGrantScopes() {
        return Collections.unmodifiableSet(new HashSet<String>(get("allowedGrantScopes").required()
                .asList(String.class)));
    }

    @Override
    public Set<String> getDefaultGrantScopes() {
        return Collections.unmodifiableSet(new HashSet<String>(get("defaultGrantScopes").required()
                .asList(String.class)));
    }

    @Override
    public boolean isAutoGrant() {
        return false;
    }

    @Override
    public Set<String> getDisplayName(){
        return Collections.unmodifiableSet(new HashSet<String>(get("displayName").required()
                .asList(String.class)));
    }

    @Override
    public Set<String> getDisplayDescription(){
        return Collections.unmodifiableSet(new HashSet<String>(get("displayDescription").required()
                .asList(String.class)));
    }
}
