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
 * $Id: Response.php,v 1.1 2007/03/09 21:13:12 chicchiricco Exp $
 *
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 */

/**
 * This <code>Response</code> class represents a response. The most important
 * information in this Response object is the content of this response. The
 * content in this Response object can be an arbitrary String. This makes it
 * possible that high level services and applications can define their own
 * response XML DTDs and then embed the corresponding XML document into this
 * Response object as its content.
 *
 * @see com.iplanet.services.comm.share.ResponseSet
 */
class Response {

	private $dtdID = null;

	private $responseContent = "";

	/**
	 * Contructs an instance of Response class with the content of the Response.
	 * The DTD ID needs to be set explicitly using the corresponding setter as
	 * it is optional for the response.
	 *
	 * @param content
	 *            The content of this Response.
	 */
	public function __construct($content) {
		$this->responseContent = $content;
	}

	/**
	 * Gets the ID of the DTD for the content of the Response
	 *
	 * @return The ID of the DTD for the content of the Response.
	 */
	public function getDtdID() {
		return $this->dtdID;
	}

	/**
	 * Gets the content of the Response.
	 *
	 * @return The content of the Response.
	 */
	public function getContent() {
		return $this->responseContent;
	}

	/**
	 * Sets the ID of the DTD for the content of the Response
	 *
	 * @param id The ID of the DTD for the content of the Response.
	 */
	public function setDtdID($id) {
		$this->dtdID = $id;
	}

	/**
	 * Sets the content of the Response.
	 *
	 * @param content The content of the Response in String format.
	 */
	public function setContent($content) {
		$this->responseContent = $content;
	}

}
?>
