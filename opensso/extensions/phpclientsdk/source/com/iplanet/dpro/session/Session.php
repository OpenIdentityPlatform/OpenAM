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
 * $Id: Session.php,v 1.1 2007/03/09 21:13:08 chicchiricco Exp $
 *
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 */


/**
 * The <code>Session</code> class represents a session. It contains session
 * related information such as session ID, session type (user/application),
 * client ID (user ID or application ID), session idle time, time left on the
 * session, and session state. It also allows applications to add listener for
 * session events.
 *
 * <pre>
 *  The following is the state diagram for a session:
 *
 *                     |
 *                     |
 *                     |
 *                     V
 *       ---------- invalid
 *      |              |
 *      |              |creation (authentication OK)
 *      |              |
 *      |max login time|   max idle time
 *      |destroy       V  ---------------&gt;
 *      |            valid              inactive --
 *      |              |  &lt;--------------           |
 *      |              |       reactivate           |
 *      |              |                            |
 *      |              | logout                     | destroy
 *      |              | destroy                    | max session time
 *      |              | max session time           |
 *      |              V                            |
 *       ---------&gt;  destroy  &lt;---------------------
 *
 * </pre>
 *
 * @see com.iplanet.dpro.session.SessionID
 * @see com.iplanet.dpro.session.SessionListener
 */
class Session {

	const INVALID = 0;
    const VALID = 1;
    const INACTIVE = 2;
    const DESTROYED = 3;

	const USER_SESSION = 0;
    const APPLICATION_SESSION = 1;

    const SESSION_HANDLE_PROP = "SessionHandle";

    const TOKEN_RESTRICTION_PROP = "TokenRestriction";

    const SESSION_SERVICE = "session";

	// TODO: read from properties!
	private static $cookieName = "iPlanetDirectoryPro";
	private static $resetLBCookie = false;
	private static $pollingEnabled = true;
	const lbCookieName = "amlbcookie";

	private static $sessionServiceURLTable = array ();
	private static $sessionTable = array ();
	private static $serverID = null;
	private static $purgeDelay = null;

	private $sessionID;
	private $latestRefreshTime;
	private $maxCachingTime;
	private $needToReset = false;
	private $sessionIsLocal = false;
	private $sessionServiceURL;

	public static function __init() {
		Session :: $serverID = WebtopNaming :: getAMServerID();
		Session :: $purgeDelay = SystemProperties :: get("com.iplanet.am.session.purgedelay");
	}

	/**
	 * Determines whether session code runs in core server or client SDK
	 * run-time mode
	 *
	 * @return true if running in core server mode, false otherwise
	 */
	static function isServerMode() {
		return WebtopNaming :: isServerMode();
	}

	/**
	 * Returns Session Service URL for a given server ID.
	 *
	 * @param serverID server ID from the platform server list.
	 * @return Session Service URL.
	 * @exception SessionException.
	 */
	public static public function getSessionServiceURLFromString($serverID) {
		try {
			$parsedServerURL = WebtopNaming :: getServerFromID($serverID);
			return Session :: getSessionServiceURLFromData($parsedServerURL["protocol"], $parsedServerURL["host"], $parsedServerURL["port"]);
		} catch (Exception $e) {
			throw new SessionException($e);
		}
	}

	/**
	 * Returns Session Service URL for a Session ID.
	 *
	 * @param sid Session ID
	 * @return Session Service URL.
	 * @exception SessionException
	 */
	public static function getSessionServiceURLFromSid($sid) {
		$primary_id = null;

		if (Session :: isServerMode()) {
			$ss = SessionService :: getSessionService();
			if ($ss->isSiteEnabled() && $ss->isLocalSite($sid)) {
				if ($ss . isSessionFailoverEnabled()) {
					return Session :: getSessionServiceURLFromString($ss->getCurrentHostServer($sid));
				} else {
					$primary_id = $sid->getExtension(SessionID :: PRIMARY_ID);
					return Session :: getSessionServiceURLFromString($primary_id);
				}
			}
		} else {
			$primary_id = $sid->getExtension(SessionID :: PRIMARY_ID);
			if ($primary_id != null) {
				$secondarysites = WebtopNaming :: getSecondarySites($primary_id);
				if (($secondarysites != null) && ($this->serverID != null)) {
					if (strpos($secondarysites, $this->serverID))
						return Session :: getSessionServiceURLFromString(Session :: $serverID);
				}
			}
		}

		return Session :: getSessionServiceURLFromData($sid->getSessionServerProtocol(), $sid->getSessionServer(), $sid->getSessionServerPort());
	}

