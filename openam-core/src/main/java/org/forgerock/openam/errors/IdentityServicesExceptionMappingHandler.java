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

import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idsvcs.IdServicesException;
import org.forgerock.json.resource.Context;
import org.forgerock.json.resource.Request;

/**
 * To convert from specific Identity Repo (e.g. LDAP) errors into a generic IdServicesError class.
 */
public class IdentityServicesExceptionMappingHandler
        implements ExceptionMappingHandler<IdRepoException, IdServicesException> {

    @Override
    public IdServicesException handleError(Context context, String debug, Request request, IdRepoException error) {
        return handleError(error);
    }

    @Override
    public IdServicesException handleError(String debug, Request request, IdRepoException error) {
        return handleError(error);
    }

    @Override
    public IdServicesException handleError(Context context, Request request, IdRepoException error) {
        return handleError(error);
    }

    @Override
    public IdServicesException handleError(Request request, IdRepoException error) {
        return handleError(error);
    }

    @Override
    public IdServicesException handleError(IdRepoException error) {

        int code;

        try {
            code = Integer.valueOf(error.getLDAPErrorCode());

            if (!IdentityServicesException.isMapped(code)) {
                code = Integer.valueOf(error.getErrorCode()); //use more generic
            }

        } catch (NumberFormatException nfe) {
            try {
                code = Integer.valueOf(error.getErrorCode()); //default to generic
            } catch (NumberFormatException nfe2) {
                code = -1; //default to -1
            }
        }

        return IdentityServicesException.getIdentityServiceException(code, error.getMessage());
    }
}