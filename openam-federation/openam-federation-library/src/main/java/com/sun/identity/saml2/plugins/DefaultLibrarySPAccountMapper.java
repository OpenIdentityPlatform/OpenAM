/*
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
 * Portions Copyrighted 2013-2015 ForgeRock AS.
 */
package com.sun.identity.saml2.plugins;

import java.security.Key;
import java.security.PrivateKey;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.List;

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
import org.forgerock.openam.utils.CollectionUtils;
import org.forgerock.openam.utils.StringUtils;

/**
 * This class <code>DefaultLibrarySPAccountMapper</code> is the default implementation of the
 * <code>SPAccountMapper</code> that is used to map the <code>SAML</code> protocol objects to the user accounts at the
 * <code>ServiceProvider</code> side of SAML v2 plugin.
 * Custom implementations may extend from this class to override some of these implementations if they choose to do so.
 */
public class DefaultLibrarySPAccountMapper extends DefaultAccountMapper implements SPAccountMapper {

     /**
      * Default constructor
      */
     public DefaultLibrarySPAccountMapper() {
         debug.message("DefaultLibrarySPAccountMapper.constructor: ");
         role = SP;
     }

    /**
     * Returns the user's distinguished name or the universal ID for the corresponding <code>SAML Assertion</code>. This
     * method will be invoked by the <code>SAML</code> framework while processing the <code>Assertion</code> and
     * retrieves the identity information.
     * The implementation of this method first checks if the NameID-Format is transient and returns the transient user.
     * Otherwise it checks for the user for the corresponding name identifier in the assertion.
     * If not found, then it will check if this is an auto federation case. 
     *
     * @param assertion <code>SAML Assertion</code> that needs to be mapped to the user.
     * @param hostEntityID <code>EntityID</code> of the hosted provider.
     * @param realm Realm or the organization name that may be used to find the user information.
     * @return User's distinguished name or the universal ID.
     * @throws SAML2Exception If there was any failure.
     */
    @Override
    public String getIdentity(Assertion assertion, String hostEntityID, String realm) throws SAML2Exception {
        if (assertion == null) {
            throw new SAML2Exception(bundle.getString("nullAssertion"));
        }

        if (hostEntityID == null) {
            throw new SAML2Exception(bundle.getString("nullHostEntityID"));
        }
        
        if (realm == null) {
            throw new SAML2Exception(bundle.getString("nullRealm"));
        }

        NameID nameID;
        EncryptedID encryptedID = assertion.getSubject().getEncryptedID();

        Set<PrivateKey> decryptionKeys = null;
        if (encryptedID != null) {
            decryptionKeys = KeyUtil.getDecryptionKeys(getSSOConfig(realm, hostEntityID));
            nameID = encryptedID.decrypt(decryptionKeys);
        } else {
            nameID = assertion.getSubject().getNameID();
        }
 
        String userID = null;
        String format = nameID.getFormat();
        boolean isTransient = SAML2Constants.NAMEID_TRANSIENT_FORMAT.equals(format);
        if (isTransient) {
            userID = getTransientUser(realm, hostEntityID);
        }

        if (StringUtils.isNotEmpty(userID)) {
            return userID;
        }

        // Check if this is an auto federation case.
        userID = getAutoFedUser(realm, hostEntityID, assertion, nameID.getValue(), decryptionKeys);
        if (StringUtils.isNotEmpty(userID)) {
            return userID;
        } else {
            if (useNameIDAsSPUserID(realm, hostEntityID) && !isAutoFedEnabled(realm, hostEntityID)) {
                if (debug.messageEnabled()) {
                     debug.message("DefaultLibrarySPAccountMapper.getIdentity: use NameID value as userID: "
                             + nameID.getValue());
                }
                return nameID.getValue();
            } else {
                return null;
            }
        }
    }

    @Override
    public boolean shouldPersistNameIDFormat(String realm, String hostEntityID, String remoteEntityID,
            String nameIDFormat) {
        return !Boolean.parseBoolean(SAML2Utils.getAttributeValueFromSSOConfig(realm, hostEntityID,
                SAML2Constants.SP_ROLE, SAML2Constants.SP_DO_NOT_WRITE_FEDERATION_INFO));
    }

