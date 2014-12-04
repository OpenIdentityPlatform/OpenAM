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
package org.forgerock.openam.cts.utils.blob.strategies;

import org.apache.commons.io.IOUtils;
import org.forgerock.openam.cts.utils.blob.BlobStrategy;
import org.forgerock.openam.cts.utils.blob.TokenStrategyFailedException;
import org.forgerock.util.Reject;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Responsible for compressing the binary object of Tokens using a GZip compression.
 */
public class CompressionStrategy implements BlobStrategy {

    /**
     * Compress the Tokens binary object.
     *
     * @param blob Non null Token to modify.
     *
     * @throws TokenStrategyFailedException {@inheritDoc}
     */
    @Override
    public byte[] perform(byte[] blob) throws TokenStrategyFailedException {
        Reject.ifNull(blob);
        final ByteArrayOutputStream bout = new ByteArrayOutputStream(blob.length);
        try {
            final GZIPOutputStream out = new GZIPOutputStream(bout);
            out.write(blob);
            out.flush();
            out.close();
        } catch (IOException e) {
            throw new TokenStrategyFailedException(e);
        }
        return bout.toByteArray();
    }

    /**
     * Decompress the Tokens binary object.
     *
     * @param blob Non null Token to modify.
     *
     * @throws TokenStrategyFailedException {@inheritDoc}
     */
    @Override
    public byte[] reverse(byte[] blob) throws TokenStrategyFailedException {
        Reject.ifNull(blob);
        final int lengthGuess = blob.length * 2;
        final ByteArrayOutputStream bout = new ByteArrayOutputStream(lengthGuess);
        try {
            GZIPInputStream inputStream = new GZIPInputStream(new ByteArrayInputStream(blob));
            IOUtils.copy(inputStream, bout);
            inputStream.close();
        } catch (IOException e) {
            throw new TokenStrategyFailedException(e);
        }
        return bout.toByteArray();
    }
}
