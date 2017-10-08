package org.forgerock.oauth2.core;

import java.util.Set;

import org.forgerock.oauth2.core.exceptions.BadRequestException;
import org.forgerock.oauth2.core.exceptions.DuplicateRequestParameterException;
import org.forgerock.oauth2.core.exceptions.InvalidClientException;
import org.forgerock.oauth2.core.exceptions.InvalidRequestException;
import org.forgerock.oauth2.core.exceptions.InvalidScopeException;
import org.forgerock.oauth2.core.exceptions.NotFoundException;
import org.forgerock.oauth2.core.exceptions.RedirectUriMismatchException;
import org.forgerock.oauth2.core.exceptions.ServerException;
import org.forgerock.oauth2.core.exceptions.UnsupportedResponseTypeException;

/**
 * Implementation of the AuthorizeRequestValidator for duplicate request parameter validation.
 *
 * @since 13.5.0
 */
public class DuplicateRequestParameterValidator implements AuthorizeRequestValidator {


    /**
     * {@inheritDoc}
     */
    public void validateRequest(OAuth2Request request) throws InvalidClientException, InvalidRequestException,
            RedirectUriMismatchException, UnsupportedResponseTypeException, ServerException, BadRequestException,
            InvalidScopeException, NotFoundException, DuplicateRequestParameterException {

        Set<String> parameterNames = request.getParameterNames();
        for (String parameterName : parameterNames) {
            if (request.getParameterCount(parameterName) > 1) {
                throw new DuplicateRequestParameterException("Invalid Request, duplicate request parameter found : " + parameterName);
            }
        }
    }
}
