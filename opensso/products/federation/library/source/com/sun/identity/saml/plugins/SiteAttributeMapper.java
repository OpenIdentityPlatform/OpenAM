/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: SiteAttributeMapper.java,v 1.2 2008/06/25 05:47:36 qcheng Exp $
 *
 */


package com.sun.identity.saml.plugins;

import com.sun.identity.saml.common.SAMLException;
import java.util.List;

/**
 * This interface needs to be implemented by partner site to return a
 * list of <code>Attribute</code> objects which they want returned
 * as <code>AttributeStatements</code> elements, as part of the
 * Authentication Assertion returned to the partner during the
 * SSO scenario of Browser Artifact and POST profile.
 * <p>
 * Different partner would need to have a different implementation
 * of the interface. The mappings between the partner source ID and 
 * the implementation class are configured at the <code>Partner URLs</code> 
 * field in SAML service.
 * @deprecated This class has been deprecated. Please use
 *      <code>PartnerSiteAttributeMapper</code> instead.
 * @see PartnerSiteAttributeMapper
 *
 *
 * @supported.all.api
 */
public interface SiteAttributeMapper {

    /**
     * Returns <code>List</code> of <code>Attribute</code> objects
     *
     * @param token User's session.
     * @return <code>List</code> if <code>Attribute</code> objects.
     *         <code>Attribute</code> is defined in the SAML SDK as part of
     *         <code>com.sun.identity.saml.assertion</code> package.
     * @throws SAMLException if attributes cannot be obtained.
     */
    public List getAttributes(Object token) throws SAMLException;
}
