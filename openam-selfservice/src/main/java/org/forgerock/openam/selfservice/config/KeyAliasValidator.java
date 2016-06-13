/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2016 ForgeRock AS.
 */

package org.forgerock.openam.selfservice.config;

import static org.forgerock.openam.utils.CollectionUtils.getFirstItem;
import static org.forgerock.openam.utils.StringUtils.isBlank;

import java.util.Set;

import org.forgerock.openam.utils.AMKeyProvider;

import com.iplanet.am.util.SystemProperties;
import com.sun.identity.saml.xmlsig.KeyProvider;
import com.sun.identity.shared.Constants;
import com.sun.identity.sm.ServiceAttributeValidator;

/**
 * Validates that a key alias is specified and that it actually exists in the
 * configured key store.
 *
 * @since 13.5.0
 */
public final class KeyAliasValidator implements ServiceAttributeValidator {

    @Override
    public boolean validate(Set<String> values) {
        if (SystemProperties.getAsBoolean(Constants.SYS_PROPERTY_INSTALL_TIME, false)) {
            // Keystore is unlikely to be available during installation.
            return true;
        }

        String keyAlias = getFirstItem(values);

        if (isBlank(keyAlias)) {
            return false;
        }

        KeyProvider keyProvider = new AMKeyProvider();
        return keyProvider.containsKey(keyAlias);
    }

}
