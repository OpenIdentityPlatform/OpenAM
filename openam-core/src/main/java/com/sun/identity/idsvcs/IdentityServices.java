/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007 Sun Microsystems Inc. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * https://opensso.dev.java.net/public/CDDLv1.0.html or
 * opensso/legal/CDDLv1.0.txt
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at opensso/legal/CDDLv1.0.txt.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * $Id: IdentityServices.java,v 1.5 2009/12/15 00:34:57 veiming Exp $
 *
 */
/**
 * Portions Copyrighted 2011 ForgeRock AS
 */
package com.sun.identity.idsvcs;

import com.iplanet.am.util.Token;

import java.util.List;
import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Base interface for all security providers.
 */
public interface IdentityServices extends Remote {

    /**
     * Attempt to authenticate using simple user/password credentials.
     * @param username Subject's user name.
     * @param password Subject's password
     * @param uri Subject's additional context such as module, organ, etc
     * @return Subject's token if authenticated.
     * @throws UserNotFound if user not found.
     * @throws InvalidPassword if password is invalid.
     * @throws NeedMoreCredentials if additional credentials are needed for
     * authentication.
     * @throws InvalidCredentials if credentials are invalid.
     * @throws GeneralFailure on other errors.
     */
    public Token authenticate(String username, String password, String uri)
        throws UserNotFound, InvalidPassword, NeedMoreCredentials,
        InvalidCredentials, GeneralFailure, RemoteException;

    /**
     * Close session referenced by the subject token.
     * @param subject Token identifying the session to close.
     * @throws GeneralFailure errors.
     */
    public void logout(Token subject)
        throws GeneralFailure, RemoteException;

    /**
     * Attempt to authorize the subject for the optional action on the
     * requested URI.
     * @param uri URI for which authorization is required
     * @param action Optional action for which subject is being authorized
     * @param subject Token identifying subject to be authorized
     * @return boolean <code>true</code> if allowed; else <code>false</code>
     * @throws NeedMoreCredentials when more credentials are required for
     * authorization.
     * @throws TokenExpired when subject's token has expired.
     * @throws GeneralFailure on other errors.
     */
    public boolean authorize(String uri, String action, Token subject)
        throws NeedMoreCredentials, TokenExpired, GeneralFailure,
        RemoteException;

    /**
     * Retrieve user details (roles, attributes) for the subject.
     * @param attributeNames Optional list of attributes to be returned
     * @param subject Token for subject.
     * @return User details for the subject.
     * @throws TokenExpired when Token has expired.
     * @throws GeneralFailure on other errors.
     * @throws AccessDenied if reading of attributes for the user is disallowed
     */
    public UserDetails attributes(List attributeNames, Token subject, boolean refresh)
        throws TokenExpired, GeneralFailure, RemoteException, AccessDenied;

    /**
     * Logs a message on behalf of the authenticated app.
     *
     * @param app         Token corresponding to the authenticated application.
     * @param subject     Optional token identifying the subject for which the
     * log record pertains.
     * @param logName     Identifier for the log file, e.g. "MyApp.access"
     * @param message     String containing the message to be logged
     * @throws AccessDenied   if app token is not specified
     * @throws GeneralFailure on error
     */
    public void log(Token app, Token subject, String logName, String message)
        throws AccessDenied, TokenExpired, GeneralFailure, RemoteException;

    /**
     * Retrieve a list of identity names matching the input criteria.
     *
     * @param filter Optional filter to use as search against identity names.
     * @param admin Token identifying the administrator to be used to authorize
     * the request.
     * @param attributes Optional list of Attribute objects which provide
     * additional search criteria for the search.
     * @return List The list of identities matching the input criteria.
     * @throws NeedMoreCredentials when more credentials are required for
     * authorization.
     * @throws TokenExpired when subject's token has expired.
     * @throws GeneralFailure on other errors.
     */
    public List search(String filter, List attributes, Token admin)
        throws NeedMoreCredentials, TokenExpired, GeneralFailure,
            RemoteException;

