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
 * $Id: SSOProviderImpl.php,v 1.1 2007/03/09 21:13:17 chicchiricco Exp $
 *
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 */

/**
 * This <code>final</code> class <code>SSOProviderImpl</code> implements
 * <code>SSOProvider</code> interface and provides implemtation of the methods
 * to create , destroy , check the validity of a single sign on token .
 */
final class SSOProviderImpl implements SSOProvider {

	// TODO: read from properties!
	private static $checkIP = true;

	/**
	 * Validate the IP address of the client with the IP stored in Session.
	 * @param sess Session object associated with the token
	 * @param clientIP IP address of the current client who made
	 * <code>HttpRequest</code>.
	 * @return Returns true if the IP is valid else false.
	 * @throws SSOException if IP cannot be validated for the given session
	 */
	public function isIPValid(Session $sess, $clientIP) {
		$check = false;
		try {
			$sessIPAddress = gethostbyname($sess->getProperty("Host"));
			$clientIPAddress = gethostbyname($clientIP);
			$check = ($sessIPAddress == $clientIPAddress);
		} catch (Exception $e) {
			//			if (debug . messageEnabled()) {
			//				debug . message("IP address check of Token Failed", e);
			//			}
		}
		return $check;
	}

	/**
	 * Creates a single sign on token for the <code>HttpRequest</code>
	 *
	 * @param request <code>HttpServletRequest</code>
	 * @return single sign on token for the request
	 * @exception SSOException if the single sign on token cannot be created.
	 */
	public function createSSOTokenFromRequest(array $request) {
		try {
			$sid = new SessionID($request);
			$session = Session :: getSession($sid);
			if ($sid != null) {
				$cookieMode = $sid->getCookieMode();
				//                if (debug.messageEnabled()) {
				//                    debug.message("cookieMode is :" + cookieMode);
				//                }
				if ($cookieMode != null)
					$session->setCookieMode($cookieMode);
			}
			if ($this->checkIP && !isIPValid($session, $_SERVER['REMOTE_ADDR']))
				throw new Exception("Invalid IP address");
			$ssoToken = new SSOTokenImpl($session);
			return $ssoToken;
		} catch (Exception $e) {
			//            if (debug.messageEnabled()) {
			//                debug.message("could not create SSOToken from HttpRequest", e);
			//            }
			throw new SSOException($e->getMessage());
		}
	}
}
?>
