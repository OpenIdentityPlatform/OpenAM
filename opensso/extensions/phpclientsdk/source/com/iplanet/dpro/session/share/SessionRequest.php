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
 * $Id: SessionRequest.php,v 1.1 2007/03/09 21:13:10 chicchiricco Exp $
 *
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 */


/**
 * This <code>SessionRequest</code> class represents a
 * <code>SessionRequest</code> XML document. The <code>SessionRequest</code>
 * DTD is defined as the following:
 * </p>
 *
 * <pre>
 *     &lt;?xml version=&quot;1.0&quot;&gt;
 *     &lt; !DOCTYPE SessionRequest [
 *     &lt; !ELEMENT SessionRequest (GetSession |
 *                                   GetValidSessions |
 *                                   DestroySession |
 *                                   Logout |
 *                                   AddSessionListener |
 *                                   AddSessionListenerOnAllSessions |
 *                                   SetProperty |
 *                                   GetSessionCount)&gt;
 *     &lt; !ATTLIST SessionRequest
 *         vers   CDATA #REQUIRED
 *         reqid  CDATA #REQUIRED&gt;
 *     &lt; !-- This attribute carries the requester identity info --&gt;
 *          requester  CDATA #IMPLIED&gt;
 *     &lt; !ELEMENT GetSession (SessionID)&gt;
 *     &lt; !-- This attribute indicates whether resets
 *     the latest access time --&gt;
 *         reset  CDATA #REQUIRED&gt;
 *     &lt; !ELEMENT GetValidSessions (SessionID Pattern?)&gt;
 *     &lt; !ELEMENT DestroySession (SessionID, DestroySessionID)&gt;
 *     &lt; !ELEMENT Logout (SessionID)&gt;
 *     &lt; !ELEMENT AddSessionListener (SessionID, URL)&gt;
 *     &lt; !ELEMENT AddSessionListenerOnAllSessions (SessionID, URL)&gt;
 *     &lt; !ELEMENT SetProperty (SessionID, Property)&gt;
 *     &lt; !ATTLIST Property
 *         name   CDATA #REQUIRED
 *         value  CDATA #REQUIRED&gt;
 *     &lt; !ELEMENT SessionID (#PCDATA)&gt;
 *     &lt; !ELEMENT DestroySessionID (#PCDATA)&gt;
 *     &lt; !ELEMENT URL (#PCDATA)&gt;
 *     &lt; !ELEMENT GetSessionCount (SessionID, UUID)&gt;
 *     &lt; !ELEMENT UUID (#PCDATA)&gt;
 *     &lt; !ELEMENT Pattern (#PCDATA)&gt;
 *     ]&gt;
 * </pre>
 *
 * </p>
 */
class SessionRequest {

	const GetSession = 0;

	const GetValidSessions = 1;

	const DestroySession = 2;

	const Logout = 3;

	const AddSessionListener = 4;

	const AddSessionListenerOnAllSessions = 5;

	const SetProperty = 6;

	const GetSessionCount = 7;

	const QUOTE = "\"";

	const NL = "\n";

	const AMPERSAND = "&amp;";

	const LESSTHAN = "&lt;";

	const GREATERTHAN = "&gt;";

	const APOSTROPHE = "&apos;";

	const QUOTATION = "&quot;";

	private $requestVersion = "1.0";

	private $requestID = null;

	private $resetFlag;

	private $methodID;

	private $sessionID = null;

	private $requester = null;

	private $destroySessionID = null;

	private $notificationURL = null;

	private $propertyName = null;

	private $propertyValue = null;

	private $pattern = null;

	private $uuid = null;

	private static $requestCount = 0;

	/*
	 * Constructors
	 */

