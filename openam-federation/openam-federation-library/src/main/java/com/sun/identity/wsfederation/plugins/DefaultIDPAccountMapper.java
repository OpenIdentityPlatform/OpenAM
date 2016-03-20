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
 * $Id: DefaultIDPAccountMapper.java,v 1.7 2009/10/28 23:58:59 exu Exp $
 *
 * Portions Copyrighted 2016 ForgeRock AS.
 */
package com.sun.identity.wsfederation.plugins;

import static org.forgerock.openam.utils.AttributeUtils.*;

import java.util.Set;

import org.forgerock.openam.utils.CollectionUtils;
import org.forgerock.openam.utils.StringUtils;

import com.sun.identity.plugin.datastore.DataStoreProviderException;
import com.sun.identity.saml.assertion.NameIdentifier;
import com.sun.identity.saml.common.SAMLException;

import com.sun.identity.plugin.session.SessionManager;
import com.sun.identity.plugin.session.SessionProvider;
import com.sun.identity.plugin.session.SessionException;

import com.sun.identity.shared.encode.Base64;
import com.sun.identity.wsfederation.common.WSFederationConstants;
import com.sun.identity.wsfederation.common.WSFederationException;
import com.sun.identity.wsfederation.common.WSFederationUtils;
import com.sun.identity.wsfederation.jaxb.entityconfig.IDPSSOConfigElement;
import com.sun.identity.wsfederation.meta.WSFederationMetaUtils;

/**
 * This class <code>DefaultIDPAccountMapper</code> is the default
 * implementation of the <code>IDPAccountMapper</code> that is used
 * to map the <code>SAML</code> protocol objects to the user accounts.
 * at the <code>IdentityProvider</code> side of the WS-Federation 
 * implementation.
 * Custom implementations may extend from this class to override some
 * of these implementations if they choose to do so.
 */

public class DefaultIDPAccountMapper extends DefaultAccountMapper 
     implements IDPAccountMapper {

     public DefaultIDPAccountMapper() {
         debug.message("DefaultIDPAccountMapper.constructor");
         role = IDP;
     }

    /**
     * Returns the user's <code>NameID</code>information that contains
     * account federation with the corresponding remote and local entities.
     *
     * @param session Session object.
     * @param realm Realm where user resides.
     * @param hostEntityID <code>EntityID</code> of the hosted provider.
     * @param remoteEntityID <code>EntityID</code> of the remote provider.
     * @return the <code>NameID</code> corresponding to the authenticated user.
     *         null if the authenticated user does not container account
     *              federation information.
     * @exception WSFederationException if any failure.
     */
    public NameIdentifier getNameID(
        Object session,
        String realm,
        String hostEntityID,
        String remoteEntityID
    ) throws WSFederationException {
        String userID = null;
        try {
            SessionProvider sessionProv = SessionManager.getProvider();
            userID = sessionProv.getPrincipalName(session);
        } catch (SessionException se) {
            throw new WSFederationException(WSFederationUtils.bundle.getString(
                   "invalidSSOToken")); 
        }
        
        IDPSSOConfigElement idpConfig = 
            WSFederationUtils.getMetaManager().getIDPSSOConfig(
                realm, hostEntityID);

        String name2 = null;
        try {
            String attrName = WSFederationMetaUtils.getAttribute(idpConfig, WSFederationConstants.NAMEID_ATTRIBUTE);
            if (StringUtils.isEmpty(attrName)) {
                attrName = WSFederationConstants.UID;
            }
            if (isBinaryAttribute(attrName)) {
                attrName = removeBinaryAttributeFlag(attrName);
                byte[][] attributeValues = dsProvider.getBinaryAttribute(userID, attrName);
                if (attributeValues != null && attributeValues.length > 0) {
                    name2 = Base64.encode(attributeValues[0]);
                }
            } else {
                Set<String> attributeValues = dsProvider.getAttribute(userID, attrName);
                if (CollectionUtils.isNotEmpty(attributeValues)) {
                    name2 = attributeValues.iterator().next();
                }
            }
            if (name2 == null) {
                String [] args = { attrName, userID };
                throw new WSFederationException(WSFederationConstants.BUNDLE_NAME, "missingNameAttribute", args);
            }
        } catch (DataStoreProviderException dspe) {
            throw new WSFederationException(dspe);
        }

        String nameIdFormat = WSFederationMetaUtils.getAttribute(idpConfig,
            WSFederationConstants.NAMEID_FORMAT);
        if ( nameIdFormat == null || nameIdFormat.length() == 0 ) {
            nameIdFormat = WSFederationConstants.NAMED_CLAIM_TYPES[
                WSFederationConstants.NAMED_CLAIM_UPN];
        }

        String strNameIncludesDomain = 
            WSFederationMetaUtils.getAttribute(idpConfig,
            WSFederationConstants.NAME_INCLUDES_DOMAIN);
        boolean nameIncludesDomain = Boolean.valueOf(strNameIncludesDomain);

        String name = null;
        if ( nameIdFormat.equals(WSFederationConstants.NAMED_CLAIM_TYPES[
            WSFederationConstants.NAMED_CLAIM_UPN]) && ! nameIncludesDomain) {
            // Need to get a domain from somewhere and append it to name2
            // Try user profile first
            String domainAttribute = 
                WSFederationMetaUtils.getAttribute(idpConfig,
                WSFederationConstants.DOMAIN_ATTRIBUTE);
            String upnDomain = null;
            if ( domainAttribute != null && domainAttribute.length() > 0 )
            {
                Set attrValues;
                try {
                    attrValues = dsProvider.getAttribute(userID, domainAttribute);
                } catch (DataStoreProviderException dspe) {
                    throw new WSFederationException(dspe);
                }
                if ((attrValues != null) && (!attrValues.isEmpty())) {
                    upnDomain = (String)attrValues.iterator().next();
                }
            }
            
            if ( upnDomain == null || upnDomain.length() == 0 ) {
                // Nothing on the user profile - get from config
                upnDomain = WSFederationMetaUtils.getAttribute(idpConfig,
                    WSFederationConstants.UPN_DOMAIN);
            }
            
            if ( upnDomain == null || upnDomain.length() == 0 )
            {
                // OK - now we have a problem
                throw new WSFederationException(WSFederationConstants.BUNDLE_NAME,
                    "noDomainConfigured",null);
            }
            
            name = name2 + "@" + upnDomain;
        } else {
            name = name2;
        }

        try {
           return new NameIdentifier(name, null, nameIdFormat);
        }
        catch (SAMLException se){
            throw new WSFederationException(se);
        }
    }
}
