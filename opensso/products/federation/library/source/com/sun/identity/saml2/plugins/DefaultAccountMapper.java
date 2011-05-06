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
 * $Id: DefaultAccountMapper.java,v 1.4 2008/06/25 05:47:50 qcheng Exp $
 *
 */


package com.sun.identity.saml2.plugins;

import java.util.ResourceBundle;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.security.PrivateKey;

import com.sun.identity.shared.debug.Debug;
import com.sun.identity.plugin.datastore.DataStoreProviderException;
import com.sun.identity.plugin.datastore.DataStoreProvider;

import com.sun.identity.saml.xmlsig.KeyProvider;
import com.sun.identity.saml2.assertion.NameID;
import com.sun.identity.saml2.assertion.EncryptedID;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.saml2.common.SAML2Utils;
import com.sun.identity.saml2.common.NameIDInfoKey;
import com.sun.identity.saml2.common.AccountUtils;
import com.sun.identity.saml2.key.KeyUtil;
import com.sun.identity.saml2.meta.SAML2MetaManager;
import com.sun.identity.saml2.meta.SAML2MetaUtils;
import com.sun.identity.saml2.meta.SAML2MetaException;
import com.sun.identity.saml2.jaxb.entityconfig.BaseConfigType;
import com.sun.identity.saml2.protocol.ManageNameIDRequest;


/**
 * This class <code>DefaultAccountMapper</code> is a base class that the
 * <code>DefaultSPAccountMapper</code> and <code>DefaultIDPAccountMapper</code>
 * shall extend from this class. This class implements the common interface
 * methods that are required for the SP and IDP account mappers and also
 * provide some utility classes that can be shared between these mappers.
 */
public class DefaultAccountMapper {

     protected static Debug debug = SAML2Utils.debug;
     protected static ResourceBundle bundle = SAML2Utils.bundle;
     protected static DataStoreProvider dsProvider = null;
     protected static SAML2MetaManager metaManager = null;
     protected static final String IDP = SAML2Constants.IDP_ROLE;
     protected static final String SP = SAML2Constants.SP_ROLE;
     protected String role = null; 
     protected static KeyProvider keyProvider = KeyUtil.getKeyProviderInstance(); 

     static {
         try {
             dsProvider = SAML2Utils.getDataStoreProvider(); 
             metaManager= new SAML2MetaManager();
         } catch (Exception se) {
             debug.error("DefaultAccountMapper.static intialization " +
             "failed", se);
         }
     }

     /**
      * Default constructor
      */
     public DefaultAccountMapper() {
         debug.message("DefaultAccountMapper.constructor: ");
     }

    /**
     * Returns the user's disntinguished name or the universal ID for the 
     * corresponding  <code>SAML</code> <code>ManageNameIDRequest</code>.
     * This method will be invoked by the <code>SAML</code> framework for
     * retrieving the user identity while processing the
     * <code>ManageIDRequest</code>. 
     * @param manageNameIDRequest <code>SAML</code> 
     *     <code>ManageNameIDRequest</code> that needs to be mapped to the user.
     * @param hostEntityID <code>EntityID</code> of the hosted provider.
     * @param realm realm or the organization name that may be used to find
     *        the user information.
     * @return user's disntinguished name or the universal ID.
     * @exception SAML2Exception if any failure.
     */
    public String getIdentity(
        ManageNameIDRequest manageNameIDRequest,
        String hostEntityID,
        String realm
    ) throws SAML2Exception {

        if(manageNameIDRequest == null) {
           throw new SAML2Exception(bundle.getString(
                 "nullManageIDRequest")); 
        }

        if(hostEntityID == null) {
           throw new SAML2Exception(bundle.getString(
                 "nullHostEntityID")); 
        }

        if(realm == null) {
           throw new SAML2Exception(bundle.getString(
                 "nullRealm")); 
        }

        NameID nameID = null;
        EncryptedID encryptedID = manageNameIDRequest.getEncryptedID();

        if (encryptedID != null) {
            String alias = SAML2Utils.getEncryptionCertAlias(realm, 
        		    hostEntityID, role); 
            PrivateKey privateKey = keyProvider.getPrivateKey(alias);
            
            nameID = encryptedID.decrypt(privateKey);
        } else {
           nameID = manageNameIDRequest.getNameID();
        }

        String remoteEntityID = manageNameIDRequest.getIssuer().getValue();
        if(debug.messageEnabled()) {
           debug.message("DefaultAccountMapper.getIdentity(ManageNameIDReq)"+
           " realm = " + realm +" hostEntityID = " + hostEntityID);
        }

        try {
            return dsProvider.getUserID(realm, SAML2Utils.getNameIDKeyMap
                (nameID, hostEntityID, remoteEntityID, realm, role));

        } catch (DataStoreProviderException dse) {
            debug.error("DefaultAccountMapper.getIdentity(MNIRequest,):" +
            " DataStoreProviderException", dse);
            throw new SAML2Exception(dse.getMessage());
        }
         
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
               config = metaManager.getIDPSSOConfig(realm, entityID);
            } else {
               config = metaManager.getSPSSOConfig(realm, entityID);
            }
            Map attributes  = SAML2MetaUtils.getAttributes(config);

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

	} catch (SAML2MetaException sme) {
            if(debug.warningEnabled()) {
		debug.warning("DefaultSPAccountMapper.getAttribute:" +
		"Meta Exception", sme);
            }
	}
        return null;
    }

}
