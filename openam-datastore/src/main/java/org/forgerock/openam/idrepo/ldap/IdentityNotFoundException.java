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
 * Copyright 2014-2015 ForgeRock AS.
 */
package org.forgerock.openam.idrepo.ldap;

import com.sun.identity.idm.IdRepoBundle;
import com.sun.identity.idm.IdRepoException;
import org.forgerock.opendj.ldap.ResultCode;

public class IdentityNotFoundException extends IdRepoException {


    /**
     * This constructor is used to pass the localized error message At this
     * level, the locale of the caller is not known and it is not possible to
     * throw localized error message at this level. Instead this constructor
     * provides Resource Bundle name ,error code and LDAP Result Code ( in case
     * of LDAP related exception for correctly locating the
     * error message. The default <code>getMessage()</code> will always return
     * English messages only. This is in consistent with current JRE.
     *
     * @param rbName
     *            Resource bundle Name to be used for getting localized error
     *            message.
     * @param errorCode
     *            Key to resource bundle. You can use <code>ResourceBundle rb =
     *        ResourceBunde.getBundle(rbName,locale);
     *        String localizedStr = rb.getString(errorCode)</code>.
     * @param ldapResultCode
     *            ldap result code
     * @param args
     *            arguments to message. If it is not present pass the as null.
     */
    public IdentityNotFoundException(String rbName, String errorCode, ResultCode ldapResultCode, Object[] args) {
        super(rbName, errorCode, ldapResultCode, args);
    }

}
