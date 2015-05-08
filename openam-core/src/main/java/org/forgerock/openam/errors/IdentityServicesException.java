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
* Copyright 2015 ForgeRock AS.
*/

package org.forgerock.openam.errors;

import org.forgerock.json.resource.ForbiddenException;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.NotFoundException;
import org.forgerock.json.resource.ResourceException;

/**
 * Used as a reference to IdServiceExceptions.
 */
public class IdentityServicesException {

    //original errors from IdServices
    public static final int GENERAL_ACCESS_DENIED = 402;
    public static final int GENERAL_OBJECT_NOT_FOUND = 223;

    //original errors from LDAP
    public static final int LDAP_NOT_ALLOWED_ON_RDN = 67;
    public static final int LDAP_NO_SUCH_OBJECT = 32;

    /**
     * Contains a map of default errors relating to known ID services errors.
     *
     * @param code to look up.
     * @param message to use in the case no specific error is found
     * @return a specific IdServicesException
     */
    public static ResourceException getException(int code, String message) {
        switch (code) {
            case LDAP_NO_SUCH_OBJECT :
                return new NotFoundException("Object does not exist.");

            case LDAP_NOT_ALLOWED_ON_RDN :
                return new ForbiddenException("Unable to set attributes for identity.");

            case GENERAL_ACCESS_DENIED:
                return new ForbiddenException(message);

            case GENERAL_OBJECT_NOT_FOUND:
                return new NotFoundException(message);

            default:
                return new InternalServerErrorException(message);
        }
    }

    /**
     * Returns true if we can specifically map this (non-general) error.
     *
     * @param code to check
     * @return true if there's a mapping, false otherwise
     */
    public static boolean isMapped(int code) {
        return code == LDAP_NOT_ALLOWED_ON_RDN || code == LDAP_NO_SUCH_OBJECT;
    }
}