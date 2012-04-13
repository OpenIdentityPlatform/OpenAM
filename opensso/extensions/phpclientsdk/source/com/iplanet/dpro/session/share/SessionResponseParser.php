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
 * $Id: SessionResponseParser.php,v 1.1 2007/03/09 21:13:11 chicchiricco Exp $
 *
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 */


/**
 * <code>SessionResponseParser</code> parses the <code>SessionResponse</code>
 * XML document and returns the <code>SessionResponse</code> object
 *
 */
class SessionResponseParser {

	/**
	 * <code>SessionResponse</code> object being returned after parsing the
	 * XML document
	 */
	private $sessionResponse = null;

	/**
	 * Document to be parsed
	 */
	private $document = null;

	/**
	 * Constructs new SessionResponseParser
	 * @param XML string representing the response
	 * @exception when the SessionReponse object cannot be parsed
	 */
	public function __construct($xmlString) {
		$this->document = DomDocument :: loadXML($xmlString);
	}

	/**
	 * Parses the session reponse element. Please see file
	 * <code>SessionResponse.dtd</code> for the corresponding DTD of the
	 * SessionResponse.
	 *
	 * @return a <code>SessionResponse</code> object.
	 */
	public function parseXML() {
		if ($this->document == null)
			return null;

		// get document element
		$elem = $this->document->documentElement;
		$this->sessionResponse = new SessionResponse(null, null);
		// set session response attribute
		$temp = $elem->getAttribute("vers");
		$this->sessionResponse->setResponseVersion($temp);
		// set session reqid
		$temp = $elem->getAttribute("reqid");
		$this->sessionResponse->setRequestID($temp);

		// check GetSession element
		$nodelist = $elem->getElementsByTagname("GetSession");
		if ($nodelist != null && $nodelist->length != 0)
			$this->sessionResponse->setMethodID(SessionRequest :: GetSession);

		// check GetActiveSessions element
		$nodelist = $elem->getElementsByTagname("GetActiveSessions");
		if ($nodelist != null && $nodelist->length != 0)
			$this->sessionResponse->setMethodID(SessionRequest :: GetValidSessions);

		// check DestroySession element
		$nodelist = $elem->getElementsByTagname("DestroySession");
		if ($nodelist != null && $nodelist->length != 0)
			$this->sessionResponse->setMethodID(SessionRequest :: DestroySession);

		// check Logout element
		$nodelist = $elem->getElementsByTagname("Logout");
		if ($nodelist != null && $nodelist->length != 0)
			$this->sessionResponse->setMethodID(SessionRequest :: Logout);

		// check AddSessionListener element
		$nodelist = $elem->getElementsByTagname("AddSessionListener");
		if ($nodelist != null && $nodelist->length != 0)
			$this->sessionResponse->setMethodID(SessionRequest :: AddSessionListener);

		// check AddSessionListenerOnAllSessions element
		$nodelist = $elem->getElementsByTagname("AddSessionListenerOnAllSessions");
		if ($nodelist != null && $nodelist->length != 0)
			$this->sessionResponse->setMethodID(SessionRequest :: AddSessionListenerOnAllSessions);

		// check SetProperty element
		$nodelist = $elem->getElementsByTagname("SetProperty");
		if ($nodelist != null && $nodelist->length != 0)
			$this->sessionResponse->setMethodID(SessionRequest :: SetProperty);

		// check GetSessionCount element
		$nodelist = $elem->getElementsByTagname("GetSessionCount");
		if ($nodelist != null && $nodelist->length != 0)
			$this->sessionResponse->setMethodID(SessionRequest :: GetSessionCount);

		// check COUNT element
		$nodelist = $elem->getElementsByTagname("SessionExpirationTimeInfo");
		if ($nodelist != null && $nodelist->length != 0)
			SessionResponseParser::parseAllSessionsGivenUUIDElements($nodelist);

		// check Session element
		$nodelist = $elem->getElementsByTagname("Session");
		if ($nodelist != null && $nodelist->length != 0)
			SessionResponseParser::parseSessionElements($nodelist);

		// check OK element
		$nodelist = $elem->getElementsByTagname("OK");
		if ($nodelist != null && $nodelist->length != 0)
			$this->sessionResponse->setBooleanFlag(true);

		// check Exception element
		$nodelist = $elem->getElementsByTagname("Exception");
		if ($nodelist != null && $nodelist->length != 0)
			$this->sessionResponse->setException(SessionRequestParser :: parseCDATA($nodelist->item(0)));

		// check Status element
		$nodelist = $elem->getElementsByTagname("Status");
		if ($nodelist != null && $nodelist->length != 0)
			$this->sessionResponse->setStatus(SessionRequestParser :: parseCDATA($nodelist->item(0)));

		// return session reponse
		return $this->sessionResponse;
	}