	/**
	* Returns Session Service URL.
	*
	* @param protocol Session Server protocol.
	* @param server Session Server host name.
	* @param port Session Server port.
	* @return Session Service URL.
	* @exception SessionException.
	*/
	public static function getSessionServiceURLFromData($protocol, $server, $port) {
		$key = $protocol +"://" + $server +":" + $port;
		$url = Session :: $sessionServiceURLTable[$key];
		if ($url == null) {
			try {
				$url = WebtopNaming :: getServiceURL(Session :: SESSION_SERVICE, $protocol, $server, $port);

				Session :: $sessionServiceURLTable[$key] = $url;
				return $url;
			} catch (Exception $e) {
				throw new SessionException($e->getMessage());
			}
		}
		return $url;
	}

	/*
	 * Used in this package only.
	 */
	function __construct($sid) {
		$this->sessionID = $sid;
		//        if (isServerMode()) {
		//            sessionService = SessionService.getSessionService();
		//        }
		$this->latestRefreshTime = time();
	}

	/**
	 * Indicates whether local or remote invocation of Sesion Service should be
	 * used
	 *
	 * @return true if local invocation should be used, false otherwise
	 */
	function isLocal() {
		return $this->sessionIsLocal;
	}

	/**
	 * Gets the Session Service URL for this session object.
	 *
	 * @return The Session Service URL for this session.
	 * @exception SessionException when cannot get Session URL.
	 */
	public function getSessionServiceURL() {
		if (Session :: isServerMode())
			return Session :: getSessionServiceURLFromSid($this->sessionID);

		// we can cache the result because in client mode
		// session service location does not change
		// dynamically
		if ($this->sessionServiceURL == null)
			$this->sessionServiceURL = Session :: getSessionServiceURLFromSid($this->sessionID);

		return $this->sessionServiceURL;
	}

	/**
	 * Returns a session based on a Session ID object.
	 *
	 * @param sid Session ID.
	 * @return A Session object.
	 * @throws SessionException if the Session ID object does not contain a
	 *         valid session string, or the session string was valid before
	 *         but has been destroyed, or there was an error during
	 *         communication with session service.
	 */
	public static function getSession($sid) {
		if ($sid == null || strlen($sid) == 0)
			throw new SessionException("Invalid session ID");

		$session = Session :: $sessionTable[$sid->__toString()];
		if ($session != null) {
			$restriction = $session->getRestriction();
			$context = RestrictedTokenContext :: getCurrent();
			if ($context == null) {
				if ($session->context != null)
					$context = $session->context;
				else {
					/*
					 * In cookie hijacking mode...
					 * After the server remove the agent token id from the
					 * user token id. server needs to create the agent token
					 * from this agent token id. Now, the restriction context
					 * required for session creation is null, so we added it
					 * to get the agent session created.*/
					$context = gethostbyname("localhost");
					$session->context = $context;
					// sessionDebug . message("Session:getSession : context: " + context);
				}
			}

			if ($restriction != null && !$restriction->isSatisfied($context))
				throw new SessionException("Restriction violation");

			if ($session->maxCachingTimeReached())
				$session->refresh(false);
			return $session;
		}

		$session = new Session($sid);
		$session->refresh(true);

		$session->context = RestrictedTokenContext :: getCurrent();

		Session :: $sessionTable[$sid->__toString()] = $session;
		if (!Session::$pollingEnabled)
			$session->addInternalSessionListener();

		return $session;
	}

	/**
	* Gets the latest session from session server and updates the local cache
	* of this session.
	*
	* @param reset The flag to indicate whether to reset the latest session
	*        access time in the session server.
	* @exception SessionException if the session reached its
	*            maximum session time, or the session was destroyed, or
	*            there was an error during communication with session
	*            service.
	*/
	public function refresh($reset) {
		// recalculate whether session is local or remote on every refresh
		// this is just an optmization
		// it is functionally safe to always use remote mode
		// but it is not efficient
		// this check takes care of migration "remote -> local"
		// reverse migration "local - > remote" will be
		// done by calling Session.markNonLocal() from
		// SessionService.handleReleaseSession()
		//        $sessionIsLocal = $this->checkSessionLocal();

		$activeContext = RestrictedTokenContext :: getCurrent();
		if ($activeContext == null)
			$activeContext = $this->context;
		//		try {
		//			RestrictedTokenContext . doUsing(activeContext, new RestrictedTokenAction() {
		//				public Object run() throws Exception {
		//					doRefresh($reset);
		//					return null;
		//				}
		//			});
		//
		//		} catch (Exception e) {
		//			Session . removeSID(sessionID);
		//			if (sessionDebug . messageEnabled()) {
		//				sessionDebug . message("session.Refresh " + "Removed SID:" + sessionID);
		//			}
		//			throw new SessionException(e);
		//		}

		$this->doRefresh($reset);
	}

