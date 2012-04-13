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
 * $Id: SessionResponse.php,v 1.1 2007/03/09 21:13:11 chicchiricco Exp $
 *
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 */


/**
 * This <code>SessionResponse</code> class represents a
 * <code>SessionResponse</code> XML document. The <code>SessionResponse</code>
 * DTD is defined as the following:
 * </p>
 *
 * <pre>
 *     &lt;?xml version=&quot;1.0&quot;&gt;
 *     &lt; !DOCTYPE SessionResponse [
 *     &lt; !ELEMENT SessionResponse(GetSession |
 *                                    GetValidSessions |
 *                                    DestroySession |
 *                                    Logout |
 *                                    AddSessionListener |
 *                                    AddSessionListenerOnAllSessions |
 *                                    SetProperty |
 *                                    GetSessionCount)&gt;
 *     &lt; !ATTLIST SessionResponse
 *         vers   CDATA #REQUIRED
 *         reqid  CDATA #REQUIRED&gt;
 *     &lt; !ELEMENT GetSession (Session|Exception)&gt;
 *     &lt; !ELEMENT GetValidSessions ((SessionList Status)|Exception)&gt;
 *     &lt; !ELEMENT DestroySession (OK|Exception)&gt;
 *     &lt; !ELEMENT Logout (OK|Exception)&gt;
 *     &lt; !ELEMENT AddSessionListener (OK|Exception)&gt;
 *     &lt; !ELEMENT AddSessionListenerOnAllSessions (OK|Exception)&gt;
 *     &lt; !ELEMENT SetProperty (OK|Exception)&gt;
 *     &lt; !ELEMENT GetSessionCount (AllSessionsGivenUUID|Exception)&gt;
 *     &lt; !ELEMENT SessionExpirationTimeInfo&gt;
 *     &lt; !ATTLIST SessionExpirationTimeInfo
 *         sid        CDATA #REQUIRED
 *         expTime    CDATA #REQUIRED&gt;
 *     &lt; !ELEMENT AllSessionsGivenUUID (SessionExpirationTimeInfo)*&gt;
 *     &lt; !ELEMENT Session (Property)*&gt;
 *     &lt; !ATTLIST Session
 *         sid        CDATA #REQUIRED
 *         stype      (user|application) &quot;user&quot;
 *         cid        CDATA #REQUIRED
 *         cdomain    CDATA #REQUIRED
 *         maxtime    CDATA #REQUIRED
 *         maxidle    CDATA #REQUIRED
 *         maxcaching CDATA #REQUIRED
 *         timeleft   CDATA #REQUIRED
 *         timeidle   CDATA #REQUIRED
 *         state   (invalid|valid|inactive|destroyed) &quot;invalid&quot;&gt;
 *     &lt; !ELEMENT Property&gt;
 *     &lt; !ATTLIST Property
 *         name   CDATA #REQUIRED
 *         value  CDATA #REQUIRED&gt;
 *     &lt; !ELEMENT SessionList (Session)*&gt;
 *     &lt; !ELEMENT OK (#PCDATA)&gt;
 *     &lt; !ELEMENT Exception (#PCDATA)&gt;
 *     &lt; !ELEMENT Status (#PCDATA)&gt;
 *     ]&gt;
 * </pre>
 *
 * </p>
 */
class SessionResponse {

	const QUOTE = "\"";

    const NL = "\n";

    private $responseVersion = "1.0";

	private $requestID = null;

	private $methodID;

	// For GetSessionCount
	private $allSessionsforGivenUUID = array ();

	// For GetSession and GetValidSessions
	private $sessionInfoVector = array ();

	// For DestroySession, Logout, AddSessionListener and
	// AddSessionListenerOnAllSessions
	private $booleanFlag = false;

	private $exception = null;

	//private $status = SearchResults.UNDEFINED_RESULT_COUNT;

