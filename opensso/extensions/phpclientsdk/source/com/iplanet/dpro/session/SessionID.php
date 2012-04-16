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
 * $Id: SessionID.php,v 1.1 2007/03/09 21:13:09 chicchiricco Exp $
 *
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 */


/**
 * The <code>SessionID</code> class is used to identify a Session object. It
 * contains a random String and the name of the session server. The random
 * String in the Session ID is unique on a given session server.
 *
 * @see com.iplanet.dpro.session.Session
 */
class SessionID {

	// TODO: read from properties!
	public static $cookieName = "iPlanetDirectoryPro";

	// prefix "S" is reserved to be used by session framework-specific
	// extensions for session id format
	const PRIMARY_ID = "S1";

	const STORAGE_KEY = "SK";

	const SITE_ID = "SI";

	private $encriptedString = "";

	private $cookieMode = false;

	private $isParsed = false;

	private $sessionServer;
	private $sessionServerPort;
	private $sessionServerProtocol;
	private $sessionDomain;
	private $sessionServerID;

	/**
	 * Constructs a <code>SessionID</code> object based on a
	 * <code>HttpServletRequest</code> object. but if cookie is not found it
	 * checks the URL for session ID.
	 *
	 * @param request <code>HttpServletRequest</code> object which contains
	 *        the encrypted session string.
	 */
	public function __construct(array $request) {
		$cookieValue = $request[SessionID :: $cookieName];

		// if no cookie found in the request then check if
		// the URL has it.
		if ($cookieValue == null) {
			$realReqSid = SessionEncodeURL :: getSidFromURL($request, SessionID :: $cookieName);
			if ($realReqSid != null)
				$this->encryptedString = $realReqSid;

			$this->cookieMode = false;
		} else {
			$this->cookieMode = true;
			// TODO: how is it possible that spaces are uncorectly unencoded?
			$this->encryptedString =  str_replace(" ", "+", $cookieValue);
		}
	}

	public function getCookieMode() {
		return $this->cookieMode;
	}

	/**
	 * Checks if encrypted string is null or empty
	 *
	 * @return true if encrypted string is null or empty.
	 */
	public function isNull() {
		return $this->isNullString($this->encryptedString);
	}

	/**
	 * Utility method to check if argument is null or empty string
	 *
	 * @param s string to check
	 * @return true if <code>s</code> is null or empty.
	 */
	private function isNullString($s) {
		return ($s == null || strlen($s) == 0);
	}

	/**
	* Retrieves extension value by name Currently used session id extensions
	* are
	*
	* <code>SessionService.SITE_ID</code> server id (from platform server list)
	* hosting this session (in failover mode this will be server id of the
	* load balancer)
	*
	* <code>SessionService.PRIMARY_ID</code>,
	* <code>SessionService.SECONDARY_ID</code> used if internal request
	* routing mode is enabled.
	*
	* @param name Name of the session ID extension.
	* @return extension.
	*/
	public function getExtension($name) {
		$this->parseSessionString();
		return $this->extensions[$name];
	}

	/**
	  * Gets the session server name in this object.
	  *
	  * @return The session server protocol in this object.
	  */
	public function getSessionServerProtocol() {
		if ($this->isNullString($this->sessionServerProtocol))
			$this->parseSessionString();

		return $this->sessionServerProtocol;
	}

	/**
	 * Gets the session server port in this object
	 *
	 * @return The session server port in this object.
	 */
	public function getSessionServerPort() {
		if ($this->isNullString($this->sessionServerPort))
			$this->parseSessionString();

		return $this->sessionServerPort;
	}

	/**
	 * Gets the session server name in this object.
	 *
	 * @return The session server name in this object.
	 */
	public function getSessionServer() {
		if ($this->isNullString($this->sessionServer))
			$this->parseSessionString();

		return $this->sessionServer;
	}

	/**
	 * Gets the domain where this session belongs to.
	 *
	 * @return The session domain name.
	 */
	public function getSessionDomain() {
		return $this->sessionDomain;
	}

	/**
	 * Gets the session server id in this object.
	 *
	 * @return The session server id in this object.
	 */
	public function getSessionServerID() {
		if ($this->isNullString($this->sessionServerID))
			parseSessionString();

		return $this->sessionServerID;
	}

	/**
	* Extracts the  server, protocol, port, extensions and tail from Session ID
	*
	*/
	private function parseSessionString() {
		// parse only once
		if ($this->isParsed)
			return;

		/**
		 * This check is done because the SessionID object is getting created
		 * with empty sid value. This is a temporary fix. The correct fix for
		 * this is, throw a SessionException while creating the SessionID
		 * object.
		 */
		if ($this->isNull())
			throw new Exception("sid value is null or empty");

		try {
			$outerIndex = strrpos($this->encryptedString, "@");
			if (!$outerIndex) {
				$this->isParsed = true;
				return;
			}

			$outer = substr($this->encryptedString, $outerIndex +1);
			$tailIndex = strpos($outer, "#");
			$tail = substr($outer, $tailIndex +1);
			if ($tailIndex) {
				// OpenSSOTODO: implement lazy parsing of the exceptions
				$extensionPart = substr($outer, 0, $tailIndex +1);
				$extensionStr = new DataInputStream(base64_decode($extensionPart));

				$extMap = array ();

				// expected syntax is a sequence of pairs of UTF-encoded strings
				// (name, value)
				while (!$extensionStr->eof()) {
					$extName = $extensionStr->readUTF();
					$extValue = $extensionStr->readUTF();
					$extMap[$extName] = $extValue;
				}
				$this->extensions = $extMap;
			}

			$serverID = $this->extensions[SessionID::SITE_ID];
			if ($serverID != null)
				$this->setServerID($serverID);
		} catch (Exception $e) {
			//debug.error("Invalid sessionid format", e);
			throw new IllegalArgumentException("Invalid sessionid format" + e);
		}
		$this->isParsed = true;
	}

	/**
	 * Sets the server info by making a naming request by passing
	 * its id which is in session id and parses it.
	 * @param id ServerID
	 */
	protected function setServerID($id) {
		try {
			$this->sessionServerID = $id;

			$server = WebtopNaming :: getServerFromID($id);
			$bits = parse_url($server);

			$this->sessionServerProtocol = (isset ($bits['protocol']) ? $bits['protocol'] : "http");
			$this->sessionServer = $bits["host"];
			$this->sessionServerPort = (isset ($bits['port']) ? $bits['port'] : 80);
		} catch (Exception $e) {
			//debug.error("Could not get server info from sessionid", e);
			throw new Exception("Invalid server id in session id: " . $e . getMessage());
		}
	}

	/**
	 * Returns the encrypted session string.
	 *
	 * @return An encrypted session string.
	 */
	public function __toString() {
		return $this->encryptedString;
	}

	    /**
     * Returns a hash code for this object.
     *
     * @return a hash code value for this object.
     */
    public function hashCode() {
        // Since SessionID is immutable, it's hashCode doesn't change.
        return mhash(MHASH_MD5, $this->encryptedString);
    }

}
?>