	/**
	 * This constructor shall only be used at the client side to construct a
	 * <code>SessionRequest</code> object.
	 *
	 * @param method The method ID of the <code>SessionRequest</code>.
	 * @param sid The session ID required by the <code>SessionRequest</code>.
	 * @param reset The flag to indicate whether this request needs to update
	 *        the latest session access time.
	 */
	public function __construct($method, $sid, $reset) {
		$this->methodID = $method;
		$this->sessionID = $sid;
		$this->resetFlag = $reset;
		$this->requestID = SessionRequest :: $requestCount++;
	}

	/**
	 * Sets the request version.
	 *
	 * @param version Request version.
	 */
	function setRequestVersion($version) {
		$this->requestVersion = $version;
	}

	/**
	 * Returns the request version.
	 *
	 * @return The request version.
	 */
	public function getRequestVersion() {
		return $this->requestVersion;
	}

	/**
	 * Sets the request ID.
	 *
	 * @param id Request ID.
	 */
	function setRequestID($id) {
		$this->requestID = $id;
	}

	/**
	 * Returns the request ID.
	 *
	 * @return The request ID.
	 */
	public function getRequestID() {
		return $this->requestID;
	}

	/**
	 * Sets the method ID.
	 *
	 * @param id Method ID.
	 */
	function setMethodID($id) {
		$this->methodID = $id;
	}

	/**
	 * Returns the method ID.
	 *
	 * @return The method ID.
	 */
	public function getMethodID() {
		return $this->methodID;
	}

	/**
	 * Sets the session ID.
	 *
	 * @param id Session ID.
	 */
	function setSessionID($id) {
		$this->sessionID = $id;
	}

	/**
	 * Returns the session ID.
	 *
	 * @return Session ID.
	 */
	public function getSessionID() {
		return $this->sessionID;
	}

	/**
	 * Sets the requester.
	 *
	 * @param requester Session requester.
	 */
	public function setRequester($requester) {
		$this->requester = $requester;
	}

	/**
	 * Returns the requester
	 *
	 * @return id Session requester.
	 */
	public function getRequester() {
		return $this->requester;
	}

	/**
	 * Sets the reset flag.
	 *
	 * @param reset <code>true</code> to update the latest session access time.
	 */
	function setResetFlag($reset) {
		$this->resetFlag = $reset;
	}

	/**
	 * Returns the reset flag.
	 *
	 * @return The reset flag.
	 */
	public function getResetFlag() {
		return $this->resetFlag;
	}

	/**
	 * Sets the ID of the session to be destroyed.
	 *
	 * @param id The ID of the session to be destroyed.
	 */
	public function setDestroySessionID($id) {
		$this->destroySessionID = $id;
	}

	/**
	 * Returns the ID of the session to be destroyed.
	 *
	 * @return The ID of the session to be destroyed.
	 */
	public function getDestroySessionID() {
		return $this->destroySessionID;
	}

	/**
	 * Sets the notification URL.
	 *
	 * @param url The notification URL.
	 */
	public function setNotificationURL($url) {
		$this->notificationURL = $url;
	}

	/**
	 * Returns the notification URL.
	 *
	 * @return The notification URL.
	 */
	public function getNotificationURL() {
		return $this->notificationURL;
	}

	/**
	 * Sets the property name.
	 *
	 * @param name The property name.
	 */
	public function setPropertyName($name) {
		$this->propertyName = $name;
	}

	/**
	 * Returns the property name.
	 *
	 * @return The property name.
	 */
	public function getPropertyName() {
		return $this->propertyName;
	}

	/**
	 * Sets the property value.
	 *
	 * @param value The property value.
	 */
	public function setPropertyValue($value) {
		$this->propertyValue = $value;
	}

	/**
	 * Returns the property value.
	 *
	 * @return The property value.
	 */
	public function getPropertyValue() {
		return $this->propertyValue;
	}

