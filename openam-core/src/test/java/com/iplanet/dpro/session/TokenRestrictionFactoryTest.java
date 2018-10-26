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

package com.iplanet.dpro.session;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.net.UnknownHostException;

import org.forgerock.openam.utils.CollectionUtils;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.sun.identity.sm.ServiceSchemaManager;

public class TokenRestrictionFactoryTest {

    private DNOrIPAddressListTokenRestriction tokenRestriction;

    @BeforeMethod
    public void setUpTest() throws UnknownHostException {
        final String DN = "dc=openam,dc=openidentityplatform,dc=org";
        final String HOST_NAME = "google.com";

        tokenRestriction = new DNOrIPAddressListTokenRestriction(DN, CollectionUtils.asSet(HOST_NAME),
                mock(ServiceSchemaManager.class));
    }

    @Test
    public void canMarshalTokenRestriction() throws Exception {
        String serialisedTokenRestriction = TokenRestrictionFactory.marshal(tokenRestriction);

        assertThat(serialisedTokenRestriction).isNotEmpty();
    }

    @Test
    public void canUnmarshalTokenRestriction() throws Exception {
        String serialisedTokenRestriction = TokenRestrictionFactory.marshal(tokenRestriction);
        TokenRestriction deserialisedTokenRestriction = TokenRestrictionFactory.unmarshal(serialisedTokenRestriction);

        assertThat(deserialisedTokenRestriction.toString()).isEqualTo(tokenRestriction.toString());
    }
}