	/*
	 * Refreshes the Session Information
	 * @param <code>true</code> refreshes the Session Information
	 */
	private function doRefresh($reset) {
		$info = null;
		$flag = $reset || $this->needToReset;
		$this->needToReset = false;
		if ($this->isLocal())
			$info = $this->sessionService->getSessionInfo($this->sessionID, $this->flag);
		else {
			$sreq = new SessionRequest(SessionRequest :: GetSession, $this->sessionID, $this->flag);
			$sres = $this->getSessionResponse($this->getSessionServiceURL(), $sreq);
			if ($sres->getException() != null)
				throw new SessionException("Invalid session state");

			$infos = $sres->getSessionInfoVector();
			if (count($infos) != 1)
				throw new SessionException("Unexpected session");

			$info = $infos[0];
		}
		$this->update($info);
		$this->latestRefreshTime = time();
	}

	    /**
     * Updates the session from the session information server.
     *
     * @param info Session Information.
     */
    function update(SessionInfo $info) {
        if ($info->stype === "user")
            $this->sessionType = Session::USER_SESSION;
        else if ($info->stype === "application")
            $this->sessionType = Session::APPLICATION_SESSION;
        $this->clientID = $info->cid;
        $this->clientDomain = $info->cdomain;
        $this->maxSessionTime = $info->maxtime;
        $this->maxIdleTime = $info->maxidle;
        $this->maxCachingTime = $info->maxcaching;
        $this->sessionIdleTime = $info->timeidle;
        $this->sessionTimeLeft = $info->timeleft;
        if ($info->state === "invalid")
            $this->sessionState = Session::INVALID;
        else if ($info->state === "valid")
            $this->sessionState = Session::VALID;
        else if ($info->state === "inactive")
            $this->sessionState = Session::INACTIVE;
        else if ($info->state === "destroyed")
            $this->sessionState = Session::DESTROYED;
        $this->sessionProperties = $info->properties;
        if ($this->timedOutAt <= 0) {
            $sessionTimedOutProp = $this->sessionProperties["SessionTimedOut"];
            if ($sessionTimedOutProp != null)
            	$this->timedOutAt = $sessionTimedOutProp;
        }
        $this->latestRefreshTime = time();
        // note : do not use getProperty() call here to avoid unexpected
        // recursion via refresh()
        $restrictionProp = $this->sessionProperties[Session::TOKEN_RESTRICTION_PROP];
        if ($restrictionProp != null)
            try {
                $this->restriction = TokenRestrictionFactory::unmarshal($restrictionProp);
            } catch (Exception $e) {
                throw new SessionException($e);
            }
    }

    /**
     * Returns lbcookie value for the Session
     * @param  a session ID for lbcookie.
     * @return lbcookie value
     * @throws SessionException if session is invalid
     */
    public static function getLBCookie(SessionID $sid) {
        $cookieValue = null;
        if ($sid == null || $sid->__toString() == null ||
            strlen($sid->__toString()) == 0)
            throw new SessionException("Invalid SessionID");

        if (Session::$resetLBCookie)
            if (Session::isServerMode()) {
                $ss = SessionService::getSessionService();
                if ($ss->isSessionFailoverEnabled())
                    $cookieValue = $ss->getCurrentHostServer($sid);
            } else {
                $sess = Session::$sessionTable[$sid->__toString()];
                if ($sess != null)
                    $cookieValue = sess.getProperty(lbCookieName);
            }

        if ($cookieValue == null || strlen($cookieValue) == 0)
            $cookieValue = $sid->getExtension(SessionID::PRIMARY_ID);

        return Session::lbCookieName . "=" . $cookieValue;
    }

	/**
	 * Returns a Session Response object based on the XML document received from
	 * remote Session Server. This is in response to a request that we send to
	 * the session server.
	 *
	 * @param svcurl The URL of the Session Service.
	 * @param sreq The Session Request XML document.
	 * @return a Vector of responses from the remote server
	 * @exception SessionException if there was an error in sending the XML
	 *            document or if the response has multiple components.
	 */
	public static function sendPLLRequest($svcurl, SessionRequest $sreq) {
		try {
			$cookies = Session :: $cookieName . "=" . $sreq->getSessionID();
			if (!Session :: isServerMode()) {
				$fakeRequest[SessionID::$cookieName] = $sreq->getSessionID()->__toString();
				$sessionID = new SessionID($fakeRequest);
				$cookies .= ";" . Session :: getLBCookie($sessionID);
			}

			$req = new Request($sreq->toXMLString());
			$set = new RequestSet(Session :: SESSION_SERVICE);
			$set->addRequest($req);
			$responses = PLLClient :: sendUrlCookiesRequestSet($svcurl, $cookies, $set);
			if (count($responses) != 1)
				throw new SessionException("Unexpected response");

			$res = $responses[0];
			return SessionResponse :: parseXML($res->getContent());
		} catch (Exception $e) {
			throw new SessionException($e);
		}
	}

