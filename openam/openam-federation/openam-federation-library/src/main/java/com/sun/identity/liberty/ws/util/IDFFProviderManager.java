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
 * $Id: IDFFProviderManager.java,v 1.3 2008/06/25 05:47:24 qcheng Exp $
 *
 */


package com.sun.identity.liberty.ws.util;

import java.security.Key;
import java.security.PrivateKey;
import java.util.List;
import java.util.Map;

import com.sun.identity.federation.common.IFSConstants;
import com.sun.identity.federation.jaxb.entityconfig.EntityConfigElement;
import com.sun.identity.federation.jaxb.entityconfig.BaseConfigType;
import com.sun.identity.federation.key.EncInfo;
import com.sun.identity.federation.key.KeyUtil;
import com.sun.identity.federation.meta.IDFFMetaException;
import com.sun.identity.federation.meta.IDFFMetaManager;
import com.sun.identity.federation.meta.IDFFMetaUtils;
import com.sun.identity.liberty.ws.meta.jaxb.ProviderDescriptorType;
import com.sun.identity.liberty.ws.meta.jaxb.SPDescriptorType;
import com.sun.identity.shared.debug.Debug;

/**
 * The class <code>IDFFProviderManager</code> retrieves properties consumed
 * by IDWSF from providers defined in IDFF.
 */
public class IDFFProviderManager implements ProviderManager {

    private static final String ROOT_REALM = "/";
    private static IDFFMetaManager idffMetaManager = null;

    static {
        try {
            idffMetaManager = new IDFFMetaManager(null);
        } catch (IDFFMetaException imex) {
            ProviderUtil.debug.error("IDFFProviderManager.static:", imex);
        }
    }

    /**
     * Returns whether the specified provider exists or not.
     * @param providerID provider ID.
     * @return true if the specified provider exists, false if it doesn't
     *     exist.
     */
    public boolean containsProvider(String providerID) {
        SPDescriptorType spDescriptor = null;
        try {
            spDescriptor = idffMetaManager.getSPDescriptor(
                ROOT_REALM, providerID);
        } catch (IDFFMetaException imex) {
            ProviderUtil.debug.error(
                "IDFFProviderManager.containsProvider:", imex);
        }

        return (spDescriptor != null);
    }

    /**
     * Returns whether the specified provider is a member of the specified
     * affiliation or not.
     * @param providerID provider ID.
     * @return true if the specified provider is a member of the specified
     *     affiliation, false if it is not.
     */
    public boolean isAffiliationMember(String providerID, String affID) {

        try {
            return idffMetaManager.isAffiliateMember(
                ROOT_REALM, providerID, affID);
        } catch (IDFFMetaException imex) {
            ProviderUtil.debug.error(
                "IDFFProviderManager.isAffiliationMember:", imex);
        }

        return false;
    }

    /**
     * Returns whether the specified provider requires name ID encryption
     * or not.
     * @param providerID provider ID.
     * @return true if the specified provider requires name ID encryption,
     *     false if it doesn't.
     */
    public boolean isNameIDEncryptionEnabled(String providerID) {
        EntityConfigElement entityConfig = null;
        try {
            entityConfig = idffMetaManager.getEntityConfig(
                ROOT_REALM, providerID);
        } catch (IDFFMetaException imex) {
            ProviderUtil.debug.error(
                "IDFFProviderManager.isNameIDEncryptionEnabled:", imex);
        }

        if (entityConfig == null) {
            return false;
        }

        BaseConfigType baseConfig =
            IDFFMetaUtils.getSPDescriptorConfig(entityConfig);
        if (baseConfig == null) {
            baseConfig = IDFFMetaUtils.getIDPDescriptorConfig(entityConfig);
            if (baseConfig == null) {
                return false;
            }
        }

        Map attrMap = IDFFMetaUtils.getAttributes(baseConfig);
        if ((attrMap == null) || (attrMap.isEmpty())) {
            return false;
        }

        List values = (List)attrMap.get(IFSConstants.ENABLE_NAMEID_ENCRYPTION);
        if ((values == null) || values.isEmpty()) {
            return false;
        }

        return ((String)values.get(0)).equalsIgnoreCase("true");
    }

