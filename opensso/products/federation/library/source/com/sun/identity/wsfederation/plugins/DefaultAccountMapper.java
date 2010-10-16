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
 * $Id: DefaultAccountMapper.java,v 1.5 2009/10/28 23:58:59 exu Exp $
 *
 */


package com.sun.identity.wsfederation.plugins;

import com.sun.identity.saml.assertion.NameIdentifier;
import com.sun.identity.saml2.common.NameIDInfoKey;
import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.wsfederation.jaxb.entityconfig.BaseConfigType;
import com.sun.identity.wsfederation.key.KeyUtil;
import com.sun.identity.wsfederation.meta.WSFederationMetaException;
import com.sun.identity.wsfederation.meta.WSFederationMetaUtils;
import com.sun.identity.wsfederation.common.AccountUtils;
import java.util.ResourceBundle;
import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import com.sun.identity.shared.debug.Debug;
import com.sun.identity.plugin.datastore.DataStoreProvider;

import com.sun.identity.saml.xmlsig.KeyProvider;

import com.sun.identity.wsfederation.common.WSFederationException;
import com.sun.identity.wsfederation.common.WSFederationUtils;

/**
 * This class <code>DefaultAccountMapper</code> is a base class extended by 
 * <code>DefaultSPAccountMapper</code> and <code>DefaultIDPAccountMapper</code>.
 * This class implements the common interface
 * methods that are required for the SP and IDP account mappers and also
 * provide some utility classes that can be shared between those mappers.
 */
public class DefaultAccountMapper {

     protected static Debug debug = WSFederationUtils.debug;
     protected static ResourceBundle bundle = WSFederationUtils.bundle;
     protected static DataStoreProvider dsProvider = null;
     protected static final String IDP = SAML2Constants.IDP_ROLE;
     protected static final String SP = SAML2Constants.SP_ROLE;
     protected String role = null; 
     protected static KeyProvider keyProvider = 
         KeyUtil.getKeyProviderInstance(); 

     static {
         try {
             dsProvider = WSFederationUtils.dsProvider; 
         } catch (Exception se) {
             debug.error("DefaultAccountMapper.static intialization " +
             "failed", se);
             throw new ExceptionInInitializerError(se);
         }
     }

     /**
      * Default constructor
      */
     public DefaultAccountMapper() {
         debug.message("DefaultAccountMapper.constructor: ");
     }

    /**
     * Returns the <code>NameIDInfoKey</code> key value pair that can
     * be used for searching the user.
     * @param nameID <code>NameID</code> object.
     * @param hostEntityID hosted <code>EntityID</code>.
     * @param remoteEntityID remote <code>EntityID</code>.
     * @exception <code>WSFederationException</code> if any failure.
     */
    protected Map getSearchParameters(NameIdentifier nameID, 
         String realm, String hostEntityID, String remoteEntityID) 
         throws WSFederationException {

         if(nameID == null) {
            throw new WSFederationException(bundle.getString(
                  "nullNameID"));
         }

         NameIDInfoKey infoKey = null;
         try
         {
             infoKey = new NameIDInfoKey(nameID.getName(), hostEntityID, 
                 remoteEntityID); 
         }
         catch (SAML2Exception se)
         {
             throw new WSFederationException(se);
         }
         
         HashSet set = new HashSet();
         set.add(infoKey.toValueString()); 

         Map keyMap = new HashMap();  
         keyMap.put(AccountUtils.getNameIDInfoKeyAttribute(), set);

         if(debug.messageEnabled()) {
            debug.message("DefaultAccountMapper.getNameIDKeyMap: " + keyMap);
         }
         return keyMap;
    }

    /**
     * Returns the attribute value configured in the given entity
     * SP or IDP configuration.
     * @param realm realm name.
     * @param entityID hosted <code>EntityID</code>.
     * @param attributeName name of the attribute.
     */
    protected String getAttribute(String realm,
	   String entityID, String attributeName) {

        if(realm == null || entityID == null || attributeName == null ) {
           if(debug.messageEnabled()) {
              debug.message("DefaultAccountMapper.getAttribute: " +
              "null input parameters.");
           }
           return null;
        }

	try {
            BaseConfigType config = null;
            if(role.equals(IDP)) {
               config = WSFederationUtils.getMetaManager().getIDPSSOConfig(
                   realm, entityID);
            } else {
               config = WSFederationUtils.getMetaManager().getSPSSOConfig(
                   realm, entityID);
            }
            Map attributes  = WSFederationMetaUtils.getAttributes(config);

            if(attributes == null || attributes.isEmpty()) {
		if(debug.messageEnabled()) {
		  debug.message("DefaultAccountMapper.getAttribute:" +
		  " attribute configuration is not defined for " +
		  "Entity " + entityID + " realm =" + realm + " role=" + role);
		}
               return null;
            }

            List list = (List)attributes.get(attributeName);
            if(list != null && list.size() > 0) {
		return (String)list.iterator().next();
            }

            if(debug.messageEnabled()) {
		debug.message("DefaultSPAccountMapper.getAttribute: " +
		attributeName + " is not configured.");
            }
            return null;

	} catch (WSFederationMetaException sme) {
            if(debug.warningEnabled()) {
		debug.warning("DefaultSPAccountMapper.getAttribute:" +
		"Meta Exception", sme);
            }
	}
        return null;
    }

}
