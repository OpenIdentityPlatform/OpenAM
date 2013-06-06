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
 * Copyright 2013 ForgeRock Inc.
 */
package org.forgerock.openam.idrepo.ldap.helpers;

import com.sun.identity.idm.IdRepoBundle;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdType;
import com.sun.identity.shared.datastruct.CollectionHelper;
import java.util.Map;
import java.util.Set;
import org.forgerock.openam.idrepo.ldap.DJLDAPv3Repo;
import static org.forgerock.openam.utils.CollectionUtils.asSet;

/**
 * Handles AD specific aspects of Data Store, more specifically handles the fact that AD uses bitmasks to store account
 * related informations in the userAccountControl attribute.
 *
 * @author Peter Major
 */
public class ADHelper extends ADAMHelper {

    private static final int DISABLED_MASK = 0x2;
    private static final int ACTIVE_MASK = 0xFFFFFFFD;

    /**
     * Based on the incoming userAccountControl value tells whether the user account is active or disabled.
     *
     * @param value The value of the userAccountControl attribute.
     * @param inactiveValue Not used in this context.
     * @return <code>true</code> if the second bit is not set.
     */
    @Override
    public boolean isActive(String value, String inactiveValue) {
        try {
            int attrValue = Integer.parseInt(value);
            return !((attrValue & DISABLED_MASK) != 0);
        } catch (NumberFormatException nfe) {
            return false;
        }
    }

    /**
     * Based on the incoming values returns the value for the userAccountControl.
     *
     * @param idRepo The IdRepo implementation to be used for querying the current value of userAccountControl.
     * @param name The name of the identity.
     * @param isActive Whether the status needs to be set to active or inactive.
     * @param userStatusAttr The name of the user status attribute (should be always userAccountControl).
     * @param activeValue Not used in this context.
     * @param inactiveValue Not used in this context.
     * @return The string representation of the userAccountControl with the account disable flag.
     * @throws IdRepoException If there was an error while trying to retrieve the current userAccountControl value.
     */
    @Override
    public String getStatus(DJLDAPv3Repo idRepo, String name, boolean isActive, String userStatusAttr, String activeValue,
            String inactiveValue) throws IdRepoException {
        Map<String, Set<String>> attrs = idRepo.getAttributes(null, IdType.USER, name, asSet(userStatusAttr));
        String status = CollectionHelper.getMapAttr(attrs, userStatusAttr);
        try {
            int value = Integer.parseInt(status);
            if (isActive) {
                value &= ACTIVE_MASK;
            } else {
                value |= DISABLED_MASK;
            }
            return Integer.toString(value);
        } catch (NumberFormatException nfe) {
            if (DEBUG.warningEnabled()) {
                DEBUG.warning("Invalid status value in " + userStatusAttr, nfe);
            }
            throw new IdRepoException(IdRepoBundle.BUNDLE_NAME, "202", new Object[]{name});
        }
    }
}
