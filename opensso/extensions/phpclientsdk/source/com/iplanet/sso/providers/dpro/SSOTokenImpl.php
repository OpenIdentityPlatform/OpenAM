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
 * $Id: SSOTokenImpl.php,v 1.1 2007/03/09 21:13:18 chicchiricco Exp $
 *
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 */


/**
 * This class <code>SSOTokenImpl</code> implements the interface
 * <code>SSOToken</code> represents the sso token created for the given
 * <code>Session</code> or through a ldap bind
 *
 * @see com.iplanet.sso.SSOToken
 */
class SSOTokenImpl implements SSOToken {
	/** SSOSession */
	private $SSOSession;

	/** regular LDAP connection for SSOToken, false by default */
	private $ldapConnect = false;

	/** ldapbind ssotoken */
	private $ssoToken = null;

	/** ldapbind */
	//private java.security.Principal ldapBindDN;

	/** HashMap for the ldap token property*/
	private $ldapTokenProperty = array ();

	/**
	 *
	 * Creates <code>SSOTokenImpl</code> for a given <code>Session</code>
	 * @param Session
	 * @see com.iplanet.dpro.session.Session
	 *
	 */
	public function __construct(Session $sess) {
		$this->SSOSession = $sess;
		$this->ldapConnect = false;
	}

	/**
	 * Returns the principal name of the SSOToken
	 *
	 * @return The Principal name
	 * @throws SSOException if the SSOToken is not VALID or if
	 *         there are errors in getting the Principal.
	 */
	public function getPrincipal() {
		try {
			if ($this->ldapConnect)
				return $this->ldapBindDN;

			$principal = $this->SSOSession->getProperty("Principal");
			return $principal;
		} catch (Exception $e) {
			//SSOProviderImpl.debug.message("Can't get token principal name");
			throw new SSOException($e);
		}
	}

	/**
	 * Returns the authentication method used for the authentication.
	 *
	 * @return The authentication method.
	 * @throws SSOException if the SSOToken is not VALID or if
	 *         there are errors in getting the authentication method.
	 */
	public function getAuthType() {
		try {
			if ($this->ldapConnect)
				return "LDAP";

			// auth type may be a list of auth types separated by "|". This can
			// happen because of session upgrade. The list is assumed to have
			// a format like "Ldap|Cert|Radius" with no space between separator.
			// this method simply returns the first auth method in that list.
			$types = $this->SSOSession->getProperty("AuthType");
			$typesC = explode($types, "|");
			return $typesC[0];
		} catch (Exception $e) {
			//SSOProviderImpl.debug.error("Can't get token authentication type");
			throw new SSOException($e);
		}
	}

	/**
	 * Returns the authentication level of the authentication method used for
	 * for authentication.
	 *
	 * @return The authentication level.
	 * @throws SSOException if the SSOToken is not VALID or if
	 *         there are errors in getting the authentication level.
	 */
	public function getAuthLevel() {
		$this->checkTokenType("getAuthLevel");
		try {
			return $this->SSOSession->getProperty("AuthLevel");
		} catch (Exception $e) {
			//SSOProviderImpl.debug.error("Can't get token authentication level");
			throw new SSOException($e);
		}
	}

	/**
	 * Returns the IP Address of the client(browser) which sent the request.
	 *
	 * @return The IP Address of the client
	 * @throws SSOException if the SSOToken is not VALID or if
	 *         there are errors in getting the IP Address of the client.
	 */
	public function getIPAddress() {
		try {
			if ($this->ldapConnect)
				return gethostbyname("localhost");

			$host = $this->SSOSession->getProperty("Host");
			if ($host == null || strlen($host) == 0)
				throw new SSOException("Null IP address");

			return gethostbyname($host);
		} catch (Exception $e) {
			//SSOProviderImpl.debug.error("Can't get client's IPAddress");
			throw new SSOException($e);
		}
	}

