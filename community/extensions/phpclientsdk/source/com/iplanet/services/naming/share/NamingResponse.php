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
 * $Id: NamingResponse.php,v 1.1 2007/03/09 21:13:14 chicchiricco Exp $
 *
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 */


/**
 * This <code>NamingResponse</code> class represents a NamingResponse XML
 * document. The NamingResponse DTD is defined as the following:
 * </p>
 *
 * <pre>
 *     &lt;?xml version=&quot;1.0&quot;&gt;
 *     &lt; !DOCTYPE NamingResponse [
 *     &lt; !ELEMENT NamingResponse (GetNamingProfile)&gt;
 *     &lt; !ATTLIST NamingResponse
 *       vers   CDATA #REQUIRED
 *       reqid  CDATA #REQUIRED&gt;
 *     &lt; !ELEMENT GetNamingProfile (Attribute*|Exception)&gt;
 *     &lt; !ELEMENT Attribute EMPTY&gt;
 *     &lt; !ATTLIST Attribute
 *       name   CDATA #REQUIRED
 *       value  CDATA #REQUIRED&gt;
 *     &lt; !ELEMENT Exception (#PCDATA)&gt;
 *     ]&gt;
 * </pre>
 *
 * </p>
 */
class NamingResponse {

	const QUOTE = "\"";

	const NL = "\n";

	private $responseVersion = "1.0";

	private $requestID = null;

	private $namingTable = array ();

	private $exception = null;

	/**
	 * This constructor shall only be used at the server side to construct a
	 * NamingResponse object.
	 *
	 * @param reqid
	 *            The original request ID.
	 */
	public function __construct($reqid) {
		$this->requestID = $reqid;
	}

	/**
	 * This method is used primarily at the client side to reconstruct a
	 * NamingResponse object based on the XML document received from server. The
	 * DTD of this XML document is described above.
	 *
	 * @param xml
	 *            The NamingResponse XML document String.
	 */
	public static function parseXML($xml) {
		$parser = new NamingResponseParser($xml);
		return $parser->parseXML();
	}

	/**
	 * Sets the response version.
	 *
	 * @param version
	 *            A string representing the response version.
	 */
	function setResponseVersion($version) {
		$this->responseVersion = $version;
	}

	/**
	 * Gets the response version.
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
	 * Gets the request ID.
	 *
	 * @return The request ID.
	 */
	public function getRequestID() {
		return $this->requestID;
	}

	/**
	 * Sets the naming attribute.
	 *
	 * @param name
	 *            attribute name.
	 * @param value
	 *            attribute value.
	 */
	public function setAttribute($name, $value) {
		$this->namingTable[$name] = $value;
	}

	/**
	 * Gets the attribute.
	 *
	 * @param name
	 *            attribute name.
	 * @return the attribute value.
	 */
	public function getAttribute($name) {
		return $this->namingTable[$name];
	}

	/**
	 * Gets the naming table.
	 */
	public function getNamingTable() {
		return $this->namingTable;
	}

	/**
	 * Sets the naming table.
	 */
	public function setNamingTable($table) {
		$this->namingTable = $table;
	}

	/**
	 * Sets the exception.
	 *
	 * @param id
	 *            A string representing the exception.
	 */
	public function setException($ex) {
		$this->exception = $ex;
	}

	/**
	 * Gets the exception.
	 *
	 * @return The exception.
	 */
	public function getException() {
		return $this->exception;
	}

	/**
	 * This method translates the response to an XML document String based on
	 * the NamingResponse DTD described above.
	 *
	 * @return An XML String representing the response.
	 */
	public function toXMLString() {
		$xml = "<NamingResponse vers=" . NamingResponse :: QUOTE .
		responseVersion . NamingResponse :: QUOTE . " reqid=" . NamingResponse :: QUOTE .
		$this->requestID . NamingResponse :: QUOTE . ">" . NamingResponse :: NL;
		$xml .= "<GetNamingProfile>" . NamingResponse :: NL;
		if ($this->exception != null)
			$xml .= "<Exception>" . $this->exception . "</Exception>" . NamingResponse :: NL;
		else
			foreach ($this->namingTable as $name => $value)
				$xml .= "<Attribute name=" . NamingResponse :: QUOTE . $name .
				NamingResponse :: QUOTE . " value=" . NamingResponse :: QUOTE .
				$value . NamingResponse :: QUOTE . ">" .
				"</Attribute>" . NamingResponse :: NL;

		$xml .= "</GetNamingProfile>" . NamingResponse :: NL;
		$xml .= "</NamingResponse>";

		return xml . toString();
	}
}
?>
