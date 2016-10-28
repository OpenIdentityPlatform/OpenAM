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

package org.forgerock.openam.upgrade.helpers;

import static org.forgerock.openam.services.email.MailServerImpl.*;

import org.forgerock.openam.upgrade.UpgradeException;

import com.sun.identity.sm.AbstractUpgradeHelper;
import com.sun.identity.sm.AttributeSchemaImpl;

/**
 * This upgrade helper is used to upgrade the mail server schema.
 *
 * @since 14.0.0
 */
public class MailServiceUpgradeHelper extends AbstractUpgradeHelper {

    /**
     * Default Constructor
     */
    public MailServiceUpgradeHelper() {
        attributes.add(SMTP_HOSTNAME);
        attributes.add(SMTP_USERNAME);
        attributes.add(SMTP_USERPASSWORD);
        attributes.add(FROM_ADDRESS);
        attributes.add(SUBJECT);
        attributes.add(MESSAGE);
    }

    @Override
    public AttributeSchemaImpl upgradeAttribute(AttributeSchemaImpl attributeToUpgrade,
            AttributeSchemaImpl attributeFromNewSchema) throws UpgradeException {

        boolean attributeNeedUpgrade = false;
        switch (attributeFromNewSchema.getName()) {
            case SMTP_HOSTNAME:
            case SMTP_USERNAME:
            case SMTP_USERPASSWORD:
            case FROM_ADDRESS:
                if (!attributeToUpgrade.getExampleValues().equals(attributeFromNewSchema.getExampleValues())) {
                    attributeToUpgrade = updateExampleValues(attributeToUpgrade, attributeFromNewSchema.getExampleValues());
                    attributeNeedUpgrade = true;
                }
                if (!attributeToUpgrade.getDefaultValues().equals(attributeFromNewSchema.getDefaultValues())) {
                    attributeToUpgrade = updateDefaultValues(attributeToUpgrade, attributeFromNewSchema.getDefaultValues());
                    attributeNeedUpgrade = true;
                }
                break;
            case SUBJECT:
            case MESSAGE:
                if (attributeFromNewSchema.isOptional() && !attributeToUpgrade.isOptional()) {
                    attributeToUpgrade = updateOptional(attributeToUpgrade, attributeFromNewSchema.isOptional());
                    attributeNeedUpgrade = true;
                }
        }

        if (attributeNeedUpgrade) {
            return attributeToUpgrade;
        } else {
            return null;
        }
    }

}