    /**
     * Creates an identity object with the specified attributes.
     *
     * @param admin Token identifying the administrator to be used to authorize
     * the request.
     * @param identity object containing the attributes of the object to be created.
     * @throws NeedMoreCredentials when more credentials are required for
     * authorization.
     * @throws DuplicateObject if an object matching the name, type and realm already exists.
     * @throws TokenExpired when subject's token has expired.
     * @throws GeneralFailure on other errors.
     */
    public void create(IdentityDetails identity, Token admin)
        throws NeedMoreCredentials, DuplicateObject, TokenExpired,
        GeneralFailure, RemoteException;

    /**
     * Retrieves an identity object matching input criteria.
     *
     * @param name The name of identity to retrieve.
     * @param attributes Attribute objects specifying criteria for the object
     * to retrieve.
     * @param admin Token identifying the administrator to be used to authorize
     * the request.
     * @return IdentityDetails of the subject.
     * @throws NeedMoreCredentials when more credentials are required for
     * authorization.
     * @throws ObjectNotFound if no subject is found that matches the input criteria.
     * @throws TokenExpired when subject's token has expired.
     * @throws GeneralFailure on other errors.
     * @throws AccessDenied if reading of attributes for the user is disallowed
     */
    public IdentityDetails read(String name, List attributes, Token admin)
        throws NeedMoreCredentials, ObjectNotFound, TokenExpired,
        GeneralFailure, RemoteException, AccessDenied;

    /**
     * Updates an identity object with the specified attributes.
     *
     * @param admin Token identifying the administrator to be used to authorize
     * the request.
     * @param identity object containing the attributes of the object to be updated.
     * @throws NeedMoreCredentials when more credentials are required for
     * authorization.
     * @throws ObjectNotFound if the requested object to update cannot be found.
     * @throws TokenExpired when subject's token has expired.
     * @throws GeneralFailure on other errors.
     * @throws AccessDenied if reading of attributes for the user is disallowed
     */
    public void update(IdentityDetails identity, Token admin)
        throws NeedMoreCredentials, ObjectNotFound, TokenExpired,
        GeneralFailure, RemoteException, AccessDenied;

    /**
     * Deletes an identity object matching input criteria.
     *
     * @param admin Token identifying the administrator to be used to authorize
     * the request.
     * @param identity IdentityDetails of the subject.
     * @throws NeedMoreCredentials when more credentials are required for
     * authorization.
     * @throws ObjectNotFound if no subject is found that matches the input criteria.
     * @throws TokenExpired when subject's token has expired.
     * @throws GeneralFailure on other errors.
     * @throws AccessDenied if deleting special users.
     */
    public void delete(IdentityDetails identity, Token admin)
        throws NeedMoreCredentials, ObjectNotFound, TokenExpired,
        GeneralFailure, RemoteException, AccessDenied;
    
    /**
     * Validates the <class>token</class> obtained during validation
     *
     * @param token Token being verified
     * @return <true> if token is valid
     * @throws InvalidToken if token is not a valid token
     * @throws TokenExpired if token has expired.
     * @throws GeneralFailure on other errors.
     */
    public boolean isTokenValid(Token token)
        throws InvalidToken, GeneralFailure, TokenExpired, RemoteException;
    
    /**
     * Returns the cookie used by OpenSSO Authentication module to store
     * the SSOToken. Can be used for Single-Sign-On by replaying this cookie
     * back to OpenSSO for other operations.
     *
     * @return cookie name that contains the SSOToken
     * @throws GeneralFailure on other errors.
     */
    public String getCookieNameForToken()
        throws GeneralFailure, RemoteException;
    
    /**
     * Returns a list of cookie names that are used by OpenSSO for
     * authentication and load balancing. Replaying all these cookies
     * during the request is highly recommended.
     *
     * @return <true> if token is valid
     * @throws InvalidToken if token is not a valid token
     * @throws TokenExpired if token has expired.
     * @throws GeneralFailure on other errors.
     */
    public List getCookieNamesToForward()
        throws GeneralFailure, RemoteException;
}
