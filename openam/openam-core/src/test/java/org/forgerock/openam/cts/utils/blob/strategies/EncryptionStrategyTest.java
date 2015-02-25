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

import com.sun.identity.shared.debug.Debug;
import org.forgerock.openam.cts.utils.blob.TokenStrategyFailedException;
import org.forgerock.openam.cts.utils.blob.strategies.encryption.DecryptAction;
import org.forgerock.openam.cts.utils.blob.strategies.encryption.EncryptAction;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.mock;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;

public class EncryptionStrategyTest {

    private EncryptAction mockEncryptAction;
    private DecryptAction mockDecryptAction;
    private EncryptionStrategy strategy;
    private byte[] emptyBytes;

    @BeforeMethod
    public void setup() {
        emptyBytes = new byte[]{};

        mockEncryptAction = mock(EncryptAction.class);
        mockDecryptAction = mock(DecryptAction.class);
        strategy = new EncryptionStrategy(
                mockEncryptAction,
                mockDecryptAction,
                mock(Debug.class));
    }

    @Test (expectedExceptions = IllegalArgumentException.class)
    public void shouldPreventNullBlobOnPerform() throws TokenStrategyFailedException {
        strategy.perform(null);
    }

    @Test (expectedExceptions = IllegalArgumentException.class)
    public void shouldPreventNullBlobOnReverse() throws TokenStrategyFailedException {
        strategy.reverse(null);
    }

    @Test
    public void shouldCallEncryptOnPerform() throws Exception {
        strategy.perform(emptyBytes);
        verify(mockEncryptAction).setBlob(eq(emptyBytes));
        verify(mockEncryptAction).run();
    }

    @Test
    public void shouldCallDecryptOnReverse() throws Exception {
        strategy.reverse(emptyBytes);
        verify(mockDecryptAction).setBlob(eq(emptyBytes));
        verify(mockDecryptAction).run();
    }

    @Test (expectedExceptions = TokenStrategyFailedException.class)
    public void shouldThrowExceptionIfEncryptionFails() throws Exception {
        given(mockEncryptAction.run()).willThrow(new Exception());
        strategy.perform(new byte[]{});
    }

    @Test (expectedExceptions = TokenStrategyFailedException.class)
    public void shouldThrowExceptionIfDecryptionFails() throws Exception {
        given(mockDecryptAction.run()).willThrow(new Exception());
        strategy.reverse(new byte[]{});
    }
}