	/**
	 * Sends remote session request without retries.
	 *
	 * @param svcurl Session Service URL.
	 * @param sreq Session Request object.
	 * @exception SessionException.
	 */
	private function getSessionResponseWithoutRetry($svcurl, $sreq) {
		try {
			$context = RestrictedTokenContext :: getCurrent();
			if ($context != null)
				$sreq->setRequester(RestrictedTokenContext :: marshal($context));

			$sres = Session :: sendPLLRequest($svcurl, $sreq);
			if ($sres->getException() != null) {
				// Check if this exception was thrown due to Session Time out or
				// not
				// If yes then set the private variable timedOutAt to the
				// current time
				// But before that check if this timedOutAt is already set or
				// not. No need of
				// setting it again
				if ($this->timedOutAt <= 0) {
					$exceptionMessage = $sres->getException();
					if (strpos($exceptionMessage . indexOf("SessionTimedOutException")))
						$this->timedOutAt = time();
				}
				throw new SessionException($sres->getException());
			}
			return $sres;
		} catch (Exception $e) {
			throw new SessionException($e->getMessage());
		}
	}

	/**
	 * When used in internal request routing mode, it sends remote session
	 * request with retries. If not in internal request routing mode simply
	 * calls <code>getSessionResponseWithoutRetry</code>.
	 *
	 * @param svcurl Session Service URL.
	 * @param sreq Session Request object.
	 * @exception SessionException
	 */
	private function getSessionResponse($svcurl, $sreq) {
		if ($this->isServerMode() && SessionService :: getUseInternalRequestRouting()) {
			try {
				return $this->getSessionResponseWithoutRetry($svcurl, $sreq);
			} catch (SessionException $e) {
				// attempt retry if appropriate
				$hostServer = $this->sessionService->getCurrentHostServer($this->sessionID);
				if (!$this->sessionService->checkServerUp($hostServer)) {
					// proceed with retry
					// Note that there is a small risk of repeating request
					// twice (e.g., normal exception followed by server failure)
					// This danger is insignificant because most of our requests
					// are idempotent. For those which are not (e.g.,
					// logout/destroy)
					// it is not critical if we get an exception attempting to
					// repeat this type of request again.
					$retryURL = $this->getSessionServiceURL();
					if ($retryURL != $svcurl)
						return getSessionResponseWithoutRetry($retryURL, $sreq);
				}
				throw e;
			}
		} else
			return $this->getSessionResponseWithoutRetry($svcurl, $sreq);
	}

    /**
     * Set the cookie Mode based on whether the request has cookies or not. This
     * method is called from <code>createSSOToken(request)</code> method in
     * <code>SSOTokenManager</code>.
     *
     * @param cookieMode whether request has cookies or not.
     */
    public function setCookieMode($cookieMode) {
//        if (sessionDebug.messageEnabled()) {
//            sessionDebug.message("CookieMode is:" + cookieMode);
//        }
        if ($cookieMode != null)
            $this->cookieMode = $cookieMode;
    }

    /**
     * Returns the session ID.
     * @return The session ID.
     */
    public function getID() {
        return $this->sessionID;
    }

        /**
     * Used to find out if the maximum caching time has reached or not.
     */
    protected function maxCachingTimeReached() {
        $cachingtime = time() - $this->latestRefreshTime;
        if ($this->cachingtime > $this->maxCachingTime * 60)
            return true;
        else
            return false;
    }

        /**
     * Gets the property stored in this session.
     *
     * @param name The property name.
     * @return The property value in String format.
     * @exception SessionException is thrown if the session reached its
     *            maximum session time, or the session was destroyed, or
     *            there was an error during communication with session
     *            service.
     */
    public function getProperty($name) {
        if ($name != Session::lbCookieName)
            if ($this->maxCachingTimeReached() || array_key_exists($name, $this->sessionProperties))
                $this->refresh(false);

        return $this->sessionProperties[$name];
    }

        /**
     * Sets a property for this session.
     *
     * @param name The property name.
     * @param value The property value.
     * @exception SessionException if the session reached its maximum session
     *            time, or the session was destroyed, or there was an error
     *            during communication with session service.
     */
    public function setProperty($name, $value) {
        try {
            if ($this->isLocal())
                $this->sessionService->setProperty($this->sessionID, $name, $value);
            else {
                $sreq = new SessionRequest(
                       SessionRequest::SetProperty, $this->sessionID, false);
                $sreq->setPropertyName($name);
                $sreq->setPropertyValue($value);
                $this->getSessionResponse($this->getSessionServiceURL(), $sreq);
            }
            $this->sessionProperties[$name] = $value;
        } catch (Exception $e) {
            throw new SessionException($e);
        }
    }

}
?>