    /**
     * Returns the transient user configured in the hosted entity configuration.
     *
     * @param realm Realm name for the given entity.
     * @param entityID Hosted <code>EntityID</code>.
     * @return The transient user id configured in entity configuration, or null if not configured or failed for any
     * reason.
     */ 
    protected String getTransientUser(String realm, String entityID) {
        return getAttribute(realm, entityID, SAML2Constants.TRANSIENT_FED_USER);
    }

    private boolean useNameIDAsSPUserID(String realm, String entityID) {
        return Boolean.parseBoolean(getAttribute(realm, entityID, SAML2Constants.USE_NAMEID_AS_SP_USERID));
    }

    private boolean isAutoFedEnabled(String realm, String entityID) {
        return Boolean.parseBoolean(getAttribute(realm, entityID, SAML2Constants.AUTO_FED_ENABLED));
    }

    /**
     * Returns user for the auto federate attribute.
     *
     * @param realm Realm name.
     * @param entityID Hosted <code>EntityID</code>.
     * @param assertion <code>Assertion</code> from the identity provider.
     * @return Auto federation mapped user from the assertion auto federation <code>AttributeStatement</code>. if the
     * statement does not have the auto federation attribute then the NameID value will be used if use NameID as SP user
     * ID is enabled, otherwise null.
     */ 
    protected String getAutoFedUser(String realm, String entityID, Assertion assertion, String decryptedNameID,
            Set<PrivateKey> decryptionKeys) throws SAML2Exception {
        if (!isAutoFedEnabled(realm, entityID)) {
            if (debug.messageEnabled()) {
                debug.message("DefaultLibrarySPAccountMapper.getAutoFedUser: Auto federation is disabled.");
            }
            return null;
        }

        String autoFedAttribute = getAttribute(realm, entityID, SAML2Constants.AUTO_FED_ATTRIBUTE);
        if (autoFedAttribute == null || autoFedAttribute.isEmpty()) {
            debug.error("DefaultLibrarySPAccountMapper.getAutoFedUser: " +
                        "Auto federation is enabled but the auto federation attribute is not configured.");
            return null;
        }

        if (debug.messageEnabled()) {
            debug.message("DefaultLibrarySPAccountMapper.getAutoFedUser: Auto federation attribute is set to: "
                    + autoFedAttribute);
        }

        Set<String> autoFedAttributeValue = null;
        List<AttributeStatement> attributeStatements = assertion.getAttributeStatements();
        if (attributeStatements == null || attributeStatements.isEmpty()) {
            if (debug.messageEnabled()) {
                debug.message("DefaultLibrarySPAccountMapper.getAutoFedUser: " +
                        "Assertion does not have any attribute statements.");
            }
        } else {
            for (AttributeStatement statement : attributeStatements) {
                autoFedAttributeValue = getAttribute(statement, autoFedAttribute, decryptionKeys);
                if (autoFedAttributeValue != null && !autoFedAttributeValue.isEmpty()) {
                    if (debug.messageEnabled()) {
                        debug.message("DefaultLibrarySPAccountMapper.getAutoFedUser: " +
                                "Found auto federation attribute value in Assertion: " + autoFedAttributeValue);
                    }
                    break;
                }
            }
        }

        if (autoFedAttributeValue == null || autoFedAttributeValue.isEmpty()) {
            if (debug.messageEnabled()) {
                debug.message("DefaultLibrarySPAccountMapper.getAutoFedUser: Auto federation attribute is not specified"
                        + " as an attribute.");
            }
            if (!useNameIDAsSPUserID(realm, entityID)) {
                if (debug.messageEnabled()) {
                    debug.message("DefaultLibrarySPAccountMapper.getAutoFedUser: NameID as SP UserID was not enabled "
                            + " and auto federation attribute " + autoFedAttribute + " was not found in the Assertion");
                }
                return null;
            } else {
                if (debug.messageEnabled()) {
                    debug.message("DefaultLibrarySPAccountMapper.getAutoFedUser: Trying now to autofederate with nameID"
                            + ", nameID =" + decryptedNameID);
                }
                autoFedAttributeValue = CollectionUtils.asSet(decryptedNameID);
            }
        }

        String autoFedMapAttribute = null;
        DefaultSPAttributeMapper attributeMapper = new DefaultSPAttributeMapper();
        Map<String, String> attributeMap = attributeMapper.getConfigAttributeMap(realm, entityID, SP);
        if (attributeMap == null || attributeMap.isEmpty()) {
           if(debug.messageEnabled()) {
              debug.message("DefaultLibrarySPAccountMapper.getAutoFedUser: attribute map is not configured.");
           }
        } else {
            autoFedMapAttribute = attributeMap.get(autoFedAttribute);
        }

        if (autoFedMapAttribute == null) {
           if (debug.messageEnabled()) {
               debug.message("DefaultLibrarySPAccountMapper.getAutoFedUser: " +
                       "Auto federation attribute map is not specified in config.");
           }
           // assume it is the same as the auto fed attribute name 
           autoFedMapAttribute = autoFedAttribute;
        }

        try {
            Map<String, Set<String>> map = new HashMap<>(1);
            map.put(autoFedMapAttribute, autoFedAttributeValue);

            if (debug.messageEnabled()) {
                debug.message("DefaultLibrarySPAccountMapper.getAutoFedUser: Search map: " + map);
            }

            String userId = dsProvider.getUserID(realm, map);
            if (userId != null && !userId.isEmpty()) {
                return userId;
            } else {
                // check dynamic profile creation or ignore profile, if enabled,
                // return auto-federation attribute value as uid 
                if (isDynamicalOrIgnoredProfile(realm)) {
                    if (debug.messageEnabled()) {
                        debug.message("DefaultLibrarySPAccountMapper: dynamical user creation or ignore profile " +
                                "enabled : uid=" + autoFedAttributeValue);
                    }
                    // return the first value as uid
                    return autoFedAttributeValue.iterator().next();
                }
            } 
        } catch (DataStoreProviderException dse) {
            if (debug.warningEnabled()) {
                debug.warning("DefaultLibrarySPAccountMapper.getAutoFedUser: Datastore provider exception", dse);
            }
        }

        return null;
    }

