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
 * information: "Portions Copyrighted [year] [name of copyright owner]".
 *
 * Copyright 2014 ForgeRock AS. All rights reserved.
 */

package org.forgerock.openam.sts.token.provider;

import org.forgerock.openam.sts.TokenMarshalException;
import org.slf4j.Logger;
import org.testng.annotations.Test;
import static org.mockito.Mockito.mock;
import static org.testng.Assert.assertTrue;


public class OpenAMSessionIdElementBuilderTest {
    private static final String SESSION_ID =
            "AQIC5wM2LY4SfcyV6vjt7OmHgqDVcjDHanTaTPjbzsxXWKo.*AAJTSQACMDEAAlNLABQtMzAyMzI5NDAzNTIxODIzMjMyOA..*";

    @Test
    public void testRountripTransformation() throws TokenMarshalException {
        Logger mockLogger = mock(Logger.class);
        OpenAMSessionIdElementBuilderImpl builder = new OpenAMSessionIdElementBuilderImpl(mockLogger);
        assertTrue(SESSION_ID.equals(builder.extractOpenAMSessionId(builder.buildOpenAMSessionIdElement(SESSION_ID))));
    }
}
