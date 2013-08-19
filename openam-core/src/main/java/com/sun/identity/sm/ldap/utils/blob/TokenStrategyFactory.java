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
package com.sun.identity.sm.ldap.utils.blob;

import com.google.inject.Inject;
import com.sun.identity.sm.ldap.CoreTokenConfig;
import com.sun.identity.sm.ldap.utils.blob.strategies.AttributeCompressionStrategy;
import com.sun.identity.sm.ldap.utils.blob.strategies.CompressionStrategy;
import com.sun.identity.sm.ldap.utils.blob.strategies.EncryptionStrategy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Responsible for providing BlobStrategy implementations based on the CoreTokenConfig.
 *
 * @author robert.wapshott@forgerock.com
 */
public class TokenStrategyFactory {
    private final CompressionStrategy compression;
    private final EncryptionStrategy encryption;
    private final AttributeCompressionStrategy attributeCompression;

    @Inject
    public TokenStrategyFactory(CompressionStrategy compression, EncryptionStrategy encryption,
                                AttributeCompressionStrategy attributeCompression) {
        this.compression = compression;
        this.encryption = encryption;
        this.attributeCompression = attributeCompression;
    }

    /**
     * Strategy patten decision point which allows us at runtime to determine the
     * required BlobStrategies.
     *
     * @param config A CoreTokenConfig to determine the appropriate strategies.
     * @return A collection of BlobStrategies based on the provided CoreTokenConfig.
     */
    public Collection<BlobStrategy> getStrategies(CoreTokenConfig config) {
        List<BlobStrategy> strategies = new ArrayList<BlobStrategy>();
        if (config.isAttributeNamesCompressed()) {
            strategies.add(attributeCompression);
        }
        if (config.isTokenCompressed()) {
            strategies.add(compression);
        }
        if (config.isTokenEncrypted()) {
            strategies.add(encryption);
        }
        return strategies;
    }
}
