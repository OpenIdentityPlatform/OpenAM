package org.forgerock.oauth2.core.exceptions;


import org.forgerock.oauth2.core.OAuth2Constants.UrlLocation;

/**
 * Thrown when the duplicate request parameter is found.
 *
 * @since 13.5.0
 */
public class DuplicateRequestParameterException extends OAuth2Exception {



    /**
     * Constructs a new DuplicateRequestParameterException with the default message.
     */
    public DuplicateRequestParameterException() {
        this("The request has a duplicate request parameter.");
    }

    /**
     * Constructs a new DuplicateRequestParameterException with the specified message.
     * The {@link UrlLocation} for the parameters are defaulted to QUERY.
     *
     * @param message The reason for the exception.
     */
    public DuplicateRequestParameterException(final String message) {
        this(message, UrlLocation.QUERY);
    }

    /**
     * Constructs a new DuplicateRequestParameterException with the specified message.
     *
     * @param message The reason for the exception.
     * @param parameterLocation Indicates the location of the parameters in the URL.
     */
    public DuplicateRequestParameterException(final String message, final UrlLocation parameterLocation) {
        super(400, "invalid_request", message, parameterLocation);
    }
}
