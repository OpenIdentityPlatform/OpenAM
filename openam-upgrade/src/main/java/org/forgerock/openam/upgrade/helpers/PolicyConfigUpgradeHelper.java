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

import static com.sun.identity.policy.PolicyConfig.*;
import com.sun.identity.sm.AbstractUpgradeHelper;
import com.sun.identity.sm.AttributeSchemaImpl;
import org.forgerock.openam.upgrade.UpgradeException;

/**
 * This helper is used to hide deprecated settings from the global policy configuration ui on upgrade of the product.
 *
 * @since 12.0.0
 */
public class PolicyConfigUpgradeHelper extends AbstractUpgradeHelper {

    public PolicyConfigUpgradeHelper() {
        attributes.add(ADVICES_HANDLEABLE_BY_AM);
        attributes.add(LDAP_BASE_DN);
        attributes.add(LDAP_ORG_SEARCH_SCOPE);
        attributes.add(LDAP_GROUP_SEARCH_FILTER);
        attributes.add(LDAP_GROUP_SEARCH_SCOPE);
        attributes.add(LDAP_ROLES_SEARCH_FILTER);
        attributes.add(LDAP_ROLES_SEARCH_SCOPE);
        attributes.add(LDAP_ORG_SEARCH_ATTRIBUTE);
        attributes.add(LDAP_GROUP_SEARCH_ATTRIBUTE);
        attributes.add(LDAP_ROLES_SEARCH_ATTRIBUTE);
        attributes.add(SELECTED_SUBJECTS);
        attributes.add(SELECTED_CONDITIONS);
        attributes.add(SELECTED_REFERRALS);
        attributes.add(SELECTED_RESPONSE_PROVIDERS);
        attributes.add(SELECTED_DYNAMIC_ATTRIBUTES);
    }

    @Override
    public AttributeSchemaImpl upgradeAttribute(AttributeSchemaImpl oldAttr, AttributeSchemaImpl newAttr)
            throws UpgradeException {

        if (oldAttr.getI18NKey() == null || oldAttr.getI18NKey().isEmpty()) {
            // If the I18N key is "" then the setting is already hidden from the UI; no further action required.
            return null;

        } else {
            // We want to hide these settings from the UI by setting their I18N key to ""; unfortunately,
            // it's not possible to simply set the i18N key so we need to copy the values of oldAttr to
            // newAttr instead.
            newAttr = updateDefaultValues(newAttr, oldAttr.getDefaultValues());
            return newAttr;
        }
    }
}
