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
 * $Id: PLLClient.php,v 1.1 2007/03/09 21:13:11 chicchiricco Exp $
 *
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 */


/**
 * The <code>PLLClient</code> class is used to send RequestSet XML documents
 * to the URL specified in the send() method. The high level services and
 * application can use Naming Service to find the service specific URL. This
 * class provides static methods to register notification handlers.
 *
 * @see com.iplanet.services.comm.share.RequestSet
 * @see com.iplanet.services.comm.share.Response
 * @see com.iplanet.services.comm.client.SendRequestException
 * @see com.iplanet.services.comm.client.NotificationHandler
 */
class PLLClient {

	/** notification handlers */
	private static $notificationHandlers = array ();

	/**
	 * Translates the Java object to an XML RequestSet document and sends the
	 * corresponding XML document to the specified URL.
	 *
	 * @param url
	 *            The destination URL for the RequestSet XML document.
	 * @param set
	 *            The RequestSet Java object to be translated to an XML
	 *            RequestSet document.
	 * @exception SendRequestException
	 *                if there is an error in sending the XML document.
	 */
	public static function sendURLRequestSet($url, RequestSet $set) {
		return PLLClient :: send($url, null, $set, null);
	}

	/**
	 * Translates the Java object to an XML RequestSet document and sends the
	 * corresponding XML document to the specified URL.
	 *
	 * @param url
	 *            The destination URL for the RequestSet XML document.
	 * @param cookies
	 *            The value for Http Request Header 'Cookie'
	 * @param set
	 *            The RequestSet Java object to be translated to an XML
	 *            RequestSet document.
	 * @exception SendRequestException
	 *                if there is an error in sending the XML document.
	 */
	public static function sendUrlCookiesRequestSet($url, $cookies, RequestSet $set) {
		return PLLClient :: send($url, $cookies, $set, null);
	}

	/**
	 * Translates the Java object to an XML RequestSet document and sends the
	 * corresponding XML document to the specified URL.
	 *
	 * @param url
	 *            The destination URL for the RequestSet XML document.
	 * @param set
	 *            The RequestSet Java object to be translated to an XML
	 *            RequestSet document.
	 * @param cookieTable
	 *            The HashMap that constains cookies to be replayed and stores
	 *            cookies retrieved from the response.
	 * @exception SendRequestException
	 *                if there is an error in sending the XML document.
	 */
	public static function sendUrlRequestSetCookieTable($url, RequestSet $set, $cookieTable) {
		return PLLClient :: send($url, null, $set, $cookieTable);
	}

	// The private method that implements the above interfaces.
	// HashMap cookieTable passes in the cookies that will be replayed. It also
	// is the place holder to retrieve additional cookies if any from the
	// URL connection response.
	private static function send($url, $cookies, RequestSet $set, $cookieTable) {
		$bits = parse_url($url);
		$host = $bits['host'];
		$port = isset ($bits['port']) ? $bits['port'] : 80;
		$path = isset ($bits['path']) ? $bits['path'] : '/';

		$conn = new HttpClient($host, $port);
		$conn->setCookies($cookieTable);
		$conn->setContentType("text/xml;charset=UTF-8");

		// Output ...
		$xml = $set->toXMLString();
		if (!$conn->post($path, $xml))
			throw new Exception("PLLClient send exception");

		// Input ...
		$in_string = $conn->getContent();
		$cookieTable = $conn->getCookies();

		$resset = ResponseSet :: parseXML($in_string);
		return $resset->getResponses();
	}

	/**
	 * Adds a notification handler for a service. This handler is used by the
	 * Platform Low Level servlet to pass notifications for processing.
	 *
	 * @param service
	 *            The name of the service such as session, profile
	 * @param handler
	 *            The handler for notification processing
	 */
	public static function addNotificationHandler($service, NotificationHandler $handler) {
		if (array_key_exists(PLLClient :: $notificationHandlers, $service))
			throw new Exception("Already registered: " . $service);

		PLLClient :: $notificationHandlers[$service] = $handler;
	}

	/**
	 * Removes a notification handler of a service.
	 *
	 * @param service
	 *            The name of the service whose handler needs to be removed
	 */
	public static function removeNotificationHandler($service) {
		PLLClient :: $notificationHandlers[$service] = null;
	}

	/**
	 * Gets a notification handler of a service.
	 *
	 * @param service
	 *            The name of the service whose handler needs to be returned
	 */
	public static function getNotificationHandler($service) {
		return PLLClient :: $notificationHandlers[$service];
	}
}
?>
