package org.openidentityplatform.openam.authentication.modules.webauthn;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

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
