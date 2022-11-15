/* 
    Licensed to the Apache Software Foundation (ASF) under one
    or more contributor license agreements.  See the NOTICE file
    distributed with this work for additional information
    regarding copyright ownership.  The ASF licenses this file
    to you under the Apache License, Version 2.0 (the
    "License"); you may not use this file except in compliance
    with the License.  You may obtain a copy of the License at
       http://www.apache.org/licenses/LICENSE-2.0
    Unless required by applicable law or agreed to in writing,
    software distributed under the License is distributed on an
    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
    KIND, either express or implied.  See the License for the
    specific language governing permissions and limitations
    under the License.  
 */

package org.openidentityplatform.openam.cassandra;


import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Random;

import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Hashes and verifies salted SHA-1 passwords, which are compliant with RFC
 * 2307.
 */
public final class SSHA256 {
	private static Logger logger = LoggerFactory.getLogger(SSHA256.class);
	
	private static final String SSHA = "{SSHA256}";

	private static final Random RANDOM = new SecureRandom();

	private static final int DEFAULT_SALT_SIZE = 8;

	/**
	 * Private constructor to prevent direct instantiation.
	 */
	private SSHA256() {
	}


	/**
	 * Creates an RFC 2307-compliant salted, hashed password with the SHA1
	 * MessageDigest algorithm. After the password is digested, the first 32
	 * bytes of the digest will be the actual password hash; the remaining bytes
	 * will be a randomly generated salt of length {@link #DEFAULT_SALT_SIZE},
	 * for example: <blockquote>
	 * <code>{SSHA256}3cGWem65NCEkF5Ew5AEk45ak8LHUWAwPVXAyyw==</code></blockquote>
	 * In layman's terms, the formula is
	 * <code>digest( secret + salt ) + salt</code>. The resulting digest is
	 * Base64-encoded.
	 * Note that successive invocations of this method with the same password
	 * will result in different hashes! (This, of course, is exactly the point.)
	 * 
	 * @param password
	 *            the password to be digested
	 * @return the Base64-encoded password hash, prepended by
	 *         <code>{SSHA256}</code>.
	 * @throws NoSuchAlgorithmException
	 *             If your JVM is completely b0rked and does not have SHA.
	 */
	public static String getSaltedPassword(byte[] password)
			throws NoSuchAlgorithmException {
		byte[] salt = new byte[DEFAULT_SALT_SIZE];
		RANDOM.nextBytes(salt);
		return getSaltedPassword(password, salt);
	}

	/**
	 * <p>
	 * Helper method that creates an RFC 2307-compliant salted, hashed password
	 * with the SHA1 MessageDigest algorithm. After the password is digested,
	 * the first 32 bytes of the digest will be the actual password hash; the
	 * remaining bytes will be the salt. Thus, supplying a password
	 * <code>testing123</code> and a random salt <code>foo</code> produces the
	 * hash:
	 * </p>
	 * <blockquote><code>{SSHA256}yfT8SRT/WoOuNuA6KbJeF10OznZmb28=</code>
	 * </blockquote>
	 * <p>
	 * In layman's terms, the formula is
	 * <code>digest( secret + salt ) + salt</code>. The resulting digest is
	 * Base64-encoded.
	 * </p>
	 * 
	 * @param password
	 *            the password to be digested
	 * @param salt
	 *            the random salt
	 * @return the Base64-encoded password hash, prepended by
	 *         <code>{SSHA256}</code>.
	 * @throws NoSuchAlgorithmException
	 *             If your JVM is totally b0rked and does not have SHA1.
	 */
	protected static String getSaltedPassword(byte[] password, byte[] salt)
			throws NoSuchAlgorithmException {
		MessageDigest digest = MessageDigest.getInstance("SHA-256");
		digest.update(password);
		byte[] hash = digest.digest(salt);

		// Create an array with the hash plus the salt
		byte[] all = new byte[hash.length + salt.length];
		for (int i = 0; i < hash.length; i++) {
			all[i] = hash[i];
		}
		for (int i = 0; i < salt.length; i++) {
			all[hash.length + i] = salt[i];
		}
		byte[] base64 = Base64.encodeBase64(all);

		String base64String = null;
		try {
			base64String = SSHA + new String(base64, "UTF8");
		} catch (UnsupportedEncodingException e) {
			logger.error("getSaltedPassword",e);
		}
		return base64String;
	}

	/**
	 * Compares a password to a given entry and returns true, if it matches.
	 * 
	 * @param password
	 *            The password in bytes.
	 * @param entry
	 *            The password entry, typically starting with {SSHA256}.
	 * @return True, if the password matches.
	 */
	public static boolean verifySaltedPassword(byte[] password, String entry) {
		try{
			// First, extract everything after {SSHA256} and decode from Base64
			if (!entry.startsWith(SSHA)) {
				throw new IllegalArgumentException(
						"Hash not prefixed by {SSHA256}; is it really a salted hash?");
			}
			byte[] challenge = Base64.decodeBase64(entry.substring(9).getBytes(
					"UTF-8"));
	
			// Extract the password hash and salt
			byte[] passwordHash = extractPasswordHash(challenge);
			byte[] salt = extractSalt(challenge);
	
			// Re-create the hash using the password and the extracted salt
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			digest.update(password);
			byte[] hash = digest.digest(salt);
	
			// See if our extracted hash matches what we just re-created
			return Arrays.equals(passwordHash, hash);
		}catch (Exception e) {
			logger.error("verifySaltedPassword",e);
			return false;
		}
	}

	/**
	 * Helper method that extracts the hashed password fragment from a supplied
	 * salted SHA digest by taking all of the characters before position 32.
	 * 
	 * @param digest
	 *            the salted digest, which is assumed to have been previously
	 *            decoded from Base64.
	 * @return the password hash
	 * @throws IllegalArgumentException
	 *             if the length of the supplied digest is less than or equal to
	 *             32 bytes
	 */
	protected static byte[] extractPasswordHash(byte[] digest)
			throws IllegalArgumentException {
		if (digest.length < 32) {
			throw new IllegalArgumentException(
					"Hash was less than 32 characters; could not extract password hash!");
		}

		// Extract the password hash
		byte[] hash = new byte[32];
		for (int i = 0; i < 32; i++) {
			hash[i] = digest[i];
		}

		return hash;
	}

	/**
	 * Helper method that extracts the salt from supplied salted digest by
	 * taking all of the characters at position 32 and higher.
	 * 
	 * @param digest
	 *            the salted digest, which is assumed to have been previously
	 *            decoded from Base64.
	 * @return the salt
	 * @throws IllegalArgumentException
	 *             if the length of the supplied digest is less than or equal to
	 *             32 bytes
	 */
	protected static byte[] extractSalt(byte[] digest)
			throws IllegalArgumentException {
		if (digest.length <= 32) {
			throw new IllegalArgumentException(
					"Hash was less than 41 characters; we found no salt!");
		}

		// Extract the salt
		byte[] salt = new byte[digest.length - 32];
		for (int i = 32; i < digest.length; i++) {
			salt[i - 32] = digest[i];
		}

		return salt;
	}
}