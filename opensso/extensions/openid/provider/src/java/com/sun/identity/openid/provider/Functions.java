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
 * $Id: Functions.java,v 1.1 2009/04/24 21:01:58 rparekh Exp $
 *
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 * Portions Copyrighted 2007 Paul C. Bryan
 */

package com.sun.identity.openid.provider;

import java.util.Map;

/**
 * TODO: Description.
 * 
 * @author pbryan
 */
public class Functions {
	/**
	 * TODO: Description.
	 * 
	 * @param a
	 *            TODO.
	 * @param b
	 *            TODO.
	 * @return TODO.
	 */
	public static String concat(String a, String b) {
		return a + b;
	}

	/**
	 * TODO: Description.
	 * 
	 * @param msg
	 *            TODO.
	 */
	public static void diag(String msg) {
		System.err.println(msg);
	}

	/**
	 * TODO: Description.
	 * 
	 * @param xss
	 *            the text that should be protected from cross-site scripting.
	 * @return TODO.
	 */
	public static String escape(String xss) {
		if (xss == null) {
			return null;
		}

		int length = xss.length();

		// provide a moderate amount of extra space for expansion
		StringBuffer buf = new StringBuffer(length + 60);

		for (int n = 0; n < length; n++) {
			char c = xss.charAt(n);

			switch (c) {
			case '&':
				buf.append("&amp;");
				break;
			case '<':
				buf.append("&lt;");
				break;
			case '>':
				buf.append("&gt;");
				break;
			case '\'':
				buf.append("&apos;");
				break;
			case '"':
				buf.append("&quot;");
				break;
			default:
				buf.append(c);
			}
		}

		return buf.toString();
	}

	/**
	 * TODO: Description.
	 * 
	 * @param url
	 *            TODO.
	 * @return TODO.
	 */
	public static String externalLink(String url) {
		// provide a moderate amount of extra space for expansion
		StringBuffer buf = new StringBuffer(url.length() + 40);

		buf.append("<a href=\"");
		buf.append(escape(url));
		buf.append("\"");

		String target = Config.getString(Config.EXTERNAL_TARGET);
		if (target != null && target.length() != 0) {
			buf.append(" target=\"" + target + "\"");
		}

		buf.append(">");
		buf.append(escape(url));
		buf.append("</a>");

		return buf.toString();
	}

	/**
	 * TODO: Description.
	 * 
	 * @param map
	 *            TODO.
	 * @return TODO.
	 */
	public static int size(Map map) {
		return (map == null ? 0 : map.size());
	}

	/**
	 * Splits a string around matches of the given regular expression.
	 * 
	 * @param string
	 *            the string to be split.
	 * @param regex
	 *            the delimiting regular expression.
	 * @return an array of strings computed by splitting the string.
	 */
	public static String[] split(String string, String regex) {
		return string.split(regex);
	}

	/**
	 * TODO: Description
	 * 
	 * @param text
	 *            TODO.
	 * @return TODO.
	 */
	public static String strong(String text) {
		StringBuffer buf = new StringBuffer(text.length() + 20);
		return buf.append("<strong>").append(text).append("</strong>")
				.toString();
	}
}
