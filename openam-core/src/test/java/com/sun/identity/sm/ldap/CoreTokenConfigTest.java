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
package com.sun.identity.sm.ldap;

import com.iplanet.dpro.session.service.InternalSession;
import org.testng.annotations.Test;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.mock;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;

/**
 * @author robert.wapshott@forgerock.com
 */
public class CoreTokenConfigTest {
    @Test
    public void shouldConvertUserIdToLowerCaseIfCaseSensitive() {
        // Given
        InternalSession session = mock(InternalSession.class);
        String badger = "BADGER";
        given(session.getUUID()).willReturn(badger);

        CoreTokenConfig config = new CoreTokenConfig();
        assertFalse(config.isCaseSensitiveUserId());

        // When
        String result = config.getUserId(session);

        // Then
        assertEquals(result, badger.toLowerCase());
    }
}