    /**
     * Checks if dynamical profile creation or ignore profile is enabled.
     *
     * @param realm Realm to check the dynamical profile creation attributes.
     * @return <code>true</code> if dynamical profile creation or ignore profile is enabled, <code>false</code>
     * otherwise.
     */
    protected boolean isDynamicalOrIgnoredProfile(String realm) {
        return true;
    }

    private Set<String> getAttribute(AttributeStatement statement, String attributeName,
            Set<PrivateKey> decryptionKeys) {
        if (debug.messageEnabled()) {
            debug.message("DefaultLibrarySPAccountMapper.getAttribute: attribute Name =" + attributeName);
        }

        // check it if the attribute needs to be encrypted?
        List<Attribute> list = statement.getAttribute();
        List<EncryptedAttribute> encList = statement.getEncryptedAttribute();
        if (encList != null && !encList.isEmpty()) {
            // a new list to hold the union of clear and encrypted attributes
            List<Attribute> allList = new ArrayList<>();
            if (list != null) {
                allList.addAll(list);
            }
            list = allList;
            for (EncryptedAttribute encryptedAttribute : encList) {
                try {
                    list.add(encryptedAttribute.decrypt(decryptionKeys));
                } catch (SAML2Exception se) {
                    debug.error("Decryption error:", se);
                    return null;
                }
            }
        }

        for (Attribute attribute : list) {
            if (!attributeName.equalsIgnoreCase(attribute.getName())) {
               continue;
            }

            List<String> values = attribute.getAttributeValueString();
            if (values == null || values.isEmpty()) {
               return null;
            }
            return new HashSet<>(values);
        }
        return null;
    }
}
