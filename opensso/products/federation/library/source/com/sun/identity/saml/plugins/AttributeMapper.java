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
 * $Id: AttributeMapper.java,v 1.4 2008/08/19 19:11:13 veiming Exp $
 *
 */


package com.sun.identity.saml.plugins;

import com.sun.identity.saml.assertion.Assertion;
import com.sun.identity.saml.common.SAMLException;
import com.sun.identity.saml.protocol.AttributeQuery;

import java.util.List;

/**
 * The class <code>AttributeMapper</code> is an interface that is 
 * implemented to get SSO information and map partner attributes to
 * OpenSSO attributes.
 * <p>
 * A different implementation of the interface may be developed for different
 * partner. The mapping between the partner source ID and the implementation
 * class are configured at the <code>Trusted Partner Sites</code> field
 * in SAML service.
 *
 * @supported.all.api
 */
public interface AttributeMapper {

    /**
     * Returns the single sign on token id to OpenSSO from the query.
     * @param query The received <code>AttributeQuery</code>.
     * @return String which is the single sign on token ID.
     */
    public String getSSOTokenID(AttributeQuery query);

    /**
     * Returns the Assertion that contains Authentication information that
     * can be used to obtain single sign on token.
     *
     * @param query The received <code>AttributeQuery</code>.
     * @return Assertion The assertion contained inside the query.
     */
    public Assertion getSSOAssertion(AttributeQuery query);

    /**
     * Returns Attributes for the user.
     *
     * @param query The received <code>AttributeQuery</code>.
     * @param sourceID source ID for the site from which the query originated.
     * @param token User' session to retrieve the attributes.
     * @return A List of Attributes
     * @exception SAMLException if an error occurs
     */
    public List getAttributes(AttributeQuery query, String sourceID,
         Object token) throws SAMLException;
}
