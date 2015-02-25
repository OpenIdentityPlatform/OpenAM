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

import com.sun.identity.shared.debug.Debug;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.Security;

/**
 * A {@link CipherProvider} that gets queries from the underlying Java Cryptographic Extension (JCE) facilities,
 * via {@link Cipher#getInstance(String, Provider)}. If the preferred JCE provider is not available, then falls back
 * on any provider that can supply the given transformation.
 *
 * @since 12.0.0
 */
public class JCECipherProvider implements CipherProvider {
    private static final Debug DEBUG = Debug.getInstance("amSDK");
    private final String transformation;
    private final Provider preferredProvider;

    /**
     * Initialises the cipher provider with the given cipher transformation and preferred provider. See
     * {@link Cipher#getInstance(String, String)} for details on valid values for these parameters.
     *
     * @param transformation the cipher transformation specification. Cannot be null.
     * @param preferredProvider the preferred crypto provider such as "SunJCE". Will be ignored if null or no such provider exists.
     */
    public JCECipherProvider(final String transformation, final String preferredProvider) {
        this(transformation, Security.getProvider(preferredProvider));
    }

    /**
     * Initialises the cipher provider with the given cipher transformation and preferred provider. See
     * {@link Cipher#getInstance(String, java.security.Provider)} for details on valid values for these parameters.
     *
     * @param transformation the cipher transformation specification. Cannot be null.
     * @param preferredProvider the preferred crypto provider. May be null, in which case it is ignored.
     */
    public JCECipherProvider(final String transformation, final Provider preferredProvider) {
        if (transformation == null) {
            throw new IllegalArgumentException("Cipher transformation cannot be null");
        }
        this.transformation = transformation;
        this.preferredProvider = preferredProvider;
    }

    /**
     * Attempts to get a cipher matching the chosen transformation from the underlying JCE facilities. If the preferred
     * provider is present, then it will use that provider, otherwise any provider will be used.
     *
     * @return a cipher matching the configuration parameters, or null if no matching cipher could be obtained from JCE.
     */
    public Cipher getCipher() {
        try {
            // Use preferred provider if available, otherwise fallback to any provider
            return (preferredProvider != null) ? Cipher.getInstance(transformation, preferredProvider)
                                               : Cipher.getInstance(transformation);
        } catch (NoSuchAlgorithmException ex) {
            DEBUG.error("JCECipherProvider: Algorithm doesn't exist: " + transformation, ex);
        } catch (NoSuchPaddingException ex) {
            DEBUG.error("JCECipherProvider: Padding doesn't exist: " + transformation, ex);
        }
        return null;
    }
}
