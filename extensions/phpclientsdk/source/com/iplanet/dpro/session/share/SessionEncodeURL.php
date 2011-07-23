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
 * $Id: SessionEncodeURL.php,v 1.1 2007/03/09 21:13:09 chicchiricco Exp $
 *
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 */


/**
 * <code>SessionEncodeURL</code> class encodes the </code>URL</code>
 * with the cookie value as a query string
 * or extra path info based on the encoding scheme.
 * <p>
 * The cookie Value is written in the URL based on the encoding scheme
 * specified. The Cookie Value could be written as path info separated
 * by either a "/" OR  ";" or as a query string.
 *
 * <p>
 * If the encoding scheme is SLASH then the  cookie value would be
 * written in the URL as extra path info in the following format:
 * <code>protocol://server:port/servletpath/&lt;cookieName>=&lt;cookieValue>?
 * queryString</code>
 * <p>
 * Note that this format works only if the path is a servlet, if a
 * a JSP file is specified then web containers return with
 * "File Not found" error. To rewrite links which are JSP files with
 * cookie value use the <code>SEMICOLON</code> or <code>QUERY</code> encoding
 * scheme.
 *
 * <p>
 * If the encoding scheme is SEMICOLON then the cookie value would be
 * written in the URL as extra path info in the following format:
 * <code>protocol://server:port/path;&lt;cookieName=cookieValue>
 * ?queryString</code>
 * Note that this is not supported in the servlet specification and
 * some web containers do not support this.
 *
 * <p>
 * If the encoding scheme is QUERY then the cookie value would be
 * written in the URL in the following format:
 * <pre>
 * protocol://server:port/path?&lt;cookieName>=&lt;cookieValue>
 * protocol://server:port/path?queryString&&lt;cookieName>=&lt;cookieValue>
 * </pre>
 * <p>
 * This is the default and Access Manager always encodes in this format
 * unless otherwise specified. If the URL passed in has query parameter then
 * entity escaping of ampersand will be done before appending the cookie
 * if the escape is true.Only the ampersand before appending cookie parameter
 * will be entity escaped.
 */
class SessionEncodeURL {

	const SESS_DELIMITER = ";";
	const SLASH_SESS_DELIMITER = "/";
	const QUERY = "?";

	// TODO: read from properties!
	static $cookieEncoding = true;

	/* retrieves the session ID from the request URI */
	private function getSidFromURI($url, $cookieName) {
		$sid = "";
		if ($url != null) {
			//			if (debug . messageEnabled()) {
			//				debug . message("getSidFromURI: url=" + url);
			//			}
			if ($url != null && length($url) > 0) {
				$start = strpos($url, $cookieName);
				if ($start) {
					$start = $start +length($cookieName) + 1;

					$end = strpos($url, SessionEncodeURL :: QUERY, $start);
					if ($end)
						$sid = substr($url, $start, $end -1);
					else
						$sid = substr($url, $start);
				}
			}
		}

		//		if (debug . messageEnabled()) {
		//			debug . message("getSidFromURL: sid =" + sid);
		//		}
		return $sid;
	}

	/* extracts the sessionId from the request Query */
	private function getSidFromQuery(array $request, $cookieName) {
		$sid = "";
		if ($request != null)
			$sid = $request[$cookieName];

		//		if (debug . messageEnabled()) {
		//			debug . message("getSidFromQuery: request =" + request);
		//			debug . message("getSidFromQuery: sid =" + sid);
		//		}
		return $sid;
	}

	/**
	* Checks whether the encoded URL has session id or not. And then extracts
	* the Session Id from it.
	*
	* @param request HTTP Servlet Request.
	* @param cookieName Cookie name.
	* @return the extracted Session ID
	*/
	public static function getSidFromURL(array $request, $cookieName) {
		$sidString = "";
		if ($request != null) {
			$url = $_SERVER['REQUEST_URI'];
			if ($url != null) {
				if (!strstr($url, SessionEncodeURL :: SLASH_SESS_DELIMITER . $cookieName) || !strstr($url, SessionEncodeURL :: SESS_DELIMITER . $cookieName))
					$sidString = getSidFromURI($url, $cookieName);
				else
					$sidString = getSidFromQuery($request, $cookieName);
			}
		}

		//        if (debug.messageEnabled()) {
		//            debug.message("before decoding getSidFromURL:sidString="
		//                    + sidString);
		//        }
		if (SessionEncodeURL :: $cookieEncoding && $sidString != null)
			$sidString = urldecode($sidString);

		//        if (debug.messageEnabled()) {
		//            debug.message("after decoding: getSidFromURL:sidString="
		//                    + sidString);
		//        }

		return $sidString;
	}
}
?>
