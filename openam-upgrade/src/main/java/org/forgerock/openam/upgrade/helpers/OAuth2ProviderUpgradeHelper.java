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
 * Copyright 2014 ForgeRock AS.
 */
package org.forgerock.openam.upgrade.helpers;

import static org.forgerock.oauth2.core.OAuth2Constants.OAuth2ProviderService.*;
import static org.forgerock.openam.upgrade.steps.UpgradeOAuth2ProviderStep.*;

import com.sun.identity.sm.AbstractUpgradeHelper;
import com.sun.identity.sm.AttributeSchemaImpl;
import java.util.HashSet;
import java.util.Set;
import org.forgerock.json.jose.jws.JwsAlgorithm;
import org.forgerock.openam.upgrade.UpgradeException;

/**
 * This upgrade helper is used to add new default values to the OAuth2 Provider schema.
 *
 * @since 12.0.0
 */
public class OAuth2ProviderUpgradeHelper extends AbstractUpgradeHelper {

    public OAuth2ProviderUpgradeHelper() {
        attributes.add(ID_TOKEN_SIGNING_ALGORITHMS);
        attributes.add(JKWS_URI);
        attributes.add(SUPPORTED_CLAIMS);
        attributes.add(OIDC_CLAIMS_EXTENSION_SCRIPT);
    }

    @Override
    public AttributeSchemaImpl upgradeAttribute(AttributeSchemaImpl oldAttr, AttributeSchemaImpl newAttr)
            throws UpgradeException {

        if (ID_TOKEN_SIGNING_ALGORITHMS.equals(newAttr.getName())) {
            final Set<String> oldAlgorithms = oldAttr.getDefaultValues();
            final Set<String> newAlgorithms = renameAlgorithms(oldAlgorithms);
            if (!newAlgorithms.contains(JwsAlgorithm.RS256.name())) {
                newAlgorithms.add(JwsAlgorithm.RS256.name());
            }
            if (!oldAlgorithms.equals(newAlgorithms)) {
                return updateDefaultValues(oldAttr, newAlgorithms);
            }
        } else if (JKWS_URI.equals(newAttr.getName()) && !oldAttr.getType().equals(newAttr.getType())) {
            return newAttr;
        } else if (SUPPORTED_CLAIMS.equals(newAttr.getName())) {
            final Set<String> oldClaims = oldAttr.getDefaultValues();
            final Set<String> newClaims = newAttr.getDefaultValues();
            if (!oldClaims.equals(newClaims)) {
                return updateDefaultValues(oldAttr, newAttr.getDefaultValues());
            }
        } else if (OIDC_CLAIMS_EXTENSION_SCRIPT.equals(newAttr.getName())) {
            if (!oldAttr.getDefaultValues().equals(newAttr.getDefaultValues())) {
                return updateDefaultValues(oldAttr, newAttr.getDefaultValues());
            }
        }

        return null;
    }

    private Set<String> renameAlgorithms(Set<String> oldAlgorithms) {
        final Set<String> newAlgorithms = new HashSet<String>();
        for (String algorithm : oldAlgorithms) {
            if (ALGORITHM_NAMES.containsKey(algorithm)) {
                newAlgorithms.add(ALGORITHM_NAMES.get(algorithm));
            } else {
                newAlgorithms.add(algorithm);
            }
        }
        return newAlgorithms;
    }

}
