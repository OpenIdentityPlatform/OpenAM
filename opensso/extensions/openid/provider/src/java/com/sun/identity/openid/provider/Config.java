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
 * $Id: Config.java,v 1.2 2009/09/07 15:03:48 hubertlvg Exp $
 *
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 * Portions Copyrighted 2007 Paul C. Bryan and Robert Nguyen
 */

package com.sun.identity.openid.provider;

import java.io.InputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;
import javax.crypto.SecretKey;

/**
 * TODO: Description.
 * 
 * @author pbryan, Robert Nguyen
 */
public class Config {
	/** TODO: Description. */
	public static final String PREFIX = "openid.provider.";

	/** TODO: Description. */
	public static final String ENCRYPTION_KEY = PREFIX + "encryption_key";

	/** TODO: Description. */
	public static final String EXTERNAL_TARGET = PREFIX + "external_target";

	/** TODO: Description. */
	public static final String IDENTITY_PATTERN = PREFIX + "identity_pattern";

	/** TODO: Description. */
	public static final String LOGIN_URL = PREFIX + "login_url";
	/** TODO: Description. */
	public static final String LOCAL_AUTH_URL = PREFIX + "local-auth-url";

	/** TODO: Description. */
	public static final String AM_PROFILE_ATTRIBUTES = PREFIX
			+ "am-profile-attributes";
	/** TODO: Description. */
	public static final String PRINCIPAL_PATTERN = PREFIX + "principal_pattern";

	/** TODO: Description. */
	public static final String TYPES = PREFIX + "attribute_types_map";

	/** TODO: Description. */
	public static final String SERVICE_URL = PREFIX + "service_url";
	
	
	/** TODO: Description. */
	public static final String SETUP_URL = PREFIX + "setup_url";

	/** TODO: Description. */
	public static final String AM_SEARCH_ATTRIBUTE = PREFIX
			+ "am-search-attribute";

	/** TODO: Description. */
	public static final String SIMPLE_REGISTRATION = PREFIX
			+ "simple_registration";

	/** TODO: Description. */
	public static final String ATTRIBUTE_EXCHANGE = PREFIX
			+ "attribute_exchange";

	/** TODO: Description. */
	public static final String STRICT_PROTOCOL = PREFIX + "strict_protocol";

	/** Cached compiled regular expression patterns. */
	private static final HashMap<String, Pattern> patterns = new HashMap<String, Pattern>();

	/** Cached secret keys. */
	private static final HashMap<String, SecretKey> secretKeys = new HashMap<String, SecretKey>();

	/** TODO: Description. */
	private static Properties properties = null;

	/* attribute persistence flag */
	public static String PERSIST = PREFIX + "persistence.enabled";
	
	/* attribute persistence class */
	public static String PERSISTENCE_IMPL = PREFIX + "persistence.class.name";

    /* Enforce RP/realm validation flag  */
    public static final String ENFORCERPID = PREFIX + "enforcerpid";

	/**
	 * TODO: Description.
	 */
	private static synchronized void loadProperties() {
		if (properties != null) {
			return;
		}

		InputStream in = Config.class.getClassLoader().getResourceAsStream(
				"Provider.properties");

		if (in == null) {
			throw new RuntimeException(
					"configuration properties file not found");
		}

		properties = new Properties();

		try {
			properties.load(in);
		}

		catch (IOException ioe) {
			throw new RuntimeException(ioe);
		}
	}

	/**
	 * TODO: Description.
	 * 
	 * @param key
	 *            TODO.
	 * @return TODO.
	 */
	public static Boolean getBoolean(String key) {
		String value = getString(key);

		if (value == null) {
			return null;
		}

		return Boolean.valueOf(value);
	}

	/**
	 * TODO: Description.
	 * 
	 * @param key
	 *            TODO.
	 * @return TODO.
	 */
	public static Pattern getPattern(String key) {
		Pattern pattern = patterns.get(key);

		if (pattern != null) {
			return pattern;
		}

		String value = getString(key);

		if (value != null) {
			pattern = Pattern.compile(value);
			patterns.put(key, pattern);
		}

		return pattern;
	}

	/**
	 * TODO: Description.
	 * 
	 * @param key
	 *            TODO.
	 * @param algorithm
	 *            TODO.
	 * @return TODO.
	 */
	public static SecretKey getSecretKey(String key, String algorithm) {
		String lookup = key + ':' + algorithm;

		SecretKey secret = secretKeys.get(lookup);

		if (secret != null) {
			return secret;
		}

		String value = getString(key);

		if (value == null || value.length() == 0) {
			return null;
		}

		try {
			secret = Codec.decodeSecretKey(value, algorithm);
		}

		catch (DecodeException de) {
			throw new IllegalStateException("invalid secret key: " + key);
		}

		// store key for future lookups
		secretKeys.put(lookup, secret);

		return secret;
	}

	/**
	 * TODO: Description.
	 * 
	 * @param key
	 *            TODO.
	 * @return TODO.
	 */
	public static String getString(String key) {
		if (properties == null) {
			loadProperties();
		}

		// no need to cache strings; just as efficient fetching from properties
		return properties.getProperty(key);
	}

	/*
	 * Assign types to attributes
	 */
	public static Map<String, String> getTypes() {
		Map<String, String> types = new HashMap<String, String>();
		String[] attrTypes = getString(TYPES).split(",");
		for (int i = 0; i < attrTypes.length; i++) {
			int index = attrTypes[i].indexOf("|");
			types.put(attrTypes[i].substring(0, index), attrTypes[i]
					.substring(index + 1));
		}
		return types;
	}
}