    /**
     * Gets encryption key for specified provider.
     * @param providerID provider ID.
     * @return encryption key for specified provider.
     */
    public Key getEncryptionKey(String providerID) {
        EncInfo encInfo = getEncInfo(providerID);
        return (encInfo == null ? null : encInfo.getWrappingKey());
    }

    /**
     * Gets encryption key strength for specified provider.
     * @param providerID provider ID.
     * @return encryption key strength for specified provider.
     */
    public int getEncryptionKeyStrength(String providerID) {
        EncInfo encInfo = getEncInfo(providerID);
        return (encInfo == null ? 0 : encInfo.getDataEncStrength());
    }

    /**
     * Gets encryption key algorithm for specified provider.
     * @param providerID provider ID.
     * @return encryption key method for specified provider.
     */
    public String getEncryptionKeyAlgorithm(String providerID) {
        EncInfo encInfo = getEncInfo(providerID);
        return (encInfo == null ? null : encInfo.getDataEncAlgorithm());
    }

    /**
     * Gets decryption key for specified provider.
     * @param providerID provider ID.
     * @return decryption key for specified provider.
     */
    public PrivateKey getDecryptionKey(String providerID) {
        BaseConfigType providerConfig = null;
        try {
            providerConfig = idffMetaManager.getSPDescriptorConfig(
                ROOT_REALM, providerID);
            if (providerConfig == null) {
                providerConfig = idffMetaManager.
                    getIDPDescriptorConfig(ROOT_REALM, providerID);
            }
        } catch (IDFFMetaException imex) {
            ProviderUtil.debug.error("IDFFProviderManager.getDecryptionKey",
                imex);
        }
        
        if (providerConfig == null) {
            return null;
        }

        return KeyUtil.getDecryptionKey(providerConfig);
    }

    /**
     * Gets signing certificate alias for specified provider.
     * @param providerID provider ID.
     * @return signing certificate alias for specified provider.
     */
    public String getSigningKeyAlias(String providerID) {
        BaseConfigType config = null;
        try {
            config = idffMetaManager.getSPDescriptorConfig(
                ROOT_REALM, providerID);
            if (config == null) {
                config = idffMetaManager.getIDPDescriptorConfig(
                    ROOT_REALM, providerID);
            }
        } catch(IDFFMetaException imex) {
            ProviderUtil.debug.error(
                "IDFFProviderManager.getSigningCertificate:", imex);
        }

        if (config == null) {
            if (ProviderUtil.debug.messageEnabled()) {
                ProviderUtil.debug.message(
                    "IDFFProviderManager.getSigningKeyAlias:" +
                    "config not found.");
            }
            return null;
        }

        return KeyUtil.getSigningCertAlias(config);
    }

    private EncInfo getEncInfo(String providerID) {
        ProviderDescriptorType providerDesc = null;
        try {
            providerDesc = idffMetaManager.getSPDescriptor(
                ROOT_REALM, providerID);
            if (providerDesc == null) {
                providerDesc = idffMetaManager.getIDPDescriptor(
                    ROOT_REALM, providerID);
            }
            if (providerDesc == null) {
                return null;
            }
        } catch (IDFFMetaException imex) {
            ProviderUtil.debug.error("IDFFProviderManager.getEncInfo:",
                imex);
            return null;
        }

        if (providerDesc == null) {
            if (ProviderUtil.debug.messageEnabled()) {
                ProviderUtil.debug.message(
                    "IDFFProviderManager.getEncInfo: Descriptor not found.");
            }
            return null;
        }

        return KeyUtil.getEncInfo(providerDesc, providerID, false);
    }
}
