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

    private static final String MEMBER_OF_ATTR = "sun-idrepo-ldapv3-config-memberof";
    private static final String MIN_POOL_SIZE = "sun-idrepo-ldapv3-config-connection_pool_min_size";

    public IdRepoServiceHelper() {
        attributes.add(IdConstants.ID_REPO);
        attributes.add(MEMBER_OF_ATTR);
        attributes.add(MIN_POOL_SIZE);
    }

    @Override
    public AttributeSchemaImpl upgradeAttribute(AttributeSchemaImpl oldAttr, AttributeSchemaImpl newAttr)
            throws UpgradeException {
        //Handling the special case for minimum pool size: we should ensure that we only upgrade the attribute if
        //the definition actually changed. This code ensures that NetscapeLDAPv3 SubSchema is properly handled, so it
        //remains untouched upon subsequent upgrades.
        if (MIN_POOL_SIZE.equals(newAttr.getName())) {
            String oldKey = oldAttr.getI18NKey();
            String newKey = newAttr.getI18NKey();
            if (oldKey == null ? newKey == null : oldKey.equals(newKey)) {
                return null;
            } else {
                return newAttr;
            }
        }
        //we only want to upgrade the sunIdRepoClass, if it still refers to the old LDAPv3Repo implementation
        //this also covers the memberof attribute case, since only the default value have changed for it.
        if (oldAttr.getDefaultValues().equals(newAttr.getDefaultValues())) {
            return null;
        }
        return newAttr;
    }
}
