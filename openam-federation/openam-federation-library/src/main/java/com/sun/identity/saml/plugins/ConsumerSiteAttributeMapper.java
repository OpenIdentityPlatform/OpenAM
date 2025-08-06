/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: ConsumerSiteAttributeMapper.java,v 1.3 2009/01/21 19:04:34 weisun2 Exp $
 *
 * Portions Copyrighted 2025 3A Systems LLC.
 */


package com.sun.identity.saml.plugins;

import com.sun.identity.saml.common.SAMLException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * This interface needs to be implemented by an trusted assertion consumer site 
 * (a partner) to return a list of <code>Attribute</code> objects to be
 * returned as <code>AttributeStatements</code> elements, as part of the
 * Authentication Assertion returned to the partner during the
 * SSO scenario of Browser Artifact and POST profile.
 * <p>
 * Different partner would need to have a different implementation
 * of the interface. The mappings between the partner source ID and
 * the implementation class are configured at the <code>Partner URLs</code>
 * field in SAML service.
 *
 * 
 */
public interface ConsumerSiteAttributeMapper {

    /**
     * Returns <code>List</code> of <code>Attribute</code> objects
     *
     * @param token  User's session.
     * @param request The HttpServletRerquest object of the request which
     *                may contains query attributes to be included in the
     *                Assertion. This could be null if unavailable.
     * @param response The HttpServletResponse object. This could be null 
     *                if unavailable.
     * @param targetURL value for TARGET query parameter when the user
     *                  accessing the SAML aware servlet or post profile
     *                  servlet. This could be null if unavailabl
     * @return <code>List</code> if <code>Attribute</code> objects.
     *         <code>Attribute</code> is defined in the SAML SDK as part of
     *         <code>com.sun.identity.saml.assertion</code> package.
     * @throws SAMLException if attributes cannot be obtained.
     */
    public List getAttributes(Object token, HttpServletRequest request,
        HttpServletResponse response, String targetURL)
        throws SAMLException;
}
