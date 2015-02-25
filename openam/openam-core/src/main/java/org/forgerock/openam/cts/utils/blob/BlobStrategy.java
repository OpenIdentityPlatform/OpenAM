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
 * Copyright 2013-2014 ForgeRock AS.
 */
package org.forgerock.openam.cts.utils.blob;

/**
 * Responsible for defining the interface of the Token Blob Strategy. Each implementation is
 * expected to modify the Token in some consistent way.
 *
 * The strategy should be symmetrical. Once performed, it should be possible to reverse the process
 * and visa versa.
 */
public interface BlobStrategy {
    /**
     * Perform the operation on the Token. This operation is expected to modify the Token in
     * some way which will require reversing in the opposite direction.
     *
     * @param blob Non null Token data to modify.
     *
     * @throws TokenStrategyFailedException If an error occurred whilst processing the Token.
     */
    byte[] perform(byte[] blob) throws TokenStrategyFailedException;

    /**
     * Reverse the operation on the Token. This operation is expected to modify the
     * Token in some way which can be reversed by performing it again.
     *
     * @param blob Non null Token data to modify.
     *
     * @throws TokenStrategyFailedException If an error occurred whilst processing the Token.
     */
    byte[] reverse(byte[] blob) throws TokenStrategyFailedException;
}
