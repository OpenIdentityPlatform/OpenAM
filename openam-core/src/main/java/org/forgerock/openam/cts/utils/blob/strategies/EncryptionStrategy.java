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
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2013-2015 ForgeRock AS.
 * Portions copyright 2025 3A Systems LLC.
 */
package org.forgerock.openam.cts.utils.blob.strategies;

import com.google.inject.name.Named;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.openam.cts.api.CoreTokenConstants;
import org.forgerock.openam.cts.utils.blob.BlobStrategy;
import org.forgerock.openam.cts.utils.blob.TokenStrategyFailedException;
import org.forgerock.openam.cts.utils.blob.strategies.encryption.DecryptAction;
import org.forgerock.openam.cts.utils.blob.strategies.encryption.EncryptAction;
import org.forgerock.util.Reject;

import javax.annotation.concurrent.ThreadSafe;
import jakarta.inject.Inject;
import java.security.AccessController;
import java.security.PrivilegedActionException;

/**
 * Responsible for managing encryption of the Token based on the current Core Token Service
 * configuration.
 */
@ThreadSafe
public class EncryptionStrategy implements BlobStrategy {
    private final Debug debug;

    @Inject
    public EncryptionStrategy(@Named(CoreTokenConstants.CTS_DEBUG) Debug debug) {
        this.debug = debug;
    }

    /**
     * Encrypt the contents of the Tokens binary data blob.
     *
     * @see org.forgerock.openam.cts.api.tokens.Token#getBlob()
     *
     * @param blob Non null token to encrypt.
     *
     * @return Non null copy of the Token which has been encrypted.
     */
    public byte[] perform(byte[] blob) throws TokenStrategyFailedException {
        Reject.ifTrue(blob == null);
        try {
            byte[] encryptedBlob = AccessController.doPrivileged(new EncryptAction(blob));

            if (debug.messageEnabled()) {
                debug.message(CoreTokenConstants.DEBUG_HEADER + "Encrypted Token");
            }

            return encryptedBlob;
        } catch (PrivilegedActionException e) {
            throw new TokenStrategyFailedException("Failed to encrypt JSON Blob", e);
        }
    }

    /**
     * Decrypt the contents of the Tokens binary data blob.
     *
     * @see org.forgerock.openam.cts.api.tokens.Token#getBlob()
     *
     * @param blob Non null Token to decrypt.
     *
     * @return Non null copy of the Token which has been decrypted.
     */
    public byte[] reverse(byte[] blob) throws TokenStrategyFailedException {
        Reject.ifTrue(blob == null);
        try {
            byte[] decryptedBlob = AccessController.doPrivileged(new DecryptAction(blob));

            if (debug.messageEnabled()) {
                debug.message(CoreTokenConstants.DEBUG_HEADER + "Decrypted Token");
            }

            return decryptedBlob;
        } catch (PrivilegedActionException e) {
            throw new TokenStrategyFailedException("Failed to decrypt JSON Blob", e);
        }
    }
}
