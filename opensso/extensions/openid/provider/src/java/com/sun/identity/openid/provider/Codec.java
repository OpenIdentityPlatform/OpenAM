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
 * $Id: Codec.java,v 1.1 2009/04/24 21:01:58 rparekh Exp $
 *
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 * Portions Copyrighted 2007 Paul C. Bryan
 */

package com.sun.identity.openid.provider;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64;

/**
 * Provides encoding and decoding methods for Java types, conformant with the
 * OpenID specification.
 * 
 * @author pbryan
 */
public class Codec {
	/** Date format used in OpenID specification. */
	private static final DateFormat DATE = new SimpleDateFormat(
			"yyyy-MM-dd'T'HH:mm:ss'Z'");

	/** Pattern to further limit types of URLs handled. */
	private static final Pattern WEB = Pattern.compile("https?://.+");

	/**
	 * TODO: Description.
	 * 
	 * @param value
	 *            TODO.
	 * @return TODO.
	 * @throws DecodeException
	 *             TODO.
	 */
	public static BigInteger decodeBigInteger(String value)
			throws DecodeException {
		byte[] bytes = decodeBytes(value);

		if (bytes == null) {
			return null;
		}

		try {
			return new BigInteger(bytes);
		}

		catch (NumberFormatException nfe) {
			throw new DecodeException("big integer could not be decoded");
		}
	}

	/**
	 * TODO: Description.
	 * 
	 * @param value
	 *            TODO.
	 * @return TODO.
	 * @throws DecodeException
	 *             TODO.
	 */
	public static Boolean decodeBoolean(String value) throws DecodeException {
		if (value == null) {
			return null;
		}

		if (value.equals("true")) {
			return Boolean.TRUE;
		}

		if (value.equals("false")) {
			return Boolean.FALSE;
		}

		throw new DecodeException("boolean could not be decoded");
	}

	/**
	 * TODO: Description.
	 * 
	 * @param value
	 *            TODO.
	 * @return TODO.
	 * @throws DecodeException
	 *             TODO.
	 */
	public static byte[] decodeBytes(String value) throws DecodeException {
		if (value == null) {
			return null;
		}

		try {
			return Base64.decodeBase64(value.getBytes("UTF-8"));
		}

		// a java virtual machine without UTF-8 encoding shouldn't occur
		catch (UnsupportedEncodingException uee) {
			throw new IllegalStateException(uee);
		}

		// handle failure to decode UTF-8 input (e.g., malformed encoding)
		catch (Exception e) {
			throw new DecodeException("bytes could not be decoded");
		}
	}

	/**
	 * Decodes a date from a passed string.
	 * 
	 * @param value
	 *            string value to decode into date.
	 * @return date value parsed from string, or null if no date could be
	 *         parsed.
	 * @throws DecodeException
	 *             TODO.
	 */
	public static Date decodeDate(String value) throws DecodeException {
		if (value == null) {
			return null;
		}

		try {
			return DATE.parse(value);
		}

		catch (ParseException pe) {
			throw new DecodeException("date could not be decoded");
		}
	}

	/**
	 * TODO: Description.
	 * 
	 * @param value
	 *            TODO.
	 * @return TODO.
	 */
	public static List<String> decodeList(String value) {
		if (value == null) {
			return null;
		}

		ArrayList<String> list = new ArrayList<String>();

		for (String item : value.split(",")) {
			item = item.trim();

			if (item.length() > 0) {
				list.add(item);
			}
		}

		return list;
	}

	/**
	 * TODO: Description.
	 * 
	 * @param value
	 *            TODO.
	 * @return TODO.
	 * @throws DecodeException
	 *             TODO.
	 */
	public static Long decodeLong(String value) throws DecodeException {
		if (value == null) {
			return null;
		}

		try {
			return new Long(value);
		}

		catch (NumberFormatException nfe) {
			throw new DecodeException("long integer could not be decoded");
		}
	}

	/**
	 * TODO: Description.
	 * 
	 * @param value
	 *            TODO.
	 * @return TODO.
	 * @throws DecodeException
	 *             TODO.
	 */
	public static SecretKey decodeSecretKey(String value, String algorithm)
			throws DecodeException {
		byte[] bytes = decodeBytes(value);

		if (bytes == null) {
			return null;
		}

		return new SecretKeySpec(bytes, algorithm);
	}

	/**
	 * TODO: Description.
	 * 
	 * @param value
	 *            TODO.
	 * @return TODO.
	 * @throws DecodeException
	 *             TODO.
	 */
	public static URL decodeURL(String value) throws DecodeException {
		if (value == null) {
			return null;
		}

		if (!WEB.matcher(value).matches()) {
			throw new DecodeException("URL could not be decoded");
		}

		try {
			return new URL(value);
		}

		catch (MalformedURLException mue) {
			throw new DecodeException(mue.getMessage());
		}
	}

	/**
	 * TODO: Description.
	 * 
	 * @param value
	 *            TODO.
	 * @return TODO.
	 */
	public static String encodeBigInteger(BigInteger value) {
		if (value == null) {
			return null;
		}

		return encodeBytes(value.toByteArray());
	}

	/**
	 * TODO: Description.
	 * 
	 * @param value
	 *            TODO.
	 * @return TODO.
	 */
	public static String encodeBoolean(Boolean value) {
		if (value == null) {
			return null;
		}

		return value.toString();
	}

	/**
	 * TODO: Description.
	 * 
	 * @param value
	 *            TODO.
	 * @return TODO.
	 */
	public static String encodeBytes(byte[] value) {
		if (value == null) {
			return null;
		}

		return new String(Base64.encodeBase64(value));
	}

	/**
	 * TODO: Description.
	 * 
	 * @param value
	 *            TODO.
	 * @return TODO.
	 */
	public static String encodeDate(Date value) {
		if (value == null) {
			return null;
		}

		return DATE.format(value);
	}

	/**
	 * TODO: Description.
	 * 
	 * @param value
	 *            TODO.
	 * @return TODO.
	 */
	public static String encodeList(List<String> value) {
		if (value == null) {
			return null;
		}

		String delim = "";

		StringBuffer buf = new StringBuffer();

		for (String item : value) {
			if (item == null) {
				continue;
			}

			item = item.trim();

			if (item.length() == 0) {
				continue;
			}

			buf.append(delim).append(item);
			delim = ",";
		}

		return buf.toString();
	}

	/**
	 * TODO: Description.
	 * 
	 * @param value
	 *            TODO.
	 * @return TODO.
	 */
	public static String encodeLong(Long value) {
		if (value == null) {
			return null;
		}

		return value.toString();
	}

	/**
	 * TODO: Description.
	 * 
	 * @param value
	 *            TODO.
	 * @return TODO.
	 */
	public static String encodeSecretKey(SecretKey value) {
		if (value == null) {
			return null;
		}

		return encodeBytes(value.getEncoded());
	}

	/**
	 * TODO: Description.
	 * 
	 * @param value
	 *            TODO.
	 * @return TODO.
	 */
	public static String encodeURL(URL value) {
		if (value == null) {
			return null;
		}

		return value.toString();
	}
}