	/**
	 * Sets the pattern value. Process escape chars in pattern with
	 * <code>CDATA</code>.
	 *
	 * @param value The pattern value.
	 */
	public function setPattern($value) {
		$data = $value;

		if ($value == null) {
			$this->pattern = null;
			return;
		}

		$data = replaceIllegalChar($data, '&', AMPERSAND);
		$data = replaceIllegalChar($data, '\'', APOSTROPHE);
		$data = replaceIllegalChar($data, '\"', QUOTATION);
		$data = replaceIllegalChar($data, '<', LESSTHAN);
		$data = replaceIllegalChar($data, '>', GREATERTHAN);

		$this->pattern = $data;
	}

	/**
	 * Returns the pattern value.
	 *
	 * @return The pattern value.
	 */
	public function getPattern() {
		$data = $this->pattern;

		if ($data == null) {
			return null;
		}

		$data = replaceEntityRef($data, AMPERSAND, '&');
		$data = replaceEntityRef($data, APOSTROPHE, '\'');
		$data = replaceEntityRef($data, QUOTATION, '\"');
		$data = replaceEntityRef($data, LESSTHAN, '<');
		$data = replaceEntityRef($data, GREATERTHAN, '>');

		return $data;
	}

	/**
	 * Sets the universal unique identifier.
	 *
	 * @param id The universal unique identifier.
	 */
	public function setUUID($id) {
		$this->uuid = $id;
	}

	/**
	 * Returns the universal unique identifier
	 *
	 * @return The universal unique identifier
	 */
	public function getUUID() {
		return $this->uuid;
	}

	/**
	 * Replacing illegal XML char with entity ref
	 */
	private function replaceIllegalChar($data, $ch, $replacement) {
		$idx = 0;
		$buffer = "";
		while (($data != null) && ($idx = strpos($data, $ch))) {
			$buffer = $buffer . substr($data, 0, $idx);
			$buffer = $buffer . $replacement;
			$data = substr($data, $idx +1);
		}
		if ($data != null && strlen($data) > 0)
			$buffer = $buffer . $data;

		return $buffer;
	}

	/**
	 * Replacing entity ref with original char
	 */
	private function replaceEntityRef($data, $ref, $ch) {
		$idx = 0;
		$buffer = "";
		while ($idx = strpos($data, $ref)) {
			$buffer = $buffer . substr($data, 0, $idx);
			$buffer = $buffer . $ch;
			$data = substr($data, $idx +strlen($ref));
		}
		if ($data != null && strlen($data) > 0)
			$buffer = $buffer . $data;

		return $buffer;
	}