	/**
	 * This constructor shall only be used at the server side to construct a
	 * <code>SessionResponse</code> object.
	 *
	 * @param reqid The original <code>SessionRequest</code> ID.
	 * @param method The method ID of the original Session Request.
	 */
	public function __construct($reqid, $method) {
		$this->requestID = $reqid;
		$this->methodID = $method;
	}

	/**
	 * This method is used primarily at the client side to reconstruct a
	 * <code>SessionResponse</code> object based on the XML document received
	 * from server. The DTD of this XML document is described above.
	 *
	 * @param xml The <code>SessionResponse</code> XML document.
	 */
	public static function parseXML($xml) {
		$parser = new SessionResponseParser($xml);
		return $parser->parseXML();
	}

	/**
	 * Sets the response version.
	 *
	 * @param version Response version.
	 */
	function setResponseVersion($version) {
		$this->responseVersion = $version;
	}

	/**
	 * Returns the response version.
	 *
	 * @return The response version.
	 */
	public function getResponseVersion() {
		return $this->responseVersion;
	}

	/**
	 * Sets the request ID.
	 *
	 * @param id
	 *            A string representing the original request ID.
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
	 * @param id
	 *            A integer representing the method ID.
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
	 * Adds a <code>SessionInfo</code> object.
	 */
	public function addSessionInfo($info) {
		$this->sessionInfoVector[] = $info;
	}

	/**
	 * Returns the <code>SessionInfo</code>.
	 *
	 * @return vector containing the session information
	 */
	public function getSessionInfoVector() {
		return $this->sessionInfoVector;
	}

	/**
	 * Sets the <code>SessionInfo</code>.
	 * @param infos vector containing the session information.
	 */
	public function setSessionInfoVector($infos) {
		$this->sessionInfoVector = $infos;
	}

	/**
	 * Sets the flag.
	 *
	 * @param flag
	 */
	public function setBooleanFlag($flag) {
		$this->booleanFlag = $flag;
	}

	/**
	 * Returns the flag.
	 * @return flag <code>true</code> if the flag is set,<code> false</code>
	 * otherwise
	 */
	public function getBooleanFlag() {
		return $this->booleanFlag;
	}

	/**
	 * Adds the Session Information for a User.
	 *
	 * @param sid Session ID.
	 * @param expTime time when the session would expire.
	 */
	public function addSessionForGivenUUID($sid, $expTime) {
		$this->allSessionsforGivenUUID[$sid] = $expTime;
	}

	/**
	 * Sets the Sessions.
	 *
	 * @param sessions number for sessions for the user.
	 */
	public function setSessionsForGivenUUID($sessions) {
		$this->allSessionsforGivenUUID = $sessions;
	}

	/**
	* Returns the Session Information for a User.
	*
	* @return list sessions for the user
	*/
	public function getSessionsForGivenUUID() {
		return $this->allSessionsforGivenUUID;
	}

	/**
	 * Returns the exception.
	 *
	 * @return The exception.
	 */
	public function getException() {
		return $this->exception;
	}

	/**
	 * Sets the exception.
	 *
	 * @param ex Exception.
	 */
	public function setException($ex) {
		$this->exception = $ex;
	}

	/**
	 * Returns the status.
	 *
	 * @return The status.
	 */
	public function getStatus() {
		return $this->status;
	}

	/**
	 * Sets the status.
	 *
	 * @param value Status.
	 */
	public function setStatus($value) {
		$this->status = $value;
	}

