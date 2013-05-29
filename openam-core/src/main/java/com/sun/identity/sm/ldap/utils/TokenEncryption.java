/**
 * Copyright 2013 ForgeRock, Inc.
 *
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
 * information: "Portions copyright [year] [name of copyright owner]".
 */
package com.sun.identity.sm.ldap.utils;

import com.iplanet.dpro.session.service.SessionService;
import com.iplanet.services.util.Crypt;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.ldap.api.CoreTokenConstants;
import com.sun.identity.sm.ldap.api.tokens.Token;

import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;

/**
 * Responsible for managing encryption of the Token based on the current Core Token Service
 * configuration.
 *
 * @author robert.wapshott@forgerock.com
 */
public class TokenEncryption {

    private Debug debug;

    public TokenEncryption() {
        this(SessionService.sessionDebug);
    }

    public TokenEncryption(Debug debug) {
        this.debug = debug;
    }

    /**
     * Encrypt the contents of the Tokens binary data blob.
     *
     * @see com.sun.identity.sm.ldap.api.tokens.Token#getBlob()
     *
     * @param token Non null token to encrypt.
     *
     * @return Non null copy of the Token which has been encrypted.
     */
    public Token encrypt(Token token) {
        Token clone = new Token(token);
        try {
            byte[] blob = token.getBlob();
            byte[] encyptedBlob = AccessController.doPrivileged(new EncryptAction(blob));
            clone.setBlob(encyptedBlob);

            if (debug.messageEnabled()) {
                debug.message(CoreTokenConstants.DEBUG_HEADER + "Encrypted Token");
            }

        } catch (PrivilegedActionException e) {
            throw new IllegalStateException(
                    "\n" + CoreTokenConstants.DEBUG_HEADER + "Failed to encrypt the JSON blob",
                    e);
        }
        return clone;
    }

    /**
     * Decrypt the contents of the Tokens binary data blob.
     *
     * @see com.sun.identity.sm.ldap.api.tokens.Token#getBlob()
     *
     * @param token Non null Token to decrypt.
     *
     * @return Non null copy of the Token which has been decrypted.
     */
    public Token decrypt(Token token) {

        Token clone = new Token(token);
        try {
            byte[] blob = token.getBlob();
            byte[] decryptedBlob = AccessController.doPrivileged(new DecryptAction(blob));
            clone.setBlob(decryptedBlob);

            if (debug.messageEnabled()) {
                debug.message(CoreTokenConstants.DEBUG_HEADER + "Decrypted Token");
            }

        } catch (PrivilegedActionException e) {
            throw new IllegalStateException(
                    "\n" + CoreTokenConstants.DEBUG_HEADER + "Failed to decrypt the JSON blob",
                    e);
        }

        return clone;
    }

    /**
     * Internal encryption action is responsible for coordinating the encryption functions.
     */
    private static class EncryptAction implements PrivilegedExceptionAction<byte[]> {
        private final byte[] blob;

        public EncryptAction(byte[] blob) {
            this.blob = blob;
        }

        public byte[] run() throws Exception {
            return Crypt.getEncryptor().encrypt(blob);
        }
    }

    /**
     * Internal encryption action is responsible for coordinating the decryption functions.
     */
    private static class DecryptAction implements PrivilegedExceptionAction<byte[]> {
        private byte[] blob;

        public DecryptAction(byte[] blob) {
            this.blob = blob;
        }

        public byte[] run() throws Exception {
            return Crypt.getEncryptor().decrypt(blob);
        }
    }
}
