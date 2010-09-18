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
 * $Id: SPAuthnContextMapper.java,v 1.5 2008/06/25 05:47:52 qcheng Exp $
 *
 */


package com.sun.identity.saml2.plugins;

import com.sun.identity.saml2.assertion.AuthnContext;
import com.sun.identity.saml2.protocol.RequestedAuthnContext;
import com.sun.identity.saml2.common.SAML2Exception;
import java.util.List;
import java.util.Map;

/**
 * The interface <code>SPAuthnContextMapper.java</code> determines
 * the Authentication Context to be set in the Authentication Request
 * and the Auth Level of an Authentication Context.
 *
 * The implementation of this interface will be used to create 
 * <code>RequestedAuthnContext</code> to set in the <code>AuthnRequest</code>
 * and the Authentication Level of an Authentication Context.
 *
 * @supported.all.api
 */

public interface SPAuthnContextMapper {

    /**
     * Returns the <code>RequestedAuthnContext</code> Object .
     * This method is called during Single Sign On initiation
     * at the Service Provider for determining the 
     * <code>RequestedAuthnContext</code>  to be set in the 
     * <code>AuthRequest</code> before sending the request to
     * the Identity Provider.
     *
     * @param realm Organization or realm of the Service Provider.
     * @param hostEntityID Entity Identifier of the Host.
     * @param paramsMap Map containing key/value pairs of request parameters.
     * @return RequestedAuthnContext Object.
     * @throws SAML2Exception if an error occurs.
     */
    public RequestedAuthnContext getRequestedAuthnContext(String realm,
					   String hostEntityID,
                                           Map paramsMap)
                                           throws SAML2Exception;

    /**
     * Returns the Auth Level for the <code>AuthContext</code>.
     *
     * This method is called by the Service Provider to determine
     * the authLevel of Identity Provider Authentication Context
     * which will set in the SSOToken created for the user on successful
     * authentication.
     *
     * @param reqCtx the <code>RequestedAuthContext</code> object.
     * @param authContext the <code>AuthContext</code> object.
     * @param realm the organization or realm of the Service Provider.
     * @param hostEntityID the Hosted Provider Entity ID.
     * @param idpEntityID the Identity Provider Entity ID.
     * @return authlevel of the <code>AuthContext</code>.
     * @throws SAML2Exception if an error occurs.
     */
    public int getAuthLevel(RequestedAuthnContext reqCtx,
			    AuthnContext authContext,String realm,
			    String hostEntityID, String idpEntityID) 
			    throws SAML2Exception;

    /** 
     * Returns true if the specified AuthnContextClassRef matches a list of
     * requested AuthnContextClassRef.
     *
     * @param requestedACClassRefs a list of requested AuthnContextClassRef's
     * @param acClassRef AuthnContextClassRef
     * @param comparison the type of comparison
     * @param realm  Realm or Organization of the Service Provider.
     * @param hostEntityID Entity ID of the Service Provider.
     * 
     * @return true if the specified AuthnContextClassRef matches a list of
     *     requested AuthnContextClassRef
     */
    public boolean isAuthnContextMatching(List requestedACClassRefs,
        String acClassRef, String comparison, String realm,
        String hostEntityID);
}
