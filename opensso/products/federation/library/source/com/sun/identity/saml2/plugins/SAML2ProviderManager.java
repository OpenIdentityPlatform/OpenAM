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
 * $Id: SAML2ProviderManager.java,v 1.3 2008/06/25 05:47:52 qcheng Exp $
 *
 */

package com.sun.identity.saml2.plugins;

import java.security.Key;
import java.security.PrivateKey;
import java.util.List;
import java.util.Map;

import com.sun.identity.liberty.ws.util.ProviderManager;
import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.saml2.common.SAML2Utils;
import com.sun.identity.saml2.jaxb.entityconfig.BaseConfigType;
import com.sun.identity.saml2.jaxb.metadata.EntityDescriptorElement;
import com.sun.identity.saml2.jaxb.metadata.SSODescriptorType;
import com.sun.identity.saml2.key.EncInfo;
import com.sun.identity.saml2.key.KeyUtil;
import com.sun.identity.saml2.meta.SAML2MetaException;
import com.sun.identity.saml2.meta.SAML2MetaManager;
import com.sun.identity.saml2.meta.SAML2MetaUtils;

/**
 * The class <code>SAML2ProviderManager</code> retrieves properties consumed
 * by IDWSF from providers defined in SAML2.
 */
public class SAML2ProviderManager implements ProviderManager {
    private static SAML2MetaManager metaManager =
        SAML2Utils.getSAML2MetaManager();

    /**
     * Returns whether the specified provider exists or not.
     * @param providerID provider ID.
     * @return true if the specified provider exists, false if it doesn't
     *     exist.
     */
    public boolean containsProvider(String providerID) {
        EntityDescriptorElement ed = null;
        try {
            ed = metaManager.getEntityDescriptor("/", providerID);
        } catch (SAML2MetaException smex) {
            SAML2Utils.debug.error(
                "SAML2ProviderManager.containsProvider:", smex);
        }

        return (ed != null);
    }

    /**
     * Returns whether the specified provider is a member of the specified
     * affiliation or not.
     * @param providerID provider ID.
     * @return true if the specified provider is a member of the specified
     *     affiliation, false if it is not.
     */
    public boolean isAffiliationMember(String providerID, String affID) {
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
        BaseConfigType config = null;
        try {
            config = metaManager.getSPSSOConfig("/", providerID);
            if (config == null) {
                config = metaManager.getIDPSSOConfig("/", providerID);
            }
        } catch (SAML2MetaException smex) {
            SAML2Utils.debug.error(
                "SAML2ProviderManager.isNameIDEncryptionEnabled:", smex);
        }

        if (config == null) {
            if (SAML2Utils.debug.messageEnabled()) {
                SAML2Utils.debug.message(
                    "SAML2ProviderManager.isNameIDEncryptionEnabled:" +
                    "config not found.");
            }
            return false;
        }

        String wantEncrypted = null;
        Map attrMap = SAML2MetaUtils.getAttributes(config);
        if ((attrMap != null) && !attrMap.isEmpty()) {
            List values =
                (List)attrMap.get(SAML2Constants.WANT_NAMEID_ENCRYPTED);

            if ((values != null) && (!values.isEmpty())) {
                wantEncrypted = (String)values.get(0);
            }
        }

        return ((wantEncrypted != null) &&
            wantEncrypted.equalsIgnoreCase("true"));
    }

    /**
     * Gets encryption certificate alias for specified provider.
     * @param providerID provider ID.
     * @return encryption certificate alias for specified provider.
     */
    public Key getEncryptionKey(String providerID) {
        EncInfo encInfo = getEncInfo(providerID);
        return (encInfo == null ? null : encInfo.getWrappingKey());
    }

    /**
     * Gets encryption key size for specified provider.
     * @param providerID provider ID.
     * @return encryption key size for specified provider.
     */
    public int getEncryptionKeyStrength(String providerID) {
        EncInfo encInfo = getEncInfo(providerID);
        return (encInfo == null ? 0 : encInfo.getDataEncStrength());
    }

    /**
     * Gets encryption key method for specified provider.
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
            providerConfig = metaManager.getSPSSOConfig("/", providerID);
            if (providerConfig == null) {
                providerConfig = metaManager.getIDPSSOConfig("/", providerID);
            }
        } catch (SAML2MetaException smex) {
            SAML2Utils.debug.error("SAML2ProviderManager.getDecryptionKey",
                smex);
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
            config = metaManager.getSPSSOConfig("/", providerID);
            if (config == null) {
                config = metaManager.getIDPSSOConfig("/", providerID);
            }
        } catch (SAML2MetaException smex) {
            SAML2Utils.debug.error(
                "SAML2ProviderManager.getSigningKeyAlias:", smex);
        }

        if (config == null) {
            if (SAML2Utils.debug.messageEnabled()) {
                SAML2Utils.debug.message(
                    "SAML2ProviderManager.getSigningKeyAlias:" +
                    "config not found.");
            }
            return null;
        }

        return KeyUtil.getSigningCertAlias(config);

    }

    private EncInfo getEncInfo(String providerID) {
        SSODescriptorType ssod = null;
        try {
            ssod = metaManager.getSPSSODescriptor("/", providerID);
            if (ssod == null) {
                ssod = metaManager.getIDPSSODescriptor("/", providerID);
            }
        } catch (SAML2MetaException smex) {
            SAML2Utils.debug.error(
                "SAML2ProviderManager.getEncInfo:", smex);
        }

        if (ssod == null) {
            if (SAML2Utils.debug.messageEnabled()) {
                SAML2Utils.debug.message(
                    "SAML2ProviderManager.getEncInfo: Descriptor not found.");
            }
            return null;
        }

        return KeyUtil.getEncInfo(ssod, providerID, SAML2Constants.SP_ROLE);
    }
}
