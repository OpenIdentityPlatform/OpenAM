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
 * $Id: NamingRequest.php,v 1.1 2007/03/09 21:13:14 chicchiricco Exp $
 *
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 */


/**
 * This <code>NamingRequest</code> class represents a NamingRequest XML
 * document. The NamingRequest DTD is defined as the following:
 * </p>
 *
 * <pre>
 *     &lt;?xml version=&quot;1.0&quot;&gt;
 *     &lt; !DOCTYPE NamingRequest [
 *     &lt; !ELEMENT NamingRequest (GetNamingProfile)&gt;
 *     &lt; !ATTLIST NamingRequest
 *       vers   CDATA #REQUIRED
 *       reqid  CDATA #REQUIRED
 *       sessid CDATA #REQUIRED&gt;
 *     &lt; !ELEMENT GetNamingProfile EMPTY&gt;
 *     ]&gt;
 * </pre>
 *
 * </p>
 */
class NamingRequest {

	const QUOTE = "\"";

	const NL = "\n";

	private $requestVersion = null;

	private $requestID = null;

	private $sessionId = null;

	private static $requestCount = 0;

	const reqVersion = "2.0";

	/**
	 * This constructor shall only be used at the client side to construct a
	 * NamingRequest object.
	 *
	 * @param ver
	 *            The naming request version.
	 */
	public function __construct($version) {
		$this->requestVersion = ($version <= 1.0) ? NamingRequest :: $reqVersion : $version;
		$this->requestID = NamingRequest :: $requestCount++;
	}

	/**
	 * This method is used primarily at the server side to reconstruct a
	 * NamingRequest object based on the XML document received from client. The
	 * DTD of this XML document is described above.
	 *
	 * @param xml
	 *            The NamingRequest XML document String.
	 */
	//    public static function parseXML($xml) {
	//        NamingRequestParser parser = new NamingRequestParser(xml);
	//        return parser.parseXML();
	//    }

	/**
	 * Sets the request version.
	 *
	 * @param version
	 *            A string representing the request version.
	 */
	function setRequestVersion($version) {
		$this->requestVersion = $version;
	}

	/**
	 * Gets the request version.
	 *
	 * @return The request version.
	 */
	public function getRequestVersion() {
		return $this->requestVersion;
	}

	/**
	 * Sets the request ID.
	 *
	 * @param id
	 *            A string representing the request ID.
	 */
	function setRequestID($id) {
		$this->requestID = $id;
	}

	/**
	 * Gets the request ID.
	 *
	 * @return The request ID.
	 */
	public function getRequestID() {
		return $this->requestID;
	}

	/**
	 * Sets the session ID.
	 *
	 * @param id
	 *            A string representing the session ID.
	 */
	function setSessionId($id) {
		$this->sessionId = $id;
	}

	/**
	 * Gets the session ID.
	 *
	 * @return The session ID.
	 */
	public function getSessionId() {
		return $this->sessionId;
	}

	/**
	 * This method translates the request to an XML document String based on the
	 * NamingRequest DTD described above.
	 *
	 * @return An XML String representing the request.
	 */
	public function toXMLString() {
		$xml = "<NamingRequest vers=" . NamingRequest :: QUOTE . $this->requestVersion .
		NamingRequest :: QUOTE . " reqid=" . NamingRequest::QUOTE .
		$this->requestID . NamingRequest :: QUOTE . ">" . NamingRequest :: NL .
		"<GetNamingProfile>" . NamingRequest :: NL .
		"</GetNamingProfile>" . NamingRequest :: NL .
		"</NamingRequest>";

		return $xml;
	}
}
?>
