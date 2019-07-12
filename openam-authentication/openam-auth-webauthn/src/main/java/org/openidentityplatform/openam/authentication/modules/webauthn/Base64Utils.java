/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions Copyrighted [year] [name of copyright owner]".
 *
 * Copyright 2019 3A-Systems LLC. All rights reserved.
 */

package org.openidentityplatform.openam.authentication.modules.webauthn;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * 
 * This class provides basic utils to work with Base64 strings
 *
 */
public class Base64Utils {
	
	private static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;
	

	public static byte[] decodeUrlSafe(byte[] src) {
		if (src.length == 0) {
			return src;
		}
		return Base64.getUrlDecoder().decode(src);
	}
	
	public static byte[] decodeFromUrlSafeString(String src) {
		return decodeUrlSafe(src.getBytes(DEFAULT_CHARSET));
	}
	
	public static String encodeToUrlSafeString(byte[] bytes) {
		return Base64.getUrlEncoder().encodeToString(bytes);
	}
}
