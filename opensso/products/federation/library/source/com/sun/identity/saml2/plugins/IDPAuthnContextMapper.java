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
 * $Id: IDPAuthnContextMapper.java,v 1.6 2008/06/25 05:47:51 qcheng Exp $
 *
 */


package com.sun.identity.saml2.plugins;

import java.util.List;

import com.sun.identity.saml2.assertion.AuthnContext;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.protocol.AuthnRequest;

/** 
 * The interface <code>IDPAuthnContextMapper</code> creates an
 * <code>IDPAuthnContextInfo<code> based on the RequestAuthnContext from
 * the AuthnRequest sent by a Service Provider and the AuthnContext
 * configuration at the IDP entity config.
 * The implementation of this class will be used by the IDP to find out
 * the authentication mechaism and set the AuthnContext in the Assertion.
 *
 * @supported.all.api
 */ 

public interface IDPAuthnContextMapper {

   /** 
    * Returns an <code>IDPAuthnContextInfo</code> object.
    *
    * @param authnRequest the <code>AuthnRequest</code> from the Service 
    * Provider
    * @param idpEntityID the Entity ID of the Identity Provider    
    * @param realm the realm to which the Identity Provider belongs
    * 
    * @return an <code>IDPAuthnContextInfo</code> object
    * @throws SAML2Exception if an error occurs.
    */
    public IDPAuthnContextInfo getIDPAuthnContextInfo(
        AuthnRequest authnRequest,
        String idpEntityID,
        String realm) throws SAML2Exception;

   /** 
    * Returns true if the specified AuthnContextClassRef matches a list of
    * requested AuthnContextClassRef.
    *
    * @param requestedACClassRefs a list of requested AuthnContextClassRef's
    * @param acClassRef AuthnContextClassRef
    * @param comparison the type of comparison
    * @param realm the realm to which the Identity Provider belongs
    * @param idpEntityID the Entity ID of the Identity Provider    
    * 
    * @return true if the specified AuthnContextClassRef matches a list of
    *     requested AuthnContextClassRef
    */
    public boolean isAuthnContextMatching(List requestedACClassRefs,
        String acClassRef, String comparison, String realm,
        String idpEntityID);

    /**
     * Returns <code>AuthnContext</code> that matches the authenticated level.
     * @param authLevel user authenticated level
     * @param realm the realm to which the Identity Provider belongs
     * @param idpEntityID the Entity ID of the Identity Provider
     *
     * @return <code>AuthnContext</code> object that matches authenticated
     *  level.
     * @throws SAML2Exception if an error occurs.
     */
    public AuthnContext getAuthnContextFromAuthLevel(
        String authLevel, String realm, String idpEntityID)
        throws SAML2Exception;
}
