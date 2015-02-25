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
 * $Id: NamingResponseParser.php,v 1.1 2007/03/09 21:13:15 chicchiricco Exp $
 *
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 */

class NamingResponseParser {

	/**
	 * NamingResponse object being returned after parsing the XML document
	 */
	private $namingResponse = null;

	/**
	 * Document to be parsed
	 */
	private $document;

	public function __construct($xmlString) {
		$this->document = DomDocument::loadXML($xmlString);
	}

	/**
	 * Parses the response document. Please see file NamingResponse.dtd for the
	 * corresponding DTD of the NamingResponse.
	 *
	 * @return a NamingResponse object.
	 */
	public function parseXML() {
		if ($this->document == null)
			return null;

		// get NamingResponse element
		$elem = $this->document->documentElement;
		$this->namingResponse = new NamingResponse(null);

		// set naming response attributes
		$temp = $elem->getAttribute("vers");
		if ($temp != null)
			$this->namingResponse->setResponseVersion($temp);

		$temp = $elem->getAttribute("reqid");
		if ($temp != null)
			$this->namingResponse->setRequestID($temp);

		// get attribute element
		$attribs = $elem->getElementsByTagName("Attribute");
		if ($attribs != null && $attribs->length != 0)
			$this->parseAttributeTag($attribs);

		// get exception element
		$exception = $elem->getElementsByTagName("Exception");
		if ($exception != null && $exception->length != 0) {
			$node = $exception->item(0);
			if ($node != null)
				$this->namingResponse->setException($node->nodeValue);
		}

		return $this->namingResponse;
	}

	/**
	 * This method is an internal method used by parseXML method to parse
	 * Attribute.
	 *
	 * @param attributes XML Node for attributes.
	 */
	public function parseAttributeTag($attributes) {
		foreach ($attributes as $tempElem) {
			// get node name & value
			$name = $tempElem->getAttribute("name");
			if ($name != null) {
				$value = $tempElem->getAttribute("value");
				$this->namingResponse->setAttribute($name, $value);
			}
		}
	}
}
?>
