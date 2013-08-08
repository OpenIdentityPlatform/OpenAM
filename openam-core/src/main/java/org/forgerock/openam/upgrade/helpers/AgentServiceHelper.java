/*
 * Copyright 2013 ForgeRock AS.
 *
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
 */
package org.forgerock.openam.upgrade.helpers;

import com.sun.identity.sm.AbstractUpgradeHelper;
import com.sun.identity.sm.AttributeSchema;
import com.sun.identity.sm.AttributeSchemaImpl;
import org.forgerock.openam.upgrade.UpgradeException;
import org.forgerock.openam.upgrade.steps.UpgradeOAuth2ClientStep;

/**
 * This service helper implementation ensures that the OAuth2 Client service attributes are using the correct uitype
 * in the upgraded system. As a secondary task this helper also upgrades the default policy cache mode settings for
 * both Java EE and Web Agents.
 *
 * @author Peter Major
 */
public class AgentServiceHelper extends AbstractUpgradeHelper {

    private static final String POLICY_CACHE_MODE = "com.sun.identity.policy.client.cacheMode";
    private static final String FETCH_FROM_ROOT = "com.sun.identity.agents.config.fetch.from.root.resource";

    public AgentServiceHelper() {
        attributes.addAll(UpgradeOAuth2ClientStep.CHANGED_PROPERTIES);
        attributes.add(POLICY_CACHE_MODE);
        attributes.add(FETCH_FROM_ROOT);
    }

    @Override
    public AttributeSchemaImpl upgradeAttribute(AttributeSchemaImpl oldAttr, AttributeSchemaImpl newAttr)
            throws UpgradeException {
        if (UpgradeOAuth2ClientStep.CHANGED_PROPERTIES.contains(newAttr.getName())) {
            //we only want to upgrade the attributes if they don't have the unorderedlist uitype yet.
            if (AttributeSchema.UIType.UNORDEREDLIST.equals(oldAttr.getUIType())) {
                return null;
            }
        } else if ((POLICY_CACHE_MODE.equals(newAttr.getName()) && oldAttr.getDefaultValues().contains("self"))
                || (FETCH_FROM_ROOT.equals(newAttr.getName()) && oldAttr.getDefaultValues().contains("false"))) {
            //i.e. if the default value has been already updated we return null, to prevent upgrade for this attribute
            return null;
        }

        return newAttr;
    }
}
