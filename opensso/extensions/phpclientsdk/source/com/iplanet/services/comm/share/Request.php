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
 * $Id: Request.php,v 1.1 2007/03/09 21:13:12 chicchiricco Exp $
 *
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 */


/**
 * This <code>Request</code> class represents a request. The most important
 * information in this Request object is the content of this request. The
 * content in this Request object can be an arbitrary String. This makes it
 * possible that high level services and applications can define their own
 * request XML DTDs and then embed the corresponding XML document into this
 * Request object as its content.
 *
 * @see com.iplanet.services.comm.share.RequestSet
 */
class Request {

	private $dtdID = null;

	private $sessionID = null;

	private $requestContent = "";

	/**
	 * Contructs an instance of Request class with the content of the Request.
	 * The session ID and DTD ID need to be set explicitly using corresponding
	 * setters as those are optional for the request.
	 *
	 * @param content
	 *            The content of this Request.
	 */
	public function __construct($content) {
		$this->requestContent = $content;
	}

	/**
	 * Sets the ID of the DTD for the content of the Request
	 *
	 * @param id
	 *            The ID of the DTD for the content of the Request.
	 */
	public function setDtdID($id) {
		$this->dtdID = $id;
	}

	/**
	 * Gets the ID of the DTD for the content of the Request
	 *
	 * @return The ID of the DTD for the content of the Request.
	 */
	public function getDtdID() {
		return $this->dtdID;
	}

	/**
	 * Sets the session ID of the request.
	 *
	 * @param id
	 *            A string representing the session ID of the request.
	 */
	public function setSessionID($id) {
		$this->sessionID = $id;
	}

	/**
	 * Gets the session ID of the request.
	 *
	 * @return The session ID of the request.
	 */
	public function getSessionID() {
		return $this->sessionID;
	}

	/**
	 * Gets the content of the Request.
	 *
	 * @return The content of the Request.
	 */
	public function getContent() {
		return $this->requestContent;
	}

	/**
	 * Sets the content of the Request.
	 *
	 * @param content
	 *            The content of the Request in String format.
	 */
	public function setContent($content) {
		$this->requestContent = $content;
	}
}
?>
