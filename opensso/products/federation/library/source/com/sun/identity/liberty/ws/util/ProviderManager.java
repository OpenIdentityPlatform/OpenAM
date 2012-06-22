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
 * $Id: ProviderManager.java,v 1.2 2008/06/25 05:47:24 qcheng Exp $
 *
 */

package com.sun.identity.liberty.ws.util;

import java.security.Key;
import java.security.PrivateKey;

/**
 * The interface <code>ProviderManager</code> is a provider interface for 
 * retrieving properties consumed by IDWSF from providers defined in
 * different specification. For example, IDFF and SAML2.
 */
public interface ProviderManager {
 
    /**
     * Returns whether the specified provider exists or not.
     * @param providerID provider ID.
     * @return true if the specified provider exists, false if it doesn't
     *     exist.
     */
    public boolean containsProvider(String providerID);

    /**
     * Returns whether the specified provider is a member of the specified
     * affiliation or not.
     * @param providerID provider ID.
     * @return true if the specified provider is a member of the specified
     *     affiliation, false if it is not.
     */
    public boolean isAffiliationMember(String providerID, String affID);

    /**
     * Returns whether the specified provider requires name ID encryption
     * or not.
     * @param providerID provider ID.
     * @return true if the specified provider requires name ID encryption,
     *     false if it doesn't.
     */
    public boolean isNameIDEncryptionEnabled(String providerID);

    /**
     * Gets encryption key for specified provider.
     * @param providerID provider ID.
     * @return encryption key for specified provider.
     */
    public Key getEncryptionKey(String providerID);

    /**
     * Gets encryption key strength for specified provider.
     * @param providerID provider ID.
     * @return encryption key strength for specified provider.
     */
    public int getEncryptionKeyStrength(String providerID);

    /**
     * Gets encryption key algorithm for specified provider.
     * @param providerID provider ID.
     * @return encryption key method for specified provider.
     */
    public String getEncryptionKeyAlgorithm(String providerID);

    /**
     * Gets decryption key for specified provider.
     * @param providerID provider ID.
     * @return decryption key for specified provider.
     */
    public PrivateKey getDecryptionKey(String providerID);

    /**
     * Gets signing certificate alias for specified provider.
     * @param providerID provider ID.
     * @return signing certificate alias for specified provider.
     */
    public String getSigningKeyAlias(String providerID);
}
