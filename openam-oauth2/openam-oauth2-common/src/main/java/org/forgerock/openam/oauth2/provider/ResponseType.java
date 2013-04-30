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
/**
 * "Portions Copyrighted 2012-2013 ForgeRock Inc"
 *
 */

package org.forgerock.openam.oauth2.provider;

import org.forgerock.openam.oauth2.model.CoreToken;

import java.util.Map;

/**
 * This class provides the functions that need to be implemented to create a response type for the authorize
 * endpoint.
 *
 * @supported.all.api
 */
public interface ResponseType {
    /**
     * Creates a token for a response type
     * @param data The data needed to create the token
     * @return  the created token
     */
    public CoreToken createToken(Map<String, Object> data);

    /**
     * Returns the location in the HTTP response the token should be returned
     * @return A string of either FRAGMENT or QUERY
     */
    public String getReturnLocation();

    /**
     * The parameter in the URI to return the token as
     */
    public String URIParamValue();
}