	/**
	 * This method translates the request to an XML document String based on the
	 * <code>SessionRequest</code> DTD described above. The ID of the session
	 * to be destroyed has to be set for method <code>DestroySession</code>.
	 * The notification URL has to be set for both methods
	 * <code>AddSessionListener</code> and
	 * <code>AddSessionListenerOnAllSessions</code>. otherwise, the returns
	 * <code>null</code>.
	 *
	 * @return An XML String representing the request.
	 */
	public function toXMLString() {
		$xml = "";
		$xml = $xml . "<SessionRequest vers=" . SessionRequest :: QUOTE . $this->requestVersion . SessionRequest :: QUOTE . " reqid=" . SessionRequest :: QUOTE . $this->requestID . SessionRequest :: QUOTE;
		if ($this->requester != null) {
			$data = base64_encode($this->requester);
			$xml = $xml . " requester=" . SessionRequest :: QUOTE . $data . SessionRequest :: QUOTE;
		}
		$xml = $xml . ">" . SessionRequest :: NL;
		switch ($this->methodID) {
			case GetSession :
				$xml = $xml . "<GetSession reset=";
				if ($this->resetFlag)
					$xml = $xml . SessionRequest :: QUOTE . "true" . SessionRequest :: QUOTE . ">" . SessionRequest :: NL;
				else
					$xml = $xml . SessionRequest :: QUOTE . "false" . SessionRequest :: QUOTE . ">" . SessionRequest :: NL;
				$xml = $xml . "<SessionID>" . $this->sessionID->__toString() . "</SessionID>" . SessionRequest :: NL;
				$xml = $xml . "</GetSession>" . SessionRequest :: NL;
				break;
			case GetValidSessions :
				$xml = $xml . "<GetValidSessions>" . SessionRequest :: NL;
				$xml = $xml . "<SessionID>" . $this->sessionID->__toString() . "</SessionID>" . SessionRequest :: NL;
				if ($this->pattern != null)
					$xml = $xml . "<Pattern>" . $this->pattern . "</Pattern>" . SessionRequest :: NL;
				$xml = $xml . "</GetValidSessions>" . SessionRequest :: NL;
				break;
			case DestroySession :
				if ($this->destroySessionID == null)
					return null;

				$xml = $xml . "<DestroySession>" . SessionRequest :: NL;
				$xml = $xml . "<SessionID>" . $this->sessionID->__toString() . "</SessionID>" . SessionRequest :: NL;
				$xml = $xml . "<DestroySessionID>" . $this->destroySessionID . "</DestroySessionID>" . SessionRequest :: NL;
				$xml = $xml . "</DestroySession>" . SessionRequest :: NL;
				break;
			case Logout :
				$xml = $xml . "<Logout>" . SessionRequest :: NL;
				$xml = $xml . "<SessionID>" . $this->sessionID->__toString() . "</SessionID>" . SessionRequest :: NL;
				$xml = $xml . "</Logout>" . SessionRequest :: NL;
				break;
			case AddSessionListener :
				if ($this->notificationURL == null)
					return null;

				$xml = $xml . "<AddSessionListener>" . SessionRequest :: NL;
				$xml = $xml . "<SessionID>" . $this->sessionID->__toString() . "</SessionID>" . SessionRequest :: NL;
				$xml = $xml . "<URL>" . $this->notificationURL . "</URL>" . SessionRequest :: NL;
				$xml = $xml . "</AddSessionListener>" . SessionRequest :: NL;
				break;
			case AddSessionListenerOnAllSessions :
				if ($this->notificationURL == null)
					return null;

				$xml = $xml . "<AddSessionListenerOnAllSessions>" . SessionRequest :: NL;
				$xml = $xml . "<SessionID>" . $this->sessionID->__toString() . "</SessionID>" . SessionRequest :: NL;
				$xml = $xml . "<URL>" . $this->notificationURL . "</URL>" . SessionRequest :: NL;
				$xml = $xml . "</AddSessionListenerOnAllSessions>" . SessionRequest :: NL;
				break;
			case SetProperty :
				if ($this->propertyName == null || $this->propertyValue == null)
					return null;

				$xml = $xml . "<SetProperty>" . SessionRequest :: NL;
				$xml = $xml . "<SessionID>" . $this->sessionID->__toString() . "</SessionID>" . SessionRequest :: NL;
				$xml = $xml . "<Property name=" . SessionRequest :: QUOTE .
				htmlentitites($this->propertyName, ENT_COMPAT, "UTF-8") .
				SessionRequest :: QUOTE . " value=" . SessionRequest :: QUOTE .
				htmlentities($this->propertyValue, ENT_COMPAT, "UTF-8") .
				SessionRequest :: QUOTE . ">" . "</Property>" . SessionRequest :: NL;
				$xml = $xml . "</SetProperty>" . SessionRequest :: NL;
				break;
			case GetSessionCount :
				$xml = $xml . "<GetSessionCount>" . SessionRequest :: NL;
				$xml = $xml . "<SessionID>" . $this->sessionID->__toString() . "</SessionID>" . SessionRequest :: NL;
				$xml = $xml . "<UUID>" . $this->uuid . "</UUID>" . SessionRequest :: NL;
				$xml = $xml . "</GetSessionCount>" . SessionRequest :: NL;
				break;
			default :
				return null;
		}
		$xml = $xml . "</SessionRequest>";
		return $xml;
	}
}
?>