	/**
	 * Returns the host name of the client(browser) which sent the request.
	 *
	 * @return The host name of the client
	 * @throws SSOException if the SSOToken is not VALID or if
	 *         there are errors in getting the host name of the client.
	 */
	public function getHostName() {
		try {
			if ($this->ldapConnect)
				return "localhost";

			$hostName = $this->SSOSession->getProperty("HostName");
			if ($hostName == null || strlen($hostName) == 0)
				throw new SSOException("Null hostname");

			return $hostName;
		} catch (Exception $e) {
			//SSOProviderImpl.debug.error("Can't get client's token Host name");
			throw new SSOException($e);
		}
	}

	/**
	 * Returns the time left for this session based on max session time
	 *
	 * @return The time left for this session
	 * @exception A
	 *                SSOException is thrown if the SSOToken is not VALID or if
	 *                there are errors in getting the maximum session time.
	 */
	public function getTimeLeft() {
		$this->checkTokenType("getTimeLeft");
		try {
			return $this->SSOSession->getTimeLeft();
		} catch (Exception $e) {
			//SSOProviderImpl.debug.error("Can't get token maximum time");
			throw new SSOException($e);
		}
	}

	/**
	 * Returns the maximum session time in minutes.
	 *
	 * @return The maximum session time.
	 * @throws SSOException if the SSOToken is not VALID or if
	 *         there are errors in getting the maximum session time.
	 */
	public function getMaxSessionTime() {
		$this->checkTokenType("getMaxSessionTime");
		try {
			return $this->SSOSession->getMaxSessionTime();
		} catch (Exception $e) {
			//SSOProviderImpl.debug.error("Can't get token maximum time");
			throw new SSOException($e);
		}
	}

	/**
	 * Returns the session idle time in seconds.
	 *
	 * @return The session idle time.
	 * @throws SSOException if the SSOToken is not VALID or if
	 *         there are errors in getting the idle time.
	 */
	public function getIdleTime() {
		$this->checkTokenType("getIdleTime");
		try {
			return $this->SSOSession->getIdleTime();
		} catch (Exception $e) {
			//SSOProviderImpl.debug.error("Can't get token idle time");
			throw new SSOException($e);
		}
	}

	/**
	 * Returns the maximum session idle time in minutes.
	 *
	 * @return The maximum session idle time.
	 * @throws SSOException if the SSOToken is not VALID or if
	 *         there are errors in getting the maximum idle time.
	 */
	public function getMaxIdleTime() {
		$this->checkTokenType("getMaxIdleTime");
		try {
			return $this->SSOSession->getMaxIdleTime();
		} catch (Exception $e) {
			//SSOProviderImpl.debug.error("Can't get token maximum idle time");
			throw new SSOException($e);
		}
	}

	/**
	 * Returns SSOToken ID object
	 *
	 * @return SSOTokenID
	 */
	public function getTokenID() {
		if ($this->ldapConnect) {
			if ($this->ssoToken != null) {
				return $ssoToken->getTokenID();
			}
			return null;
		}
		return new SSOTokenIDImpl($this->SSOSession->getID());
	}

	/**
	 * Sets a property for this token.
	 *
	 * @param name
	 *            The property name.
	 * @param value
	 *            The property value.
	 * @throws SSOException if the SSOToken is not VALID or if
	 *         there are errors in setting the property name and value.
	 */
	public function setProperty($name, $value) {
		if ($this->ldapConnect) {
			$this->ldapTokenProperty[$name] = $value;
			return;
		}
		try {
			$this->SSOSession->setProperty($name, $value);
		} catch (Exception $e) {
			//            SSOProviderImpl.debug.error("Can't set property:  " + name + " "
			//                    + value);
			throw new SSOException($e);
		}
	}

