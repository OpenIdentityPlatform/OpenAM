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
 * $Id: DefaultIDPAuthenticationMethodMapper.java,v 1.4 2009/10/28 23:58:59 exu Exp $
 *
 */


package com.sun.identity.wsfederation.plugins;

import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.wsfederation.common.WSFederationUtils;
import com.sun.identity.wsfederation.common.WSFederationException;
import com.sun.identity.wsfederation.jaxb.entityconfig.IDPSSOConfigElement;
import com.sun.identity.wsfederation.meta.WSFederationMetaException;
import com.sun.identity.wsfederation.meta.WSFederationMetaUtils;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

/**
 * 
 * This class is an out of the box default implementation of interface
 * <code>IDPAuthenticationMethodMapper</code>.
 */ 

public class DefaultIDPAuthenticationMethodMapper 
    implements IDPAuthenticationMethodMapper {
    Debug debug = WSFederationUtils.debug;
    
   /**
    * Constructor
    */
    public DefaultIDPAuthenticationMethodMapper() {
    }

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
        String realm) 
        throws WSFederationException {

        String classMethod = 
            "DefaultIDPAuthnContextMapper.getIDPAuthnContextInfo: ";

        Map attrs = null;
        Set authTypeAndValues = null;
        IDPAuthenticationTypeInfo info = null;
        List requestedClassRefs = null;
        String requestedClassRef = null;
        List classRefs = null;
        String classRef = null;

        try {
            IDPSSOConfigElement config = 
                WSFederationUtils.getMetaManager().getIDPSSOConfig(
                    realm, idpEntityID);
            attrs = WSFederationMetaUtils.getAttributes(config);
        } catch (WSFederationMetaException sme) {
            debug.error(classMethod +
                   "get IDPSSOConfig failed:", sme);
            throw new WSFederationException(sme);
        }
        List values = (List) attrs.get(
                    SAML2Constants.IDP_AUTHNCONTEXT_CLASSREF_MAPPING);
        if ((values != null) && (values.size() != 0)) {
            if (authenticationType != null) {
                for (int i = 0; i < values.size(); i++) {
                    String value = ((String) values.get(i)).trim();
                    if (debug.messageEnabled()) {
                        debug.message(classMethod +
                            "configured mapping=" + value); 
                    }
                    StringTokenizer st = new StringTokenizer(value, "|");
                    if (st.hasMoreTokens()) {
                        // the first element is an AuthnContextClassRef 
                        classRef = ((String)st.nextToken()).trim();
                        if (classRef.equals(authenticationType)) {
                            authTypeAndValues = new HashSet();
                            while (st.hasMoreTokens()) {
                                String authTypeAndValue = 
                                    ((String)st.nextToken()).trim();
                                if (authTypeAndValue.length() != 0) {
                                    authTypeAndValues.add(authTypeAndValue);
                                }
                            }
                            break;
                        }
                    } 
                }
            }
            if (authTypeAndValues == null) {
                // no matching authnContextClassRef found in config, or
                // no valid requested authn class ref, use the first 
                // one in  the config 
                String value = ((String) values.get(0)).trim();
                StringTokenizer st = new StringTokenizer(value, "|");
                if (st.hasMoreTokens()) {
                    // the first element is an AuthnContextClassRef 
                    classRef = ((String)st.nextToken()).trim();
                    authTypeAndValues = new HashSet();
                    while (st.hasMoreTokens()) {
                        String authTypeAndValue = 
                            ((String)st.nextToken()).trim();
                        if (authTypeAndValue.length() != 0) {
                            authTypeAndValues.add(authTypeAndValue);
                        }
                    }
                } 
            }
            info = new IDPAuthenticationTypeInfo(authenticationType, 
                authTypeAndValues); 
            if (debug.messageEnabled()) {
                debug.message(classMethod +
                    "requested AuthnContextClassRef=" + requestedClassRef + 
                    "\nreturned AuthnContextClassRef=" + classRef + 
                    "\nauthTypeAndValues=" + authTypeAndValues);
            }
        } 
        return info;
    } 
}
