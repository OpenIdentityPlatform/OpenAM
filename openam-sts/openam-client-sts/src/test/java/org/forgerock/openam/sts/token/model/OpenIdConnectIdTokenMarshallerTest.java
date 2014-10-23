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

package org.forgerock.openam.sts.token.model;

import org.forgerock.openam.sts.TokenMarshalException;
import org.forgerock.openam.sts.XMLUtilitiesImpl;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

public class OpenIdConnectIdTokenMarshallerTest {
    private static final String TOKEN_VALUE = "eyJhb.eyJpc3MiOiJhY2N.SqcfMU-BsrS69tGLIFRq";

    @Test
    public void testJsonRoundTrip() throws TokenMarshalException {
        OpenIdConnectIdToken idToken = new OpenIdConnectIdToken(TOKEN_VALUE);
        OpenIdConnectIdTokenMarshaller marshaller = new OpenIdConnectIdTokenMarshaller(new XMLUtilitiesImpl());
        assertEquals(idToken.getTokenValue(), marshaller.fromJson(marshaller.toJson(idToken)).getTokenValue());
    }

    @Test
    public void testXmlRoundTrip() throws TokenMarshalException {
        OpenIdConnectIdToken idToken = new OpenIdConnectIdToken(TOKEN_VALUE);
        OpenIdConnectIdTokenMarshaller marshaller = new OpenIdConnectIdTokenMarshaller(new XMLUtilitiesImpl());
        assertEquals(idToken.getTokenValue(), marshaller.fromXml(marshaller.toXml(idToken)).getTokenValue());
    }
}
