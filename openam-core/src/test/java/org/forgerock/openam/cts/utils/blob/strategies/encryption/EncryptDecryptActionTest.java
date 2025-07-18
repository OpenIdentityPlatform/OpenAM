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
 * Copyright 2013-2015 ForgeRock AS.
 */
package org.forgerock.openam.cts.utils.blob.strategies.encryption;

import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class EncryptDecryptActionTest {
    @Test
    public void shouldDecyrptEncryptedText() throws Exception {
        // Given
        byte[] source = "blobby blobby blob".getBytes();

        EncryptAction encryptAction = new EncryptAction(source);

        // When
        byte[] encryptedResult = encryptAction.run();

        DecryptAction decryptAction = new DecryptAction(encryptedResult);
        byte[] decryptedResult = decryptAction.run();

        // Then
        assertThat(source).isNotEqualTo(encryptedResult);
        assertThat(source).isEqualTo(decryptedResult);
    }
}
