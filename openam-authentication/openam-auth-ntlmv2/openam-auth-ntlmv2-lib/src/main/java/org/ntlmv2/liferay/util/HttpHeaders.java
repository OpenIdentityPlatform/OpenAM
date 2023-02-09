/**
 * Copyright (c) 2000-2012 Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package org.ntlmv2.liferay.util;

/**
 * @author Brian Wing Shun Chan
 */
public interface HttpHeaders {

	// Names

	public static final String ACCEPT = "ACCEPT";

	public static final String ACCEPT_ENCODING = "Accept-Encoding";

	public static final String ACCEPT_RANGES = "Accept-Ranges";

	public static final String AUTHORIZATION = "Authorization";

	public static final String CACHE_CONTROL = "Cache-Control";

	public static final String COOKIE = "Cookie";

	public static final String CONNECTION = "Connection";

	public static final String CONTENT_DISPOSITION = "Content-Disposition";

	public static final String CONTENT_ENCODING = "Content-Encoding";

	public static final String CONTENT_ID = "Content-ID";

	public static final String CONTENT_LENGTH = "Content-Length";

	public static final String CONTENT_RANGE = "Content-Range";

	public static final String CONTENT_TYPE = "Content-Type";

	public static final String EXPIRES = "Expires";

	public static final String ETAG = "ETag";

	public static final String IF_MODIFIED_SINCE = "If-Modified-Since";

	public static final String IF_NONE_MATCH = "If-None-Match";

	public static final String KEEP_ALIVE = "Keep-Alive";

	public static final String LAST_MODIFIED = "Last-Modified";

	public static final String LIFERAY_EMAIL_ADDRESS = "LIFERAY_EMAIL_ADDRESS";

	public static final String LIFERAY_SCREEN_NAME = "LIFERAY_SCREEN_NAME";

	public static final String LIFERAY_USER_ID = "LIFERAY_USER_ID";

	public static final String LOCATION = "Location";

	public static final String PRAGMA = "Pragma";

	public static final String RANGE = "Range";

	public static final String USER_AGENT = "User-Agent";

	public static final String WWW_AUTHENTICATE = "WWW-Authenticate";

	// Values

	public static final String ACCEPT_RANGES_BYTES_VALUE = "bytes";

	public static final String CONNECTION_CLOSE_VALUE = "close";

	public static final String CACHE_CONTROL_DEFAULT_VALUE =
		"max-age=315360000, public";

	public static final String CACHE_CONTROL_NO_CACHE_VALUE =
		"private, no-cache, no-store, must-revalidate";

	public static final String CACHE_CONTROL_PUBLIC_VALUE = "public";

	/**
	 * @deprecated Use <code>CONNECTION_CLOSE_VALUE</code>.
	 */
	public static final String CLOSE = CONNECTION_CLOSE_VALUE;

	public static final String EXPIRES_DEFAULT_VALUE = "315360000";

	public static final String PRAGMA_NO_CACHE_VALUE = "no-cache";

	public static final String PRAGMA_PUBLIC_VALUE = "public";

	/**
	 * @deprecated Use <code>CACHE_CONTROL_PUBLIC_VALUE</code>.
	 */
	public static final String PUBLIC = CACHE_CONTROL_PUBLIC_VALUE;

}