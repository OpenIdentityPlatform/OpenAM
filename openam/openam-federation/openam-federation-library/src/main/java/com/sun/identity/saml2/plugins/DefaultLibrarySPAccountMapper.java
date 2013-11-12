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
 * $Id: DefaultLibrarySPAccountMapper.java,v 1.12 2009/03/12 20:34:45 huacui Exp $
 *
 */

/**
 * Portions Copyrighted 2013 ForgeRock, Inc.
 */
package com.sun.identity.saml2.plugins;

import java.security.PrivateKey;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;

import com.sun.identity.plugin.datastore.DataStoreProviderException;

import com.sun.identity.saml2.assertion.Attribute;
import com.sun.identity.saml2.assertion.AttributeStatement;
import com.sun.identity.saml2.assertion.Assertion;
import com.sun.identity.saml2.assertion.EncryptedAttribute;
import com.sun.identity.saml2.assertion.EncryptedID;
import com.sun.identity.saml2.assertion.NameID;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.saml2.common.SAML2Utils;
import com.sun.identity.saml2.key.KeyUtil;

/**
 * This class <code>DefaultLibrarySPAccountMapper</code> is the default 
 * implementation of the <code>SPAccountMapper</code> that is used
 * to map the <code>SAML</code> protocol objects to the user accounts.
 * at the <code>ServiceProvider</code> side of SAML v2 plugin.
 * Custom implementations may extend from this class to override some
 * of these implementations if they choose to do so.
 */
