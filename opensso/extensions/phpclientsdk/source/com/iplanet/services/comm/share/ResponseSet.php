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
 * $Id: ResponseSet.php,v 1.1 2007/03/09 21:13:13 chicchiricco Exp $
 *
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 */


/**
 * This <code>ResponseSet</code> class represents a ResponseSet XML document.
 * The ResponseSet DTD is defined as the following:
 * </p>
 *
 * <pre>
 *     &lt;?xml version=&quot;1.0&quot;&gt;
 *     &lt; !-- This DTD is used by PLL --&gt;
 *     &lt; !DOCTYPE ResponseSet [
 *     &lt; !ELEMENT ResponseSet(Response)+&gt;
 *     &lt; !ATTLIST ResponseSet
 *       vers  CDATA #REQUIRED
 *       svcid CDATA #REQUIRED
 *       reqid CDATA #REQUIRED&gt;
 *     &lt; !ELEMENT Response(#PCDATA)*&gt;
 *     &lt; !ATTLIST Response
 *       dtdid CDATA #IMPLIED&gt;
 *     ]&gt;
 * </pre>
 *
 * </p>
 * Each ResponseSet object contains a version, service ID, response set ID, the
 * original request set ID, and a collection of Response objects. The
 * ResponseSet views each Response object as a String. This makes it possible
 * that the content of the Response object can be another XML document. The PLL
 * provides a reference Response DTD. Please see class Response for details on
 * the Response DTD. This class also provides a method to aggregate each
 * Response object and returns a ResponseSet XML document based on the
 * ResponseSet DTD mentioned above.
 *
 * @see com.iplanet.services.comm.share.Response
 */
class ResponseSet {

	const QUOTE = "\"";

	const NL = "\n";

	const BEGIN_CDATA = "<![CDATA[";

	const END_CDATA = "]]>";

	private $responseSetVersion = null;

	private $serviceID = null;

	private $requestSetID = null;

	private $responseVector = array ();

	/**
	 * This constructor is used primarily at the server side to construct a
	 * ResponseSet object for a given service. Individual response shall be
	 * added to this object by calling addResponse method.
	 *
	 * @param service
	 *            The name of the service.
	 */
	public function __construct($service) {
		$this->serviceID = $service;
		$this->responseSetVersion = "1.0";
	}

	/**
	 * This method is used primarily at the client side to reconstruct a
	 * ResponseSet object based on the XML document received from server. The
	 * DTD of this XML document is described above.
	 *
	 * @param xml
	 *            The ResponseSet XML document String.
	 */
	public static function parseXML($xml) {
		// Parse the XML document and extract the XML objects out of the
		// XML document
		$parser = new ResponseSetParser($xml);
		return $parser->parseXML();
	}

	/**
	 * Sets the original RequestSet ID for this object.
	 *
	 * @param id The original RequestSet ID.
	 */
	public function setRequestSetID($id) {
		$this->requestSetID = $id;
	}

	/**
	 * Gets the Response objects contained in this object.
	 *
	 * @return A Vector of Response objects.
	 */
	public function getResponses() {
		return $this->responseVector;
	}

	/**
	 * Adds a Response object to this object.
	 *
	 * @param response A reference to a Response object.
	 */
	public function addResponse(Response $response) {
		$this->responseVector[] = $response;
	}

	/**
	 * Returns an XML ResponseSet document in String format. The returned String
	 * is formatted based on the ResponseSet DTD by aggregating each Response
	 * object in this object.
	 *
	 * @return An XML ResponseSet document in String format.
	 */
	public function toXMLString() {
		$xml = "<?xml version=" . ResponseSet :: QUOTE . "1.0" . ResponseSet :: QUOTE .
		" encoding=" . ResponseSet :: QUOTE . "UTF-8" .
		ResponseSet :: QUOTE . " standalone=" . ResponseSet :: QUOTE .
		"yes" . ResponseSet :: QUOTE . "?>" . ResponseSet :: NL;

		$xml .= "<ResponseSet vers=" . ResponseSet :: QUOTE .
		$this->responseSetVersion . ResponseSet :: QUOTE . " svcid=" .
		ResponseSet :: QUOTE . $this->serviceID . ResponseSet :: QUOTE . " reqid=" .
		ResponseSet :: QUOTE . $this->requestSetID . ResponseSet :: QUOTE . ">" . ResponseSet :: NL;

		for ($i = 0; $i < count($this->responseVector); $i++) {
			$res = $this->responseVector[i];
			$xml .= "<Response";
			if ($res->getDtdID() != null)
				$xml .= " dtdid=" . ResponseSet :: QUOTE . $res->getDtdID() . ResponseSet :: QUOTE;
			$xml .= ">";
			$xml .= ResponseSet :: BEGIN_CDATA . $res->getContent() . ResponseSet :: END_CDATA;
			$xml .= "</Response>" . ResponseSet :: NL;
		}
		$xml .= "</ResponseSet>";
		return $xml;
	}

	/*
	 * The following methods are used by ResponseParser to reconstruct a
	 * ResponseSet object.
	 */
	function setResponseSetVersion($ver) {
		$this->responseSetVersion = $ver;
	}

	function setServiceID($id) {
		$this->serviceID = $id;
	}
}
?>