	/**
	 * Returns the property stored in this token.
	 *
	 * @param name
	 *            The property name.
	 * @return The property value in String format.
	 * @throws SSOException if the SSOToken is not VALID or if
	 *         there are errors in getting the property value.
	 */
	public function getProperty($name) {
		$property = null;
		if ($this->ssoToken != null)
			$property = $ssoToken->getProperty($name);
		if ($property == null) {
			if ($this->ldapConnect)
				$property = $this->ldapTokenProperty[$name];
			else
				try {
					$property = $this->SSOSession->getProperty($name);
				} catch (Exception $e) {
				//SSOProviderImpl.debug.error("Can't get property: " + name);
				throw new SSOException($e);
			}
		}
		return $property;
	}

	/**
	 * Adds a sso token listener for the token change events.
	 *
	 * @param listener
	 *            A reference to a SSOTokenListener object.
	 * @throws SSOException if the SSOToken is not VALID or if
	 *         there are errors in adding the sso token listener.
	 */
	public function addSSOTokenListener(SSOTokenListener $listener) {
		if (!$this->ldapConnect) {
			try {
				$ssoListener = new SSOSessionListener($listener);
				$this->SSOSession->addSessionListener($ssoListener);
			} catch (Exception $e) {
				//                SSOProviderImpl.debug.error("Couldn't add listener to the token"
				//                    + getTokenID().toString());
				throw new SSOException($e);
			}
		}
	}

	/**
	 * Returns true if the SSOToken is valid.
	 *
	 * @return true if the SSOToken is valid.
	 * @deprecated THIS METHOD WILL BE REMOVED ON 3/15/01. INSTEAD USE
	 *             SSOTokenManager.getInstance().isValidToken(SSOToken)
	 */
	public function isValid() {
		try {
			if ($this->ldapConnect == true)
				return true;
			$state = $this->SSOSession->getState(true);
			return ($state == Session :: VALID) || ($state == Session :: INACTIVE);
		} catch (Exception $e) {
			return false;
		}
	}

	/**
	 * Checks if the SSOTOken is valid
	 *
	 * @throws SSOException is thrown if the SSOToken is not valid
	 * @deprecated THIS METHOD WILL BE REMOVED ON 3/15/01. INSTEAD USE
	 *             SSOTokenManager.getInstance().validateToken(SSOToken)
	 */
	public function validate() {
		try {
			if ($this->ldapConnect)
				return;

			$state = $this->SSOSession->getState(true);
			if (($state == Session :: VALID) || ($state == Session :: INACTIVE));
			return;

			throw new SSOException("Invalid state");
		} catch (Exception $e) {
			throw new SSOException($e);
		}
	}

	/**
	 * Returns true if the token is for ldap connection.
	 *
	 * @return true if the token is for ldap connection.
	 */
	public function isLdapConnection() {
		return $this->ldapConnect;
	}

	/**
	 * Sets the value of ldapConnect. It is used to destroy this token.
	 *
	 * @param status LDAP Connection status.
	 */
	protected function setStatus($status) {
		$this->ldapConnect = $status;
	}

	/**
	 * Returns the encoded URL , rewritten to include the session id.
	 *
	 * @param url
	 *            the URL to be encoded
	 * @return the encoded URL if cookies are not supported or the url if
	 *         cookies are supported.
	 */
	public function encodeURL($url) {
		$this->checkTokenType("encodeURL");
		return $this->SSOSession->encodeURL($url);
	}

	/**
	 * Check if the token is created by direct ldap connection. If yes then
	 * throw unsupported exception
	 *
	 * @param methodName Name of the method calling this check.
	 */
	public function checkTokenType($methodName) {
		if ($this->ldapConnect) {
			$str = methodName + "is an unsupported operation for tokens created" + "by direct ldap connection";
			//SSOProviderImpl.debug.error(str);
			throw new UnsupportedOperationException($str);
		}
	}

	/**
	 * Returns the Session Object.
	 *
	 * @return Session object.
	 */
	function getSession() {
		return $this->SSOSession;
	}

}
?>