public class DefaultLibrarySPAccountMapper extends DefaultAccountMapper 
       implements SPAccountMapper {

    private PrivateKey decryptionKey = null;

     /**
      * Default constructor
      */
     public DefaultLibrarySPAccountMapper() {
         debug.message("DefaultLibrarySPAccountMapper.constructor: ");
         role = SP;
     }

    /**
     * Returns the user's disntinguished name or the universal ID for the 
     * corresponding  <code>SAML</code> <code>Assertion</code>. This method
     * will be invoked by the <code>SAML</code> framework while processing
     * the <code>Assertion</code> and retrieves the identity information. 
     * The implementation of this method first checks if the nameid format 
     * is transient and returns the transient user. Otherwise it checks for
     * the user for the corresponding name identifier in the assertion.
     * If not found, then it will check if this is an auto federation case. 
     *
     * @param assertion <code>SAML</code> <code>Assertion</code> that needs
     *        to be mapped to the user.
     * @param hostEntityID <code>EntityID</code> of the hosted provider.
     * @param realm realm or the organization name that may be used to find
     *        the user information.
     * @return user's disntinguished name or the universal ID.
     * @exception SAML2Exception if any failure.
     */
    public String getIdentity(
        Assertion assertion,
        String hostEntityID,
        String realm
    ) throws SAML2Exception {

        if(assertion == null) {
           throw new SAML2Exception(bundle.getString(
                 "nullAssertion"));
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
        EncryptedID encryptedID = assertion.getSubject().getEncryptedID();

        if(encryptedID != null) {
            decryptionKey = KeyUtil.getDecryptionKey(
                SAML2Utils.getSAML2MetaManager().
                getSPSSOConfig(realm, hostEntityID));
            nameID = encryptedID.decrypt(decryptionKey);
        } else {
            nameID = assertion.getSubject().getNameID();
        }
 
        String userID = null;
        String format = nameID.getFormat();
        boolean transientFormat = false;
        if(format != null && 
               format.equals(SAML2Constants.NAMEID_TRANSIENT_FORMAT)) {
           transientFormat = true;
           userID = getTransientUser(realm, hostEntityID);
        }
     

        if((userID != null) && (userID.length() != 0)) {
           return  userID;
        }
        
        if(!transientFormat) {
            String remoteEntityID = assertion.getIssuer().getValue();
            if (debug.messageEnabled()) {
                 debug.message(
                    "DefaultLibrarySPAccountMapper.getIdentity(Assertion):" +
                    " realm = " + realm + " hostEntityID = " + hostEntityID);  
            }
  
            try {
                userID = dsProvider.getUserID(realm, SAML2Utils.getNameIDKeyMap(
                    nameID, hostEntityID, remoteEntityID, realm, role));

            } catch(DataStoreProviderException dse) {
                debug.error(
                    "DefaultLibrarySPAccountMapper.getIdentity(Assertion): " +
                    "DataStoreProviderException", dse);
                throw new SAML2Exception(dse.getMessage());
            }
            if (userID != null) {
                return userID;
            }
        }

        // Check if this is an auto federation case.
        userID = getAutoFedUser(realm, hostEntityID, assertion, nameID.getValue());
        if ((userID != null) && (userID.length() != 0)) {
            return userID;
        } else {
            if (useNameIDAsSPUserID(realm, hostEntityID) && ! isAutoFedEnabled(realm, hostEntityID)) {
                if (debug.messageEnabled()) {
                     debug.message("DefaultLibrarySPAccountMapper.getIdentity:"
                         + " use NameID value as userID: " + nameID.getValue());
                }
                return nameID.getValue();
            } else {
                return null;
            }
        }
    }

    /**
     * Returns the transient user configured in the hosted entity 
     * configuration.
     * @param realm realm name for the given entity.
     * @param entityID hosted <code>EntityID</code>.
     * @return the transient user id configured in entity configuration.
     *         null if not configured or failed for any reason.
     */ 
    protected String getTransientUser(String realm, String entityID) {

        return getAttribute(realm, entityID,
                 SAML2Constants.TRANSIENT_FED_USER);
    }

    private boolean useNameIDAsSPUserID(String realm, String entityID) {
        return Boolean.valueOf(getAttribute(realm, entityID, SAML2Constants.USE_NAMEID_AS_SP_USERID));
    }

    private boolean isAutoFedEnabled(String realm, String entityID) {
        return Boolean.valueOf(getAttribute(realm, entityID, SAML2Constants.AUTO_FED_ENABLED));
    }

    /**
     * Returns user for the auto federate attribute.
     *
     * @param realm realm name.
     * @param entityID hosted <code>EntityID</code>.
     * @param assertion <code>Assertion</code> from the identity provider.
     * @return auto federation mapped user from the assertion
     *         auto federation <code>AttributeStatement</code>.
     *         null if the statement does not have the auto federation 
     *         attribute.
     */ 
    protected String getAutoFedUser(String realm, String entityID, Assertion assertion, String decryptedNameID)
            throws SAML2Exception {

        List<AttributeStatement> attributeStatements = assertion.getAttributeStatements();
        if(attributeStatements == null || attributeStatements.isEmpty()) {
           if(debug.messageEnabled()) { 
              debug.message("DefaultLibrarySPAccountMapper.getAutoFedUser: " +
              "Assertion does not have attribute statements.");
           }
           return null;
        }

        if (!isAutoFedEnabled(realm, entityID)) {
            if (debug.messageEnabled()) {
                debug.message("DefaultLibrarySPAccountMapper.getAutoFedUser: Auto federation is disabled.");
            }
            return null;
        }
       
        String autoFedAttribute = getAttribute(realm, entityID,
               SAML2Constants.AUTO_FED_ATTRIBUTE);

        if(autoFedAttribute == null || autoFedAttribute.length() == 0) {
           if(debug.messageEnabled()) { 
              debug.message("DefaultLibrarySPAccountMapper.getAutoFedUser: " +
              "Auto federation attribute is not configured.");
           }
           return null;
        }
        
        Set<String> autoFedAttributeValue = null;
        for (AttributeStatement statement : attributeStatements) {
            autoFedAttributeValue = getAttribute(statement, autoFedAttribute, realm, entityID);
            if (autoFedAttributeValue != null && !autoFedAttributeValue.isEmpty()) {
                break;
            }
        }

        if (autoFedAttributeValue == null || autoFedAttributeValue.isEmpty()) {
            if (debug.messageEnabled()) {
                debug.message("DefaultLibrarySPAccountMapper.getAutoFedUser: Auto federation attribute is not specified "
                        + "as an attribute.");
            }
            if (!useNameIDAsSPUserID(realm, entityID)) {
                return null;
            } else {
                if (debug.messageEnabled()) {
                    debug.message("DefaultLibrarySPAccountMapper.getAutoFedUser: Trying now to autofederate with nameID, "
                            + "nameID =" + decryptedNameID);
                }
                autoFedAttributeValue = new HashSet<String>(1);
                autoFedAttributeValue.add(decryptedNameID);
            }
        }

        DefaultSPAttributeMapper attributeMapper = 
                   new DefaultSPAttributeMapper();
        Map attributeMap = attributeMapper.getConfigAttributeMap(
                   realm, entityID, SP);
        if(attributeMap == null || attributeMap.isEmpty()) {
           if(debug.messageEnabled()) {
              debug.message("DefaultLibrarySPAccountMapper.getAutoFedUser: " +
              "attribute map is not configured.");
           }
        }

        String autoFedMapAttribute = (String)attributeMap.get(autoFedAttribute);

        if(autoFedMapAttribute == null) {
           if(debug.messageEnabled()) {
              debug.message("DefaultLibrarySPAccountMapper.getAutoFedUser: " +
              "Auto federation attribute map is not specified in config.");
           }
           // assume it is the same as the auto fed attribute name 
           autoFedMapAttribute = autoFedAttribute;
        }

        try {
            Map map = new HashMap();
            map.put(autoFedMapAttribute, autoFedAttributeValue);

            if(debug.messageEnabled()) {
               debug.message("DefaultLibrarySPAccountMapper.getAutoFedUser: " +
               "Search map: " + map);
            }

            String userId = dsProvider.getUserID(realm, map); 
            if (userId != null && userId.length() != 0) {
                return userId;
            } else {
                // check dynamic profile creation or ignore profile, if enabled,
                // return auto-federation attribute value as uid 
                if (isDynamicalOrIgnoredProfile(realm)) {
                    if(debug.messageEnabled()) {
                        debug.message(
                            "DefaultLibrarySPAccountMapper: dynamical user " +
                            "creation or ignore profile enabled : uid=" 
                            + autoFedAttributeValue); 
                    }
                    // return the first value as uid
                    return (String) autoFedAttributeValue.
                           iterator().next();
                }
            } 
        } catch (DataStoreProviderException dse) {

            if(debug.warningEnabled()) {
               debug.warning("DefaultLibrarySPAccountMapper.getAutoFedUser: " +
               "Datastore provider exception", dse);
            }
        }
        return null;

    }

    /**
     * Checks if dynamical profile creation or ignore profile is enabled.
     * @param realm realm to check the dynamical profile creation attributes.
     * @return true if dynamical profile creation or ignore profile is enabled,
     * false otherwise.
     */
    protected boolean isDynamicalOrIgnoredProfile(String realm) {
        return true;
    }

    /**
     * Returns the attribute name.
     */
    private Set getAttribute(
                AttributeStatement statement,
                String attributeName,
                String realm,
                String hostEntityID)
    {

        if (debug.messageEnabled()) {
            debug.message(
                "DefaultLibrarySPAccountMapper.getAttribute: attribute" +
                "Name =" + attributeName);
        }

        // check it if the attribute needs to be encrypted?
        List list = statement.getAttribute();
        List encList = statement.getEncryptedAttribute();
        if (encList != null && encList.size() != 0) {
            // a new list to hold the union of clear and encrypted attributes
            List allList = new ArrayList();
            if (list != null && !list.isEmpty()) {
                allList.addAll(list);
            }
            list = allList;
            for (Iterator encIter = encList.iterator(); encIter.hasNext();) {
                try {
                    if (decryptionKey == null) {
                        decryptionKey = KeyUtil.getDecryptionKey(
                            SAML2Utils.getSAML2MetaManager().
                                        getSPSSOConfig(realm, hostEntityID));
                    }
                    list.add(((EncryptedAttribute) encIter.next()).
                                        decrypt(decryptionKey));
                } catch (SAML2Exception se) {
                    debug.error("Decryption error:", se);
                    return null;
                }
            }
        }

        for(Iterator iter=list.iterator(); iter.hasNext();) {
            Attribute attribute = (Attribute)iter.next();
            if(!attributeName.equalsIgnoreCase(attribute.getName())) {
               continue;
            }

            List values = attribute.getAttributeValueString();
            if(values == null || values.size() == 0) {
               return null;
            }
            Set set = new HashSet();
            set.addAll(values); 
            return set; 
        }
        return null;
    }
}
