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

import com.sun.identity.idm.IdConstants;
import com.sun.identity.sm.AbstractUpgradeHelper;
import com.sun.identity.sm.AttributeSchemaImpl;
import org.forgerock.openam.upgrade.UpgradeException;

/**
 * This service upgrade helper ensures that IdRepo schema is being updated to reference to the DJ SDK based
 * implementation.
 *
 * @author Peter Major
 */
public class IdRepoServiceHelper extends AbstractUpgradeHelper {

    public IdRepoServiceHelper() {
        attributes.add(IdConstants.ID_REPO);
    }

    @Override
    public AttributeSchemaImpl upgradeAttribute(AttributeSchemaImpl oldAttr, AttributeSchemaImpl newAttr)
            throws UpgradeException {
        //we only want to upgrade the sunIdRepoClass attribute, if it still refers to the old LDAPv3Repo implementation
        if (oldAttr.getDefaultValues().equals(newAttr.getDefaultValues())) {
            return null;
        }
        return newAttr;
    }
}