   /**
     * Translates the response to an XML document String based on
     * the <code>SessionResponse</code> DTD described above.
     *
     * @return An XML String representing the response.
     */
    public function toXMLString() {
        $xml = "<SessionResponse vers=" . SessionResponse::QUOTE . $this->responseVersion . SessionResponse::QUOTE
                . " reqid=" . SessionResponse::QUOTE . $this->requestID . SessionResponse::QUOTE . ">" . SessionResponse::NL;
        switch ($this->methodID) {
        case SessionRequest::GetSession:
            $xml .= "<GetSession>" . SessionResponse::NL;
            if ($this->exception != null)
                $xml .= "<Exception>" . $this->exception . "</Exception>" . SessionResponse::NL;
            else {
                if (count($this->sessionInfoVector) != 1)
                    return null;

                $info = $this->sessionInfoVector[0];
                $xml .= $info->toXMLString();
            }
            $xml .= "</GetSession>" . SessionResponse::NL;
            break;
        case SessionRequest::GetValidSessions:
            $xml .= "<GetValidSessions>" . SessionResponse::NL;
            if ($this->exception != null)
                $xml .= "<Exception>" . $this->exception . "</Exception>" . SessionResponse::NL;
            else {
                $xml .= "<SessioSessionResponse::NList>" . SessionResponse::NL;
                foreach($this->sessionInfoVector as $info)
                    $xml .= $info->toXMLString();

                $xml .= "</SessioSessionResponse::NList>" . SessionResponse::NL;
                $xml .= "<Status>" . $this->status . "</Status>" . SessionResponse::NL;
            }
            $xml .= "</GetValidSessions>" . SessionResponse::NL;
            break;
        case SessionRequest::DestroySession:
            $xml .= "<DestroySession>" . SessionResponse::NL;
            if ($this->exception != null)
                $xml .= "<Exception>" . $this->exception . "</Exception>" . SessionResponse::NL;
            else
                $xml .= "<OK></OK>" . SessionResponse::NL;

            $xml .= "</DestroySession>" . SessionResponse::NL;
            break;
        case SessionRequest::Logout:
            $xml .= "<Logout>" . SessionResponse::NL;
            if ($this->exception != null)
                $xml .= "<Exception>" . $this->exception . "</Exception>" . SessionResponse::NL;
            else
                $xml .= "<OK></OK>" . SessionResponse::NL;

            $xml .= "</Logout>" . SessionResponse::NL;
            break;
        case SessionRequest::AddSessionListener:
            $xml .= "<AddSessionListener>" . SessionResponse::NL;
            if ($this->exception != null)
                $xml .= "<Exception>" . $this->exception . "</Exception>" . SessionResponse::NL;
            else
                $xml .= "<OK></OK>" . SessionResponse::NL;

            $xml .= "</AddSessionListener>" . SessionResponse::NL;
            break;
        case SessionRequest::AddSessionListenerOnAllSessions:
            $xml .= "<AddSessionListenerOnAllSessions>" . SessionResponse::NL;
            if ($this->exception != null)
                $xml .= "<Exception>" . $this->exception . "</Exception>" . SessionResponse::NL;
            else
                $xml .= "<OK></OK>" . SessionResponse::NL;

            $xml .= "</AddSessionListenerOnAllSessions>" . SessionResponse::NL;
            break;
        case SessionRequest::SetProperty:
            $xml .= "<SetProperty>" . SessionResponse::NL;
            if ($this->exception != null)
                $xml .= "<Exception>" . $this->exception . "</Exception>" . SessionResponse::NL;
            else
                $xml .= "<OK></OK>" . SessionResponse::NL;

            $xml .= "</SetProperty>" . SessionResponse::NL;
            break;
        case SessionRequest::GetSessionCount:
            $xml .= "<GetSessionCount>" . SessionResponse::NL;
            if ($this->exception != null)
                $xml .= "<Exception>" . $this->exception . "</Exception>" . SessionResponse::NL;
            else {
                $xml .= "<AllSessionsGivenUUID>" . SessionResponse::NL;
                foreach ($this->allSessionsforGivenUUID as $sid => $sessions)
                    $xml .= "<SessionExpirationTimeInfo sid=" . SessionResponse::QUOTE .
                             $sid . SessionResponse::QUOTE . " expTime=" . SessionResponse::QUOTE .
                             $sessions . 							SessionResponse							::QUOTE . ">" . "</SessionExpirationTimeInfo>";

                $xml .= "</AllSessionsGivenUUID>" . SessionResponse::NL;
            }
            $xml .= "</GetSessionCount>" . SessionResponse::NL;
            break;
        default:
            return null;
        }
        $xml .= "</SessionResponse>";
        return $xml;
    }

}
?>
