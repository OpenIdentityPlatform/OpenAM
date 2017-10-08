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
 * Copyright 2016 ForgeRock AS.
 */

package org.forgerock.openam.shared.security.crypto;

import static org.mockito.Mockito.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.KeyStore;

import org.testng.annotations.Test;

public class KeyStoreBuilderTest {

    @Test
    public void shouldCloseInputStream() throws Exception {
        // Given
        char[] password = "test".toCharArray();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        KeyStore ks = createKeyStore(baos, password);

        // When
        InputStream is = spy(new ByteArrayInputStream(baos.toByteArray()));
        new KeyStoreBuilder().withInputStream(is).withKeyStoreType(KeyStoreType.JKS).withPassword(password).build();

        // Then
        verify(is, atLeastOnce()).close();
    }

    private KeyStore createKeyStore(OutputStream outputStream, char[] password) throws Exception {
        KeyStore keyStore = KeyStore.getInstance("JKS");
        keyStore.load(null, null);
        keyStore.store(outputStream, password);
        return keyStore;
    }
}