/**
 * Copyright 2013 ForgeRock, Inc.
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
package org.forgerock.openam.auth.shared;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOTokenManager;
import org.testng.annotations.Test;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.testng.AssertJUnit.assertNull;

/**
 * @author robert.wapshott@forgerock.com
 */
public class SSOTokenFactoryTest {
    @Test
    public void shouldRejectEmptyTokenId() {
        SSOTokenManager mockManager = mock(SSOTokenManager.class);
        SSOTokenFactory tokenFactory = new SSOTokenFactory(mockManager);
        assertNull(tokenFactory.getTokenFromId(""));
    }

    @Test
    public void shouldRejectEmptyTokenNull() {
        SSOTokenManager mockManager = mock(SSOTokenManager.class);
        SSOTokenFactory tokenFactory = new SSOTokenFactory(mockManager);
        assertNull(tokenFactory.getTokenFromId(null));
    }

    @Test
    public void shouldUseManagerToCreateSSOToken() throws SSOException {
        String key = "badger";
        SSOTokenManager mockManager = mock(SSOTokenManager.class);
        SSOTokenFactory tokenFactory = new SSOTokenFactory(mockManager);
        tokenFactory.getTokenFromId(key);
        verify(mockManager).createSSOToken(eq(key));
    }
}
