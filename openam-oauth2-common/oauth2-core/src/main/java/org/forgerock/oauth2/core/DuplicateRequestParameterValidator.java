package org.forgerock.oauth2.core;

import org.forgerock.oauth2.core.exceptions.*;

import java.util.Set;

/**
 * Implementation of the AuthorizeRequestValidator for duplicate request parameter validation.
 *
 * @since 14.0.0
 */
public class DuplicateRequestParameterValidator implements AuthorizeRequestValidator {


    /**
     * {@inheritDoc}
     */
    public void validateRequest(OAuth2Request request) throws InvalidClientException, InvalidRequestException, RedirectUriMismatchException,
            UnsupportedResponseTypeException, ServerException, BadRequestException, InvalidScopeException, NotFoundException, DuplicateRequestParameterException {

        Set<String> parameterNames = request.getParameterNames();
        for (String parameterName : parameterNames) {
            if (request.getParameterCount(parameterName) > 1) {
                throw new DuplicateRequestParameterException ("Invalid Request, duplicate request parameter found : " + parameterName);
            }
        }
    }
}
