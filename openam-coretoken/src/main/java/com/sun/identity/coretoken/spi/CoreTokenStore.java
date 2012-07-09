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
 * If applicable, addReferral the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * $Id: CoreTokenStore.java,v 1.1 2009/11/19 00:07:41 qcheng Exp $
 */

package com.sun.identity.coretoken.spi;

import com.sun.identity.coretoken.CoreTokenException;
import javax.security.auth.Subject;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * This <code>CoreTokenStore</code> interface defines methods for managing
 * resources of the OpenSSO Core Token service.  
 */
public interface CoreTokenStore {

    /**
     * Creates a token.
     *
     * @param subject Subject of the caller.
     * @param attributes Attributes of the token to be created.
     * @return JSON-encoded token.id attribute.
     * @throws CoreTokenException if failed to create the token.
     * @throws JSONException if failed to parse the JSON-encoded token
     *                     attributes.
     */
    public String createToken(Subject subject,
        JSONObject attributes)
        throws CoreTokenException, JSONException;

    /**
     * Reads token attributes.
     *
     * @param subject Subject of the caller.
     * @param tokenId token.id of the token to be retrieved.
     * @return JSON-encoded token attributes.
     * @throws CoreTokenException if failed to read the token.
     */
    public String readToken(Subject subject, String tokenId)
        throws CoreTokenException;

    /**
     * Deletes a token.
     *
     * @param subject Subject of the caller
     * @param tokenId token.id of the token to be deleted.
     * @throws CoreTokenException if failed to delete the token.
     * @throws JSONException if failed to parse the JSON-encoded token
     *                       attributes.
     */
    public void deleteToken(Subject subject, String tokenId)
        throws CoreTokenException;

    /**
     * Searches tokens.
     * @param subject Subject of the caller
     * @param queryString HTTP query string.
     * @return JSON array of token.id values for resources that match
     *         the query. May be empty.
     * @throws CoreTokenException
     */
    public JSONArray searchTokens (Subject subject,
        String queryString) throws CoreTokenException;

    /**
     * Updates a token.
     * @param subject caller subject.
     * @param tokenId token.id of the token to be updated.
     * @param eTag etag attribute value, this must match that presents in the
     *             the token to be updated.
     * @param newVals attributes to be updated.
     * @throws CoreTokenException if failed to update the token.
     * @throws JSONException if failed to parse the JSON-encoded attributes.
     */
    public void updateToken(Subject subject, String tokenId,
        String eTag, JSONObject newVals)
        throws CoreTokenException, JSONException;
}
