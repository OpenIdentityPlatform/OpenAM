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
 * $Id: DefaultADFSPartnerAccountMapper.java,v 1.5 2009/10/29 00:03:49 exu Exp $
 *
 */

package com.sun.identity.wsfederation.plugins;

import com.sun.identity.shared.datastruct.CollectionHelper;
import com.sun.identity.authentication.service.AuthD;
import com.sun.identity.authentication.util.ISAuthConstants;
import com.sun.identity.sm.OrganizationConfigManager;
import com.sun.identity.sm.ServiceConfig;
import com.sun.identity.saml.assertion.NameIdentifier;
import com.sun.identity.wsfederation.common.WSFederationConstants;
import com.sun.identity.wsfederation.common.WSFederationException;
import com.sun.identity.wsfederation.common.WSFederationUtils;
import com.sun.identity.wsfederation.jaxb.entityconfig.IDPSSOConfigElement;
import com.sun.identity.wsfederation.meta.WSFederationMetaException;
import com.sun.identity.wsfederation.meta.WSFederationMetaUtils;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 * This default <code>PartnerAccountMapper</code> for ADFS uses configuration 
 * to determine the attribute on which to search for the incoming user 
 * identifier.
 */
public class DefaultADFSPartnerAccountMapper 
    extends DefaultLibrarySPAccountMapper {

     /**
      * Default constructor
      */
    public DefaultADFSPartnerAccountMapper() {
        super();
        debug.message("DefaultADFSPartnerAccountMapper.constructor: ");
    }
    
    /**
     * This method simply extracts the NameIDValue and constructs a search map
     * according to the configuration.
     * @param nameID NameIdentifier for the subject
     * @param hostEntityID entity ID of the identity provider
     * @param remoteEntityID entity ID of the service provider
     */
    protected Map getSearchParameters(NameIdentifier nameID, 
        String realm, String hostEntityID, String remoteEntityID) 
        throws WSFederationException
    {
        String classMethod = 
            "DefaultADFSPartnerAccountMapper.getSearchParameters: ";
        
        // Get configuration for this IdP
        IDPSSOConfigElement idpConfig = null;
        try {
            idpConfig = 
                WSFederationUtils.getMetaManager().getIDPSSOConfig(
                    realm, remoteEntityID);
        } catch (WSFederationMetaException wsfme) {
            throw new WSFederationException(wsfme);
        }
        
        String nameIdAttribute = WSFederationMetaUtils.getAttribute(idpConfig,
                WSFederationConstants.NAMEID_ATTRIBUTE);
        // Search on uid by default
        if ( nameIdAttribute == null || nameIdAttribute.length() == 0) {
            nameIdAttribute = WSFederationConstants.UID;
        }
        String domainAttribute = WSFederationMetaUtils.getAttribute(idpConfig,
                WSFederationConstants.DOMAIN_ATTRIBUTE);
        String strNameIncludesDomain = 
            WSFederationMetaUtils.getAttribute(idpConfig,
            WSFederationConstants.NAME_INCLUDES_DOMAIN);
        boolean nameIncludesDomain = Boolean.valueOf(strNameIncludesDomain);

        String nameValue = nameID.getName();
        if (nameValue == null || nameValue.length() == 0 ) {
            throw new WSFederationException(WSFederationConstants.BUNDLE_NAME,
                "nullNameID",null);
        }
        
        // Now construct the key map
        Map keyMap = new HashMap();  
        String name = null;

        if ( nameID.getFormat().equals(WSFederationConstants.NAMED_CLAIM_TYPES[
            WSFederationConstants.NAMED_CLAIM_UPN]) && ! nameIncludesDomain) {
            int atSign = nameValue.indexOf('@');
            if ( atSign == -1 ) {
                String[] args = {nameValue};
                throw new 
                    WSFederationException(WSFederationConstants.BUNDLE_NAME,
                    "missingAtInUpn",args);
            }
            
            name = nameValue.substring(0,atSign);
            String upnDomain = nameValue.substring(atSign+1);

            if ( domainAttribute != null && domainAttribute.length() > 0) {
                HashSet set = new HashSet();
                set.add(upnDomain);
                keyMap.put(domainAttribute, set); 
            }

            if ( debug.messageEnabled() ) {
                debug.message(classMethod + "domain is "+upnDomain);
            }
        } else {
            name = nameValue;
        } 

        if ( debug.messageEnabled() ) {
            debug.message(classMethod + "name is "+name);
        }

        HashSet set = new HashSet();
        set.add(name);
        keyMap.put(nameIdAttribute, set); 

        return keyMap;
    }

    /**
     * Checks if dynamical profile creation or ignore profile is enabled.
     * @param realm realm to check the dynamical profile creation attributes.
     * @return true if dynamical profile creation or ignore profile is enabled,
     * false otherwise.
     */
    protected boolean isDynamicalOrIgnoredProfile(String realm) {
        try {
            OrganizationConfigManager orgConfigMgr = AuthD.getAuth().
                getOrgConfigManager(realm);
            ServiceConfig svcConfig = orgConfigMgr.getServiceConfig(
                ISAuthConstants.AUTH_SERVICE_NAME);
            Map attrs = svcConfig.getAttributes();
            String tmp = CollectionHelper.getMapAttr(
                attrs, ISAuthConstants.DYNAMIC_PROFILE);
            if (debug.messageEnabled()) {
                debug.message("dynamicalCreationEnabled, attr=" + tmp);
            }
            if (tmp != null && (tmp.equalsIgnoreCase("createAlias")
                || tmp.equalsIgnoreCase("true")
                || tmp.equalsIgnoreCase("ignore"))) {
                return true;
            } else {
                return false;
            }
        } catch (Exception e) {
            debug.error("dynamicalCreationEnabled, unable to get attribute", e);
            return false;
        }
    }
}
