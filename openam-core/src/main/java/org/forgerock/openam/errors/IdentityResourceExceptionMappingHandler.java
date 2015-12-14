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

package org.forgerock.openam.errors;

import com.sun.identity.idm.IdRepoErrorCode;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.PasswordPolicyException;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.ForbiddenException;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.NotFoundException;
import org.forgerock.json.resource.Request;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.opendj.ldap.ResultCode;
import org.forgerock.services.context.Context;

/**
 *  * Translates errors from IdRepoException to ResourceException

 */
public class IdentityResourceExceptionMappingHandler implements ExceptionMappingHandler<IdRepoException, ResourceException> {

    //original errors from IdServices
    public static final int GENERAL_ACCESS_DENIED = 402;
    public static final int GENERAL_OBJECT_NOT_FOUND = 223;


    @Override
    public ResourceException handleError(Context context, String debug, Request request, IdRepoException error) {
        return handleError(error);
    }

    @Override
    public ResourceException handleError(String debug, Request request, IdRepoException error) {
        return handleError(error);
    }

    @Override
    public ResourceException handleError(Context context, Request request, IdRepoException error) {
        return handleError(error);
    }

    @Override
    public ResourceException handleError(Request request, IdRepoException error) {
        return handleError(error);
    }

    @Override
    public ResourceException handleError(IdRepoException idRepoException) {

        int code = Integer.valueOf(idRepoException.getErrorCode());
        ResultCode ldapResultCode = ResultCode.valueOf(idRepoException.getLdapErrorIntCode());

        if (idRepoException instanceof PasswordPolicyException) {
            //Convert the error code for the LDAP code
            if (ldapResultCode == ResultCode.INVALID_CREDENTIALS) {
                    idRepoException = new PasswordPolicyException(
                            ldapResultCode,
                            IdRepoErrorCode.OLD_PASSWORD_INCORRECT,
                            idRepoException.getMessageArgs());
            }

            if (ldapResultCode == ResultCode.INSUFFICIENT_ACCESS_RIGHTS) {
                return new ForbiddenException(idRepoException);
            }

            if (ldapResultCode == ResultCode.CONSTRAINT_VIOLATION) {
                idRepoException = new PasswordPolicyException(idRepoException.getConstraintViolationDetails());
            }

            return new BadRequestException(idRepoException.getMessage());
        }

        //compute LDAP error
        if (ldapResultCode == ResultCode.NO_SUCH_OBJECT) {
            return new NotFoundException(idRepoException);
        }
        if (ldapResultCode == ResultCode.NOT_ALLOWED_ON_RDN) {
            return new ForbiddenException(idRepoException);
        }

        // Compute error code
        switch (code) {
            case GENERAL_OBJECT_NOT_FOUND:
                return new NotFoundException(idRepoException);
            case GENERAL_ACCESS_DENIED:
                return new ForbiddenException(idRepoException);
            default:
                return new InternalServerErrorException(idRepoException);
        }
    }

}
