/*
 * DO NOT REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2013 ForgeRock Inc. All rights reserved.
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

package org.forgerock.openam.oauth2.rest.mocks;

import com.sun.identity.coretoken.interfaces.OAuth2TokenRepository;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.JsonResourceException;

/**
 * Mock OAuth2TokenRepository
 */
public class mockOAuth2TokenRepository implements OAuth2TokenRepository {
    public JsonValue oauth2Create(JsonValue request) throws JsonResourceException {
        return null;
    }

    public JsonValue oauth2Read(JsonValue request) throws JsonResourceException{
        return null;
    }

    public JsonValue oauth2Update(JsonValue request) throws JsonResourceException{
        return null;
    }

    public JsonValue oauth2Delete(JsonValue request) throws JsonResourceException{
        return null;
    }

    public JsonValue oauth2Query(JsonValue request) throws JsonResourceException{
        return null;
    }

    public void oauth2DeleteWithFilter(String filter) throws JsonResourceException{
        return;
    }

    public void oauth2Delete(String id) throws JsonResourceException{
        return;
    }
}

