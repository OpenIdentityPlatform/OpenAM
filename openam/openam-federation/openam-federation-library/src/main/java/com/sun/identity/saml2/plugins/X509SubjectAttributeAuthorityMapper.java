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
 * $Id: X509SubjectAttributeAuthorityMapper.java,v 1.2 2008/06/25 05:47:52 qcheng Exp $
 *
 */

package com.sun.identity.saml2.plugins;

import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.profile.AttributeQueryUtil;
import com.sun.identity.saml2.protocol.AttributeQuery;

/**
 * This class <code>X509SubjectAttributeAuthorityMapper</code> is the
 * implementation of the <code>AttributeAuthorityMapper</code> that is used by
 * attribute authority to process attribute query with profile
 * "urn:oasis:names:tc:SAML:2.0:profiles:query:attribute:X509".
 */ 
public class X509SubjectAttributeAuthorityMapper implements
    AttributeAuthorityMapper {

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
        String attrAuthorityEntityID, String realm) throws SAML2Exception {

        AttributeQueryUtil.validateEntityRequester(attrQuery,
            attrAuthorityEntityID, realm);
    }

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
        String attrAuthorityEntityID, String realm) throws SAML2Exception {

        AttributeQueryUtil.verifyAttrQuerySignature(attrQuery,
            attrAuthorityEntityID, realm);;
    }

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
        String attrAuthorityEntityID, String realm) throws SAML2Exception {

        return AttributeQueryUtil.getIdentityFromDataStoreX509Subject(
            attrQuery, attrAuthorityEntityID, realm);
    }

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
        String attrAuthorityEntityID, String realm) throws SAML2Exception {

        return AttributeQueryUtil.getUserAttributes((String)identity,
            attrQuery, attrAuthorityEntityID, realm);
    }
}
