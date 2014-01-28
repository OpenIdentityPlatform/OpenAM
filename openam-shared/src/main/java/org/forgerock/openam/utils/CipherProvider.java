/*
 * Copyright 2014 ForgeRock, AS.
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

package org.forgerock.openam.utils;

import javax.crypto.Cipher;

/**
 * Abstract factory for obtaining {@link Cipher} instances. Implementations may cache or pool ciphers to avoid
 * expensive initialisation overhead. Callers should ensure that they initialise ciphers returned from a cipher provider
 * before making use of them.
 *
 * @since 12.0.0
 */
public interface CipherProvider {
    /**
     * Returns a cipher from the underlying mechanism. The algorithm, padding, and provider used by the cipher are
     * determined by the underlying CipherProvider implementation.
     * The caller should call {@link Cipher#init(int, java.security.Key, java.security.AlgorithmParameters)}
     * before making use of the cipher to ensure it is properly initialised.
     *
     * @return a configured cipher or null if a cipher could not be provided.
     */
    Cipher getCipher();
}
