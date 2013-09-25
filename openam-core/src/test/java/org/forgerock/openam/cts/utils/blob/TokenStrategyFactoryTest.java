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
package org.forgerock.openam.cts.utils.blob;

import com.sun.identity.shared.debug.Debug;
import org.forgerock.openam.cts.CoreTokenConfig;
import org.forgerock.openam.cts.utils.blob.strategies.AttributeCompressionStrategy;
import org.forgerock.openam.cts.utils.blob.strategies.CompressionStrategy;
import org.forgerock.openam.cts.utils.blob.strategies.EncryptionStrategy;
import org.forgerock.openam.cts.utils.blob.strategies.encryption.DecryptAction;
import org.forgerock.openam.cts.utils.blob.strategies.encryption.EncryptAction;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.mock;

/**
 * @author robert.wapshott@forgerock.com
 */
public class TokenStrategyFactoryTest {

    private TokenStrategyFactory factory;
    private CompressionStrategy compression;
    private EncryptionStrategy encryption;
    private AttributeCompressionStrategy attributeCompression;

    @BeforeMethod
    public void setup() {
        compression = new CompressionStrategy();
        encryption = new EncryptionStrategy(new EncryptAction(), new DecryptAction(), mock(Debug.class));
        attributeCompression = new AttributeCompressionStrategy(new TokenBlobUtils());
        factory = new TokenStrategyFactory(compression, encryption, attributeCompression);

    }

    @Test
    public void shouldReturnCompression() {
        // Given
        CoreTokenConfig config = mock(CoreTokenConfig.class);
        given(config.isTokenCompressed()).willReturn(true);

        // When
        Collection<BlobStrategy> strategies = factory.getStrategies(config);

        // Then
        assertThat(strategies).contains(compression);
    }

    @Test
    public void shouldReturnEncryption() {
        // Given
        CoreTokenConfig config = mock(CoreTokenConfig.class);
        given(config.isTokenEncrypted()).willReturn(true);

        // When
        Collection<BlobStrategy> strategies = factory.getStrategies(config);

        // Then
        assertThat(strategies).contains(encryption);
    }

    @Test
    public void shouldReturnAttributeCompression() {
        // Given
        CoreTokenConfig config = mock(CoreTokenConfig.class);
        given(config.isAttributeNamesCompressed()).willReturn(true);

        // When
        Collection<BlobStrategy> strategies = factory.getStrategies(config);

        // Then
        assertThat(strategies).contains(attributeCompression);
    }

    @Test
    public void shouldReturnMultipleStrategies() {
        // Given
        CoreTokenConfig config = mock(CoreTokenConfig.class);
        given(config.isTokenEncrypted()).willReturn(true);
        given(config.isTokenCompressed()).willReturn(true);

        // When
        Collection<BlobStrategy> strategies = factory.getStrategies(config);

        // Then
        assertThat(strategies).contains(compression, encryption);
    }

    @Test
    public void shouldReturnAttributeCompressionBeforeGzipCompression() {
        // Given
        CoreTokenConfig config = mock(CoreTokenConfig.class);
        given(config.isAttributeNamesCompressed()).willReturn(true);
        given(config.isTokenCompressed()).willReturn(true);

        // When
        List<BlobStrategy> strategies = new ArrayList<BlobStrategy>(factory.getStrategies(config));

        // Then
        assertThat(strategies.get(0)).isEqualTo(attributeCompression);
        assertThat(strategies.get(1)).isEqualTo(compression);
    }
}
