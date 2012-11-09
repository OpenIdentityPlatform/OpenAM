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
package org.forgerock.openam.oauth2.model;

import java.util.Set;

import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;
import org.forgerock.openam.oauth2.OAuth2Constants;

/**
 * Implements the common methods and attributes of all standard OAuth 2 tokens
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public interface Token {
    /**
     * Get the string representation of the identifier of this token
     * <p/>
     *
     * 
     * @return unique identifier of the represented token
     */
    @JsonProperty(OAuth2Constants.Params.ACCESS_TOKEN)
    public String getToken();

    /**
     * Get tokens UserID
     * 
     * @return
     *          ID of user
     */
    @JsonIgnore
    public String getUserID();

    /**
     * Get Tokens Realm
     * 
     * @return
     *          the realm
     */
    public String getRealm();

    /**
     * Get tokens client
     * 
     * @return
     *          the {@link SessionClient} for the token
     */
    @JsonIgnore
    public SessionClient getClient();

    /**
     * Gets the tokens scope
     * 
     * @return
     *          Set of strings that are the tokens scope
     */
    @JsonIgnore
    public Set<String> getScope();

    /**
     * Get the exact expiration time in POSIX format.
     * 
     * @return long representation of the maximum valid date.
     */
    @JsonIgnore
    public long getExpireTime();

    /**
     * Checks if token is expired
     * 
     * @return
     *          true if expired
     *          false if not expired
     */
    public boolean isExpired();
}
