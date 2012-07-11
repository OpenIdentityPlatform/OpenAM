/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2005 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: SSOToken.java,v 1.4 2009/01/16 10:43:14 manish_rustagi Exp $
 *
 */

/*
 * Portions Copyrighted [2010] [ForgeRock AS]
 */

package com.iplanet.sso;


/**
 * The <code>SSOToken</code> class represents a "single sign on"(SSO) token.
 * It contains SSO token-related information such as authentication method used
 * for authentication, authentication level of the authentication method, host
 * name of the client that sent the request (browser). It also contains
 * session-related information such as maximum session time, maximum session
 * idle time and session idle time.
 *
 * @supported.all.api
 */
public interface SSOToken {

    /**
     * Returns the value of the property "Principal" set to the single sign on
     * token.
     * 
     * @return The principal name
     * @exception SSOException
     *                if the single sign on token is not valid or if there are
     *                errors in getting the principal
     */
    public java.security.Principal getPrincipal() throws SSOException;

    /**
     * Returns the authentication method used for authentication.
     * 
     * @return The authentication method
     * @exception SSOException
     *                if the single sign on token is not valid or if there are
     *                errors in getting the authentication method
     */
    public java.lang.String getAuthType() throws SSOException;

    /**
     * Returns the authentication level of the authentication method used for
     * authentication.
     * 
     * @return The authentication level
     * @exception SSOException
     *                if the single sign on token is not valid or if there are
     *                errors in getting the authentication level
     */
    public int getAuthLevel() throws SSOException;

    /**
     * Returns the IP Address of the client (browser) that sent the request.
     * 
     * @return The IP Address of the client
     * @exception SSOException
     *                if the single sign on token is not valid or if there are
     *                errors in getting the IP Address of the client
     */
    public java.net.InetAddress getIPAddress() throws SSOException;

    /**
     * Returns the host name of the client (browser) that sent the request.
     * 
     * @return The host name of the client
     * @exception SSOException
     *                if the single sign on token is not valid or if there are
     *                errors in getting the host name of the client
     */
    public java.lang.String getHostName() throws SSOException;

    /**
     * Returns the time left in seconds on the session based on max session
     * time.
     * 
     * @return The time left in seconds on the session.
     * @exception SSOException
     *                if the single sign on token is not valid or if there are
     *                errors in getting the maximum session time.
     */
    public long getTimeLeft() throws SSOException;

    /**
     * Returns the maximum session time in minutes.
     * 
     * @return The maximum session time in minutes
     * @exception SSOException
     *                if the single sign on token is not valid or if there are
     *                errors in getting the maximum session time
     */
    public long getMaxSessionTime() throws SSOException;

    /**
     * Returns the session idle time in seconds.
     * 
     * @return The session idle time in seconds
     * @exception SSOException
     *                if the single sign on token is not valid or if there are
     *                errors in getting the session idle time
     */
    public long getIdleTime() throws SSOException;

    /**
     * Returns the maximum session idle time in minutes.
     * 
     * @return The maximum session idle time in minutes
     * @exception SSOException
     *                if the single sign on token is not valid or if there are
     *                errors in getting the maximum idle time
     */
    public long getMaxIdleTime() throws SSOException;

    /**
     * Returns single sign on token ID object.
     * 
     * @return single sign on token ID.
     */
    public SSOTokenID getTokenID();

    /**
     * Sets a property for this token.
     * 
     * @param name
     *            The property name.
     * @param value
     *            The property value.
     * @exception SSOException
     *                if the single sign on token is not valid or if there are
     *                errors in setting the property name and value
     */
    public void setProperty(java.lang.String name, java.lang.String value)
            throws SSOException;

    /**
     * Gets the property stored in this token.
     * 
     * @param name
     *            The property name.
     * @return The property value in string format.
     * @exception SSOException
     *                if the single sign on token is not valid or if there are
     *                errors in getting the property value
     */
    public java.lang.String getProperty(java.lang.String name)
            throws SSOException;

    /**
     * Gets the property stored in this token. When ignoreState is set to true,
     * it will return the session property value without refreshing the session
     * even if the session state is invalid but it should be running in the 
     * server mode
     * 
     * @param name
     *            The property name.
     * @param ignoreState
     *            The ignoreState flag.
     * @return The property value in string format.
     * @exception SSOException
     *                if the SSOToken is not VALID and if
     *                ignoreState is set to false.
     */
    public java.lang.String getProperty(java.lang.String name,
                boolean ignoreState ) throws SSOException;

    /**
     * Adds an SSO token listener for the token change events.
     * 
     * @param listener
     *            A reference to an <code>SSOTokenListener</code> object.
     * @exception SSOException
     *                if the token is not valid or if there are errors in
     *                setting the SSO token listener.
     */
    public void addSSOTokenListener(com.iplanet.sso.SSOTokenListener listener)
            throws SSOException;

    /**
     * Returns the encoded URL , rewritten to include the session id. The
     * session id will be encoded in the URL as a query string with entity
     * escaping of ampersand when appending the session id to the query string
     * if the query string has other query parameters.
     * <p>
     * Encoded URL format if query string in the original URL passed is present
     * will be :
     * 
     * <pre>
     *   protocol://server:port/path?queryString&amp;cookieName=cookieValue
     * </pre>
     * 
     * Encoded URL format if query string in the original URL passed is not
     * present will be:
     * 
     * <pre>
     *   protocol://server:port/path?cookieName=cookieValue
     * </pre>
     * 
     * @param url
     *            the URL to be encoded
     * @return the encoded URL if cookies are not supported or the URL if
     *         cookies are supported. Note: We should not use this method for
     *         encoding the image URLs
     * @exception SSOException
     *                if URL cannot be encoded.
     */
    public String encodeURL(String url) throws SSOException;

    /**
     * Returns true if the SSOTokenID associated with this SSOToken is a
     * restricted token, false otherwise.
     *
     * @return true if the token is restricted
     * @throws SSOException If we are unable to determine if the session is
     *              restricted
     */
    public boolean isTokenRestricted() throws SSOException;

    /**
     * Given a restricted token, returns the SSOTokenID of the master token
     * can only be used if the requester is an app token
     *
     * @param requester Must be an app token
     * @param restrictedId The SSOTokenID of the restricted token
     * @return The SSOTokenID string of the master token
     * @throws SSOException If the master token cannot be dereferenced
     */
    public String dereferenceRestrictedTokenID(SSOToken requester, String restrictedId)
    throws SSOException;
}
