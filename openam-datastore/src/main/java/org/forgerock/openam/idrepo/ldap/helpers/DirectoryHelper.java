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
 * Copyright 2013-2016 ForgeRock AS.
 */

package org.forgerock.openam.idrepo.ldap.helpers;

import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdType;
import com.sun.identity.shared.debug.Debug;
import java.nio.charset.Charset;
import java.util.Set;
import org.forgerock.openam.idrepo.ldap.DJLDAPv3Repo;
import static org.forgerock.openam.ldap.LDAPConstants.*;

/**
 * Provides a generic implementation of directory specific settings, that for non-generic directories (like AD), could
 * override when needed.
 */
public class DirectoryHelper {

    protected static final Debug DEBUG = Debug.getInstance("DJLDAPv3Repo");

    /**
     * Encodes the password to use the "correct" character encoding for AD.
     *
     * @param type The type of the identity, which should be always USER.
     * @param binaryValues The password value in binary format which is assumed to be correctly encoded already. May
     * be null.
     * @return The encoded password, or null if the input was null.
     */
    public byte[] encodePassword(IdType type, byte[][] binaryValues) {
        if (type.equals(IdType.USER)) {
            if (binaryValues != null && binaryValues.length > 0) {
                return binaryValues[0];
            }
        }

        return null;
    }

    /**
     * Encodes the password to use the "correct" character encoding for AD.
     *
     * @param type The type of the identity, which should be always USER.
     * @param passwordValues The password value in string format to be encoded. May be null.
     * @return The encoded password, or null if the input was null.
     */
    public byte[] encodePassword(IdType type, Set<String> passwordValues) {
        if (type.equals(IdType.USER)) {
            if (passwordValues != null && !passwordValues.isEmpty()) {
                return encodePassword(passwordValues.iterator().next());
            }
        }

        return null;
    }

    /**
     * Encodes the password to use the "correct" character encoding.
     *
     * @param password The password in string format. May be null.
     * @return The encoded password, or null if the input was null.
     */
    public byte[] encodePassword(String password) {
        return password == null ? null : password.getBytes(Charset.forName("UTF-8"));
    }

    /**
     * Converts the directory specific status value to the default Inactive/Active values.
     *
     * @param value The value of the user's status attribute.
     * @param activeValue The active value configured in the data store settings.
     * @return <code>Active</code> if the value is null or the value is the same as activeValue. Otherwise
     * returns <code>Inactive</code>.
     */
    public String convertToInetUserStatus(String value, String activeValue) {
        return isActive(value, activeValue) ? STATUS_ACTIVE : STATUS_INACTIVE;
    }

    /**
     * Tells whether the user's status attribute corresponds to active or inactive status.
     *
     * @param value The value of the user's status attribute.
     * @param activeValue The active value configured in the data store settings.
     * @return <code>true</code> if the value is null or the value is the same as activeValue. Otherwise
     * returns <code>false</code>.
     */
    public boolean isActive(String value, String activeValue) {
        return value == null || value.equalsIgnoreCase(activeValue);
    }

    /**
     * Returns the directory specific user status attribute value based on the default Inactive/Active settings.
     *
     * @param idRepo Reference to the IdRepo implementation, so extra attribute queries can be made.
     * @param name The name of the identity.
     * @param isActive The user status that needs to be represented.
     * @param userStatusAttr The name of the user status attribute.
     * @param activeValue The active value of the user status attribute.
     * @param inactiveValue The inactive value of the user status attribute.
     * @return The directory specific user status attribute value.
     * @throws IdRepoException If there was an error while retrieving the existing user status attribute.
     */
    public String getStatus(DJLDAPv3Repo idRepo, String name, boolean isActive, String userStatusAttr,
            String activeValue, String inactiveValue) throws IdRepoException {
        return isActive ? activeValue : inactiveValue;
    }
}
