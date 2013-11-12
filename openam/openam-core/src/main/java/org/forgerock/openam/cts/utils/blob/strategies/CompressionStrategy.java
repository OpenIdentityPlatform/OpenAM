/**
 * Copyright 2013 ForgeRock AS.
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

import org.forgerock.openam.cts.api.tokens.Token;
import org.forgerock.openam.cts.utils.blob.BlobStrategy;
import org.forgerock.openam.cts.utils.blob.TokenStrategyFailedException;
import org.apache.commons.io.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Responsible for compressing the binary object of Tokens using a GZip compression.
 *
 * @author robert.wapshott@forgerock.com
 */
public class CompressionStrategy implements BlobStrategy {

    private final ByteArrayOutputStream bout = new ByteArrayOutputStream();

    /**
     * Compress the Tokens binary object.
     *
     * @param token Non null Token to modify.
     *
     * @throws org.forgerock.openam.cts.utils.blob.TokenStrategyFailedException {@inheritDoc}
     */
    @Override
    public void perform(Token token) throws TokenStrategyFailedException {
        bout.reset();
        try {
            GZIPOutputStream out = new GZIPOutputStream(bout);
            out.write(token.getBlob(), 0, token.getBlob().length);
            out.flush();
            out.close();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        token.setBlob(bout.toByteArray());
    }

    /**
     * Decompress the Tokens binary object.
     *
     * @param token Non null Token to modify.
     *
     * @throws TokenStrategyFailedException {@inheritDoc}
     */
    @Override
    public void reverse(Token token) throws TokenStrategyFailedException {
        bout.reset();
        try {
            GZIPInputStream inputStream = new GZIPInputStream(new ByteArrayInputStream(token.getBlob()));
            IOUtils.copy(inputStream, bout);
            inputStream.close();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        token.setBlob(bout.toByteArray());
    }
}
