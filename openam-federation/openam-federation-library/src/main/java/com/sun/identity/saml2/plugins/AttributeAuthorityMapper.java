/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: AttributeAuthorityMapper.java,v 1.3 2008/12/03 00:34:10 hengming Exp $
 *
 * Portions Copyrighted 2025 3A Systems LLC.
 */

package com.sun.identity.saml2.plugins;

import java.util.List;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.protocol.AttributeQuery;

/**
 * This interface <code>AttributeAuthorityMapper</code> is used by attribute
 * authority to process attribute query. Make sure to use thread-safe code if 
 * you implement the AttributeAuthorityMapper. You can use the attributes on 
 * the HttpRequest instead of synchronizing them. The default 
 * AttributeAuthorityMapper uses an attribute on the HttpServletRequest to
 * pass information to the AttributeQueryUtil.
 *
 * 
 */ 
public interface AttributeAuthorityMapper {

    /**
     * Checks if the attribute query requester is valid.
     *
     * @param request HttpServletRequest
     * @param response HttpServletResponse
     * @param attrQuery attribute query
     * @param attrAuthorityEntityID entity ID of attribute authority
     * @param realm the realm of hosted entity
     *
     * @exception SAML2Exception if the request is not valid. 
     */
    public void authenticateRequester(HttpServletRequest request,
        HttpServletResponse response, AttributeQuery attrQuery,
        String attrAuthorityEntityID, String realm) throws SAML2Exception;

    /**
     * Checks if the attribute query is valid.
     *
     * @param request HttpServletRequest
     * @param response HttpServletResponse
     * @param attrQuery attribute query
     * @param attrAuthorityEntityID entity ID of attribute authority
     * @param realm the realm of hosted entity
     *
     * @exception SAML2Exception if the attribute query is not valid. 
     */
    public void validateAttributeQuery(HttpServletRequest request,
        HttpServletResponse response, AttributeQuery attrQuery,
        String attrAuthorityEntityID, String realm) throws SAML2Exception;

    /**
     * Returns an identity that matches the subject in the attribute query.
     *
     * @param request HttpServletRequest
     * @param response HttpServletResponse
     * @param attrQuery attribute query
     * @param attrAuthorityEntityID entity ID of attribute authority
     * @param realm the realm of hosted entity
     * @return an identity that matches the subject in the attribute query.
     *
     * @exception SAML2Exception if error occurs. 
     */
    public Object getIdentity(HttpServletRequest request,
        HttpServletResponse response, AttributeQuery attrQuery,
        String attrAuthorityEntityID, String realm) throws SAML2Exception;

    /**
     * Returns attributes of the specifed identity.
     *
     * @param identity the identity
     * @param attrQuery attribute query
     * @param attrAuthorityEntityID entity ID of attribute authority
     * @param realm the realm of hosted entity
     * @return a list of
     *     <code>com.sun.identity.saml2.assertion.Attribute</code>.
     *
     * @exception SAML2Exception if error occurs. 
     */
    public List getAttributes(Object identity, AttributeQuery attrQuery,
        String attrAuthorityEntityID, String realm) throws SAML2Exception;

}
