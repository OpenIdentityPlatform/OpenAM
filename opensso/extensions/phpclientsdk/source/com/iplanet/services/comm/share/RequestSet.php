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
 * $Id: RequestSet.php,v 1.1 2007/03/09 21:13:12 chicchiricco Exp $
 *
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 */


/**
 * This <code>RequestSet</code> class represents a RequestSet XML document.
 * The RequestSet DTD is defined as the following:
 * </p>
 *
 * <pre>
 *    &lt;?xml version=&quot;1.0&quot;&gt;
 *    &lt; !-- This DTD is used by PLL --&gt;
 *    &lt; !DOCTYPE RequestSet [
 *    &lt; !ELEMENT RequestSet(Request)+&gt;
 *    &lt; !ATTLIST RequestSet
 *       vers  CDATA #REQUIRED
 *       svcid CDATA #REQUIRED
 *       reqid CDATA #REQUIRED&gt;
 *    &lt; !ELEMENT Request(#PCDATA)*&gt;
 *    &lt; !ATTLIST Request
 *       dtdid CDATA #IMPLIED
 *       sid   CDATA #IMPLIED&gt;
 *    ]&gt;
 * </pre>
 *
 * </p>
 * Each RequestSet object contains a version, service ID, request set ID, and a
 * collection of Request objects. The RequestSet views each Request object as a
 * String. This makes it possible that the content of the Request object can be
 * another XML document. The PLL provides a reference Request DTD. Please see
 * class Request for details on the Request DTD. This class also provides a
 * method to aggregate each Request object and returns a RequestSet XML document
 * based on the RequestSet DTD mentioned above.
 *
 * @see com.iplanet.services.comm.share.Request
 */

class RequestSet {

	const QUOTE = "\"";

	const NL = "\n";

	const BEGIN_CDATA = "<![CDATA[";

	const END_CDATA = "]]>";

	private $requestSetVersion = null;

	private $serviceID = null;

	private $requestSetID = null;

	private $requestVector = array ();

	private static $requestCount = 0;

	/**
	 * This constructor is used primarily at the client side to construct a
	 * RequestSet object for a given service. Individual request shall be added
	 * to this object by calling addRequest method. service.
	 *
	 * @param service
	 *            The name of the service.
	 */
	public function __construct($service) {
		$this->serviceID = $service;
		$this->requestSetVersion = "1.0";
		$this->requestSetID = RequestSet :: $requestCount++;
	}

	/**
	 * This method is used primarily at the server side to reconstruct a
	 * RequestSet object based on the XML document received from client. The DTD
	 * of this XML document is described above.
	 *
	 * @param xml
	 *            The RequestSet XML document String.
	 */
	//    public static RequestSet parseXML(String xml) {
	//        // Parse the XML document and extract the XML objects out of the
	//        // XML document
	//        RequestSetParser parser = new RequestSetParser(xml);
	//        return parser.parseXML();
	//    }

	/**
	 * Gets the version of the RequestSet.
	 *
	 * @return The version of the request.
	 */
	public function getRequestSetVersion() {
		return $this->requestSetVersion;
	}

	/**
	 * Gets the service ID of the RequestSet.
	 *
	 * @return The service ID of the RequestSet.
	 */
	public function getServiceID() {
		return $this->serviceID;
	}

	/**
	 * Gets the RequestSet ID for this object.
	 *
	 * @return The RequestSet ID.
	 */
	public function getRequestSetID() {
		return $this->requestSetID;
	}

	/**
	 * Gets the Request objects contained in this object.
	 *
	 * @return A Vector of Request objects.
	 */
	public function getRequests() {
		return $this->requestVector;
	}

	/**
	 * Adds a Request object to this object.
	 *
	 * @param request
	 *            A reference to a Request object.
	 */
	public function addRequest($request) {
		$this->requestVector[] = $request;
	}

	/**
	 * Returns an XML RequestSet document in String format. The returned String
	 * is formatted based on the RequestSet DTD by aggregating each Request
	 * object in this object.
	 *
	 * @return An XML RequestSet document in String format.
	 */
	public function toXMLString() {
		$xml = "<?xml version=" . RequestSet :: QUOTE . "1.0" . RequestSet :: QUOTE .
		" encoding=" . RequestSet :: QUOTE . "UTF-8" .
		RequestSet :: QUOTE . " standalone=" . RequestSet :: QUOTE .
		"yes" . RequestSet :: QUOTE . "?>" . RequestSet :: NL .
		"<RequestSet vers=" . RequestSet :: QUOTE . $this->requestSetVersion .
		RequestSet :: QUOTE . " svcid=" . RequestSet :: QUOTE .
		$this->serviceID . RequestSet :: QUOTE . " reqid=" .
		RequestSet :: QUOTE . $this->requestSetID . RequestSet :: QUOTE . ">" .
		RequestSet :: NL;

		for ($i = 0; $i < count($this->requestVector); $i++) {
			$req = $this->requestVector[$i];
			$xml .= "<Request";
			if ($req->getDtdID() != null)
				$xml .= " dtdid=" . RequestSet :: QUOTE . $req->getDtdID() . RequestSet :: QUOTE;
			if ($req->getSessionID() != null)
				$xml .= " sid=" . RequestSet :: QUOTE . $req->getSessionID() . RequestSet :: QUOTE;
			$xml .= ">";
			$xml .= RequestSet :: BEGIN_CDATA . $req->getContent() . RequestSet :: END_CDATA;
			$xml .= "</Request>" . RequestSet :: NL;
		}
		$xml .= "</RequestSet>";
		return $xml;
	}

	/*
	 * The following methods are used by the RequestSetParser to reconstruct a
	 * RequestSet object.
	 */
	function setRequestSetVersion($vers) {
		$this->requestSetVersion = $vers;
	}

	function setServiceID($id) {
		$this->serviceID = $id;
	}

	function setRequestSetID($id) {
		$this->requestSetID = $id;
	}
}
?>