	/*
	 * Parses all the Sessions for the given user.
	 *
	 * @param nodelist.
	 */
	private function parseAllSessionsGivenUUIDElements($nodelist) {
		// parse SessionExpirationTimeInfo one by one
		for ($i = 0; $i < $nodelist->length; $i++) {
			// get one SessionExpirationTimeInfo element
			$sess = $nodelist->item($i);
			// parse one SessionExpirationTimeInfo element
			SessionResponseParser :: parseSessionExpirationTimeInfo($sess);
		}
	}

	/**
	 * Parses all the Sessions Expiration for the given user.
	 *
	 * @param sess session element.
	 */
	private function parseSessionExpirationTimeInfo($sess) {
		$sid = null;
		$expTime = null;

		// parse the attributes
		$temp = $sess->getAttribute("sid");
		if ($temp != null)
			$sid = $temp;

		$temp = $sess->getAttribute("expTime");
		if ($temp != null)
			$expTime = $temp;

		// add to sessionResponse
		$this->sessionResponse->addSessionForGivenUUID($sid, $expTime);
	}

	/**
	 * Parse Session Elements.
	 *
	 * @param nodelist NodeList of Session element.
	 */
	private function parseSessionElements($nodelist) {
		// parse session one by one
		for ($i = 0; $i < $nodelist->length; $i++) {
			// get one Session element
			$sess = $nodelist->item($i);
			// parse one Session element
			$sessionInfo = SessionResponseParser :: parseSessionElement($sess);
			// add to sessionResponse
			$this->sessionResponse->addSessionInfo($sessionInfo);
			//            SessionRequestParser.debug.message("In parse session "
			//                    + sessionInfo.toString());
		}
	}

	/**
	 * Parse one Session Element, it contains two attributes and properties.
	 *
	 * @param sess Session Element.
	 * @return SessionInfo
	 */
	static function parseSessionElement($sess) {
		$sessionInfo = new SessionInfo();
		// parse Session attributes
		$temp = $sess->getAttribute("sid");
		if (temp != null)
			$sessionInfo->sid = $temp;

		$temp = $sess->getAttribute("stype");
		if ($temp != null)
			$sessionInfo->stype = $temp;

		$temp = $sess->getAttribute("cid");
		if ($temp != null)
			$sessionInfo->cid = $temp;

		$temp = $sess->getAttribute("cdomain");
		if ($temp != null)
			$sessionInfo->cdomain = $temp;

		$temp = $sess->getAttribute("maxtime");
		if ($temp != null)
			$sessionInfo->maxtime = $temp;

		$temp = $sess->getAttribute("maxidle");
		if ($temp != null)
			$sessionInfo->maxidle = $temp;

		$temp = $sess->getAttribute("maxcaching");
		if ($temp != null)
			$sessionInfo->maxcaching = $temp;

		$temp = $sess->getAttribute("timeleft");
		if ($temp != null)
			$sessionInfo->timeleft = $temp;

		$temp = $sess->getAttribute("timeidle");
		if ($temp != null)
			$sessionInfo->timeidle = $temp;

		$temp = $sess->getAttribute("state");
		if ($temp != null)
			$sessionInfo->state = $temp;

		// parse session properties
		$properties = $sess->getElementsByTagname("Property");
		if ($properties != null) {
			// parse all properties
			for ($j = 0; $j < $properties->length; $j++) {
				// get Property element
				$property = $properties->item($j);
				// get property attributes
				$name = $property->getAttribute("name");
				if ($name != null)
					$sessionInfo->properties[$name] = $property->getAttribute("value");
			}
		}

		return $sessionInfo;
	}
}
?>
