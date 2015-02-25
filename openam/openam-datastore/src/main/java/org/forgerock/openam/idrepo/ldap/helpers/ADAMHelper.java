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
 * Copyright 2013-2014 ForgeRock AS.
 */
package org.forgerock.openam.idrepo.ldap.helpers;

import java.nio.charset.Charset;

/**
 * Handles AD and ADAM specific aspects of Data Store. More specifically handles the way the unicodePwd attribute needs
 * to be generated.
 */
public class ADAMHelper extends DirectoryHelper {

    /**
     * Encloses the password with double quotes first, then returns the UTF-16LE bytes representing that value.
     *
     * @param password The password in string format.
     * @return The encoded password, or null if encoding is not applicable.
     */
    @Override
    public byte[] encodePassword(String password) {
        return password == null ? null : ("\"" + password + "\"").getBytes(Charset.forName("UTF-16LE"));
    }
}
