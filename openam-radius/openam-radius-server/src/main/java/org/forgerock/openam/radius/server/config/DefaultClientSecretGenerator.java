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
 * Copyrighted 2015 Intellectual Reserve, Inc (IRI)
 *
 * Portions Copyrighted 2016 ForgeRock AS.
 */
package org.forgerock.openam.radius.server.config;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import org.forgerock.util.encode.Base64;

import com.sun.identity.sm.DefaultValues;

/**
 * Generates a random, alphanumeric 16 character password.
 */
public class DefaultClientSecretGenerator extends DefaultValues {

    private final static Random NUMGEN;

    static {
        Random numGen;
        try {
            numGen = SecureRandom.getInstance("SHA1PRNG");
        } catch (final NoSuchAlgorithmException e) {
            numGen = new SecureRandom();
        }
        NUMGEN = numGen;
    }

    @Override
    public Set<String> getDefaultValues() {
        return generateSecretHolder();
    }

    /**
     * Generates client secret of 16 octets in length with characters selected randomly from allowedChars.
     *
     * @return a set containing a string (Base64) encoded that may server as the secret between RADIUS client and
     *         server.
     */
    private Set<String> generateSecretHolder() {
        // rfc2865 says "It is preferred that the secret be 16 octets". However, we use 16 characters since ascii
        // chars have an empty first byte in a java character.
        final byte[] buff = new byte[12];
        NUMGEN.nextBytes(buff);
        final String secret = Base64.encode(buff, true);
        final Set<String> holder = new HashSet<String>();
        holder.add(secret);
        return holder;
    }

}
