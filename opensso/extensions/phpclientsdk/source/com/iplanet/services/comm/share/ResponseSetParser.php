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
 * $Id: ResponseSetParser.php,v 1.1 2007/03/09 21:13:13 chicchiricco Exp $
 *
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 */

class ResponseSetParser {

	/**
	 * Document being parsed
	 */
	private $document;

	public function __construct($xmlString) {
		$this->document = DomDocument::loadXML($xmlString);
		//echo $xmlString;
	}

	/**
	 * Parses the tree from root element. Please see ResponseSet.php for the
	 * corresponding DTD of the ResponseSet.
	 *
	 * @return a ResponseSet object.
	 */
	public function parseXML() {
		if ($this->document == null)
			return null;

		$responseSet = new ResponseSet(null);
		// set response set attributes
		$this->setResponseSetAttributes($this->document->documentElement, $responseSet);

		// get Responses
		$responses = $this->document->getElementsByTagName("Response");
		if ($responses == null)
			return $responseSet;

		// go through each response, and add them to the response set
		foreach ($responses as $response)
			$responseSet->addResponse($this->parseResponseElement($response));

		return $responseSet;
	}

	/**
	 * This method is an internal method used by parseXML method.
	 *
	 * @param elem XML element object.
	 * @param responseSet Response Set.
	 */
	public function setResponseSetAttributes($elem, ResponseSet $responseSet) {
		// get vers attribute
		$temp = $elem->getAttribute("vers");
		if ($temp != null)
			$responseSet->setResponseSetVersion($temp);

		// get service id
		$temp = $elem->getAttribute("svcid");
		if ($temp != null)
			$responseSet->setServiceID($temp);

		// get request id
		$temp = $elem->getAttribute("reqid");
		if ($temp != null)
			$responseSet->setRequestSetID($temp);
	}

	/**
	 * function to parse a single response element. Response contain a text
	 * element and dtdid attribute
	 */
	private function parseResponseElement($elem) {
		$response = new Response(null);
		// process request attributes
		$temp = $elem->getAttribute("dtdid");
		if ($temp != null)
			$response->setDtdID($temp);

		// process TEXT child element
		$text = $elem->firstChild;
		if ($text != null)
			$response->setContent($text->data);

		return $response;
	}
}
?>
