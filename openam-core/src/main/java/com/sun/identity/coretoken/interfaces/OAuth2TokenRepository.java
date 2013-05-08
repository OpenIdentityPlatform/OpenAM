/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright © 2012-2013 ForgeRock Inc. All rights reserved.
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
 * "Portions Copyrighted [year] [name of copyright owner]"

 */

package com.sun.identity.coretoken.interfaces;

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.JsonResourceException;

public interface OAuth2TokenRepository {

    static final String EXPDATE_FILTER_PRE_OAUTH = "(";
    static final String EXPDATE_FILTER_COMPARE = "<=";
    static final String EXPDATE_FILTER_POST_OAUTH = ")";

    static final String SYS_PROPERTY_TOKEN_OAUTH2_REPOSITORY_ROOT_SUFFIX = "iplanet-am-token-oauth2-root-suffix";
    /**
     * Creates a token in the OpenDJ instance
     * @param request a JsonValue created from a Token
     * @return returns the created token
     * @throws JsonResourceException
     */
    public JsonValue oauth2Create(JsonValue request) throws JsonResourceException;

    /**
     * Read a token from the OpenDJ store given a request that contains an id of the token.
     * @param request a JsonValue containing an id value to retrieve
     * @return A JsonValue containing the returned token Map<String, Set<String>>
     * @throws JsonResourceException
     */
    public JsonValue oauth2Read(JsonValue request) throws JsonResourceException;

    /**
     *  Calls create with the request.
     * @param request a request containing a token to create
     * @return returns the created token
     * @throws JsonResourceException
     */
    public JsonValue oauth2Update(JsonValue request) throws JsonResourceException;

    /**
     * Deletes a token
     * @param request a JsonValue containing an id value of a token to delete
     * @return returns a null JsonValue
     * @throws JsonResourceException
     */
    public JsonValue oauth2Delete(JsonValue request) throws JsonResourceException;

    /**
     * Queries the OpenDJ store given a set of filters
     * @param request request contains a filter value which has a valid LDAP filter command
     *                The filter values key is a Map<String, String> where the key is the key to filter
     *                on and the value is the value to filter for.
     * @return returns a Set of tokens that match the filter.
     * @throws JsonResourceException
     */
    public JsonValue oauth2Query(JsonValue request) throws JsonResourceException;

    /**
     * Deletes a set of tokens given an ldap filter to delete on.
     * @param filter the filter to delete the tokens on.
     * @throws JsonResourceException
     */
    public void oauth2DeleteWithFilter(String filter) throws JsonResourceException;

    /**
     * Delete a single token
     * @param id identifier of the token to delete
     * @throws JsonResourceException
     */
    public void oauth2Delete(String id) throws JsonResourceException;
}
