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
 * $Id: IDPAuthenticationMethodMapper.java,v 1.3 2008/06/25 05:48:07 qcheng Exp $
 *
 */


package com.sun.identity.wsfederation.plugins;

import com.sun.identity.wsfederation.common.WSFederationException;

/**
 * 
 * The interface <code>IDPAuthenticationMethodMapper</code> creates an
 * <code>IDPAuthenticationTypeInfo<code> based on the RequestAuthnContext from
 * the AuthnRequest sent by a Service Provider and the AuthnContext
 * configuration at the IDP entity config.
 * The implementation of this class will be used by the IDP to find out
 * the authentication mechaism and set the AuthnContext in the Assertion.
 * 
 * 
 * @supported.all.api 
 */ 

public interface IDPAuthenticationMethodMapper {

   /**
     * 
     * Returns an <code>IDPAuthenticationTypeInfo</code> object.
     * 
     * @param authenticationType the <code>AuthenticationType</code> from the 
     * Service Provider
     * @param idpEntityID the Entity ID of the Identity Provider
     * @param realm the realm to which the Identity Provider belongs
     * @return an <code>IDPAuthenticationTypeInfo</code> object
     * @throws WSFederationException if an error occurs.
     */
    public IDPAuthenticationTypeInfo getIDPAuthnContextInfo(
        String authenticationType,
        String idpEntityID,
        String realm) throws WSFederationException;

}
