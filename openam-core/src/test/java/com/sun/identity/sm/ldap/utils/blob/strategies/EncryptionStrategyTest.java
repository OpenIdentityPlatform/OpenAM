/**
 * Copyright 2013 ForgeRock, AS.
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
package com.sun.identity.sm.ldap.utils.blob.strategies;

import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.ldap.api.tokens.Token;
import com.sun.identity.sm.ldap.utils.blob.TokenStrategyFailedException;
import com.sun.identity.sm.ldap.utils.blob.strategies.encryption.DecryptAction;
import com.sun.identity.sm.ldap.utils.blob.strategies.encryption.EncryptAction;
import org.testng.annotations.Test;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.mock;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;

/**
 * @author robert.wapshott@forgerock.com
 */
public class EncryptionStrategyTest {
    @Test
    public void shouldCallEncryptOnPerform() throws Exception {
        // Given
        byte[] bytes = {};

        EncryptAction mockEncryptAction = mock(EncryptAction.class);
        EncryptionStrategy strategy = new EncryptionStrategy(
                mock(Debug.class),
                mockEncryptAction,
                mock(DecryptAction.class));

        Token mockToken = mock(Token.class);
        given(mockToken.getBlob()).willReturn(bytes);

        // When
        strategy.perform(mockToken);

        // Then
        verify(mockEncryptAction).setBlob(eq(bytes));
        verify(mockEncryptAction).run();
    }

    @Test
    public void shouldCallDecryptOnReverse() throws Exception {
        // Given
        byte[] bytes = {};

        DecryptAction mockDecryptAction = mock(DecryptAction.class);
        EncryptionStrategy strategy = new EncryptionStrategy(
                mock(Debug.class),
                mock(EncryptAction.class),
                mockDecryptAction);

        Token mockToken = mock(Token.class);
        given(mockToken.getBlob()).willReturn(bytes);

        // When
        strategy.reverse(mockToken);

        // Then
        verify(mockDecryptAction).setBlob(eq(bytes));
        verify(mockDecryptAction).run();
    }

    @Test (expectedExceptions = TokenStrategyFailedException.class)
    public void shouldThrowExceptionIfEncryptionFails() throws Exception {
        // Given
        EncryptAction mockEncryptAction = mock(EncryptAction.class);
        EncryptionStrategy strategy = new EncryptionStrategy(
                mock(Debug.class),
                mockEncryptAction,
                mock(DecryptAction.class));

        given(mockEncryptAction.run()).willThrow(new Exception());

        // When / Then
        strategy.perform(mock(Token.class));
    }

    @Test (expectedExceptions = TokenStrategyFailedException.class)
    public void shouldThrowExceptionIfDecryptionFails() throws Exception {
        // Given
        DecryptAction mockDecryptAction = mock(DecryptAction.class);
        EncryptionStrategy strategy = new EncryptionStrategy(
                mock(Debug.class),
                mock(EncryptAction.class),
                mockDecryptAction);

        given(mockDecryptAction.run()).willThrow(new Exception());

        // When / Then
        strategy.reverse(mock(Token.class));
    }
}
