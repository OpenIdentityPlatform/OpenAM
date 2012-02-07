<?php
/* The contents of this file are subject to the terms
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
 * $Id: SSOProvider.php,v 1.1 2007/03/09 21:13:15 chicchiricco Exp $
 *
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 */


/**
 * <code>SSOProvider</code> is the interface that the SSO providers or the
 * plug-ins need to implement. The implementation class that implements this
 * interface MUST have the public default no-arg constructor because
 * SSOTokenManger relies on that to dynamically instantiate an object of such
 * class using Java Reflection.
 */
interface SSOProvider {
	/**
	 * Creates an SSOToken.
	 *
	 * @param request HttpServletRequest
	 * @return SSOToken
	 * @exception SSOException is thrown if the SSOToken can't be created.
	 */
	public function createSSOTokenFromRequest(array $request);

	/**
	 * Creates an SSOToken.
	 *
	 * @param user Principal representing a user or service
	 * @param password LDAP password of the user or service
	 * @return SSOToken
	 * @exception An
	 *                SSOException is thrown if the SSOToken can't be created.
	 * @exception an UnsupporedOperationException is thrown when other errors
	 * occur during the token creation.
	 */
	//public function createSSOTokenFromCredentials($user, $password);

	/**
	 * Creates an SSOToken.
	 * @param sid String representing the SSOToken Id
	 * @return SSOToken
	 * @exception SSOException is thrown if the SSOToken can't be
	 * created.
	 * @exception UnsupportedOperationException is thrown when other unsupported
	 * operation is performed.
	 */
	//public function createSSOTokenFromSid($sid);

	/**
	 * Creates an SSOToken.
	 *
	 * @param sid
	 *            representing the SSOToken Id
	 * @param clientIP
	 *            representing the IP address of the client
	 * @return SSOToken
	 * @exception An
	 *                SSOException is thrown if the SSOToken can't be created.
	 */
	//public function createSSOTokenFromSidAndClientIP($sid, $clientIP);

	/**
	 * Destroys an SSOToken.
	 *
	 * @param token
	 *            The SSOToken object to be destroyed
	 * @exception An SSOException is thrown if the SSOToken can't be destroyed.
	 */
	//public function destroyToken(SSOToken $token);

	/**
	 * Checks if an SSOToken is valid or not.
	 *
	 * @param token
	 *            The SSOToken object to be validated.
	 * @return true or false, true if the token is valid
	 */
	//public function isValidToken(SSOToken $token);

	/**
	 * Checks if the SSOToken is valid.
	 *
	 * @exception An
	 *                SSOException is thrown if the SSOToken is not valid.
	 */
	//public function validateToken(SSOToken $token);

	/**
	 * Refresh the Session corresponding to the SSOToken from the Session
	 * Server.
	 *
	 * @param token SSOToken
	 * @exception An SSOException is thrown if the session cannot be refreshed
	 * for the token
	 *
	 */
	//public function refreshSession(SSOToken $token);

	/**
	 * Destroys an SSOToken.
	 *
	 * @param destroyer
	 *            The SSOToken object used to authorize the operation
	 * @param destroyed
	 *            The SSOToken object to be destroyed.
	 * @exception A
	 *                SSOException is thrown if the there was an error during
	 *                communication with session service.
	 */
	//public function destroyTokenWithDestroyer(SSOToken $destroyer, SSOToken $destroyed);

	/**
	 * Returns valid Sessions.
	 *
	 * @param requester
	 *            The SSOToken object used to authorize the operation
	 * @param server
	 *            The server for which the valid sessions are to be retrieved
	 * @return Set The set of Valid Sessions
	 * @exception A
	 *                SSOException is thrown if the there was an error during
	 *                communication with session service.
	 */
	//public function getValidSessions(SSOToken $requester, $server);
}
?>
