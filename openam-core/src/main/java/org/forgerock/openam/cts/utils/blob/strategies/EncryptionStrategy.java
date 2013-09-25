/**
 * Copyright 2013 ForgeRock, AS.
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
package org.forgerock.openam.cts.utils.blob.strategies;

import javax.inject.Inject;
import com.google.inject.name.Named;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.openam.cts.api.CoreTokenConstants;
import org.forgerock.openam.cts.api.tokens.Token;
import org.forgerock.openam.cts.utils.blob.BlobStrategy;
import org.forgerock.openam.cts.utils.blob.TokenStrategyFailedException;
import org.forgerock.openam.cts.utils.blob.strategies.encryption.DecryptAction;
import org.forgerock.openam.cts.utils.blob.strategies.encryption.EncryptAction;

import java.security.AccessController;
import java.security.PrivilegedActionException;

/**
 * Responsible for managing encryption of the Token based on the current Core Token Service
 * configuration.
 *
 * @author robert.wapshott@forgerock.com
 */
public class EncryptionStrategy implements BlobStrategy {
    private final Debug debug;
    private final EncryptAction encyptionAction;
    private final DecryptAction decyptAction;

    @Inject
    public EncryptionStrategy(EncryptAction encyptionAction, DecryptAction decyptAction,
                              @Named(CoreTokenConstants.CTS_DEBUG) Debug debug) {
        this.debug = debug;
        this.encyptionAction = encyptionAction;
        this.decyptAction = decyptAction;
    }

    /**
     * Encrypt the contents of the Tokens binary data blob.
     *
     * @see org.forgerock.openam.cts.api.tokens.Token#getBlob()
     *
     * @param token Non null token to encrypt.
     *
     * @return Non null copy of the Token which has been encrypted.
     */
    public void perform(Token token) throws TokenStrategyFailedException {
        try {
            encyptionAction.setBlob(token.getBlob());
            byte[] encryptedBlob = AccessController.doPrivileged(encyptionAction);
            token.setBlob(encryptedBlob);

            if (debug.messageEnabled()) {
                debug.message(CoreTokenConstants.DEBUG_HEADER + "Encrypted Token");
            }

        } catch (PrivilegedActionException e) {
            throw new TokenStrategyFailedException("Failed to encrypt JSON Blob", e);
        }
    }

    /**
     * Decrypt the contents of the Tokens binary data blob.
     *
     * @see org.forgerock.openam.cts.api.tokens.Token#getBlob()
     *
     * @param token Non null Token to decrypt.
     *
     * @return Non null copy of the Token which has been decrypted.
     */
    public void reverse(Token token) throws TokenStrategyFailedException {
        try {
            decyptAction.setBlob(token.getBlob());
            byte[] decryptedBlob = AccessController.doPrivileged(decyptAction);
            token.setBlob(decryptedBlob);

            if (debug.messageEnabled()) {
                debug.message(CoreTokenConstants.DEBUG_HEADER + "Decrypted Token");
            }

        } catch (PrivilegedActionException e) {
            throw new TokenStrategyFailedException("Failed to decrypt JSON Blob", e);
        }
    }
}
