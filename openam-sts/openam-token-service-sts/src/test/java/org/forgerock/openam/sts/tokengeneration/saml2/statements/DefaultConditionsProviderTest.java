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

package org.forgerock.openam.sts.tokengeneration.saml2.statements;

import com.sun.identity.saml2.assertion.AudienceRestriction;
import com.sun.identity.saml2.assertion.Conditions;
import org.forgerock.openam.sts.TokenCreationException;
import org.forgerock.openam.sts.config.user.SAML2Config;
import org.forgerock.openam.sts.tokengeneration.service.TokenGenerationServiceInvocationState;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.testng.Assert.assertTrue;

public class DefaultConditionsProviderTest {
    private static final boolean WITH_AUDIENCES = true;
    private static final String AM_SP_AUDIENCE = "http://macbook.dirk.internal.forgerock.com:8080/openam/sp";
    private static final int TOKEN_LIFETIME_SECONDS = 600;
    @Test
    public void testBearerWithAudiences() throws TokenCreationException {
        Date issueInstant = new Date();
        ConditionsProvider conditionsProvider = new DefaultConditionsProvider();
        Conditions conditions =
                conditionsProvider.get(createSAML2Config(WITH_AUDIENCES), issueInstant,
                        TokenGenerationServiceInvocationState.SAML2SubjectConfirmation.BEARER);
        assertTrue(issueInstant.equals(conditions.getNotBefore()));
        assertTrue((issueInstant.getTime() + (TOKEN_LIFETIME_SECONDS * 1000)) == conditions.getNotOnOrAfter().getTime());
        AudienceRestriction audienceRestriction = (AudienceRestriction)conditions.getAudienceRestrictions().get(0);
        assertTrue(audienceRestriction.getAudience().contains(AM_SP_AUDIENCE));
    }

    @Test (expectedExceptions = TokenCreationException.class)
    public void testBearerNoAudiences() throws TokenCreationException {
        Date issueInstant = new Date();
        ConditionsProvider conditionsProvider = new DefaultConditionsProvider();
        conditionsProvider.get(createSAML2Config(!WITH_AUDIENCES), issueInstant,
                TokenGenerationServiceInvocationState.SAML2SubjectConfirmation.BEARER);
    }

    @Test
    public void testNoBearerNoAudiences() throws TokenCreationException {
        Date issueInstant = new Date();
        ConditionsProvider conditionsProvider = new DefaultConditionsProvider();
        Conditions conditions =
                conditionsProvider.get(createSAML2Config(!WITH_AUDIENCES), issueInstant,
                        TokenGenerationServiceInvocationState.SAML2SubjectConfirmation.HOLDER_OF_KEY);
        assertTrue(issueInstant.equals(conditions.getNotBefore()));
        assertTrue((issueInstant.getTime() + (TOKEN_LIFETIME_SECONDS * 1000)) == conditions.getNotOnOrAfter().getTime());
    }

    private SAML2Config createSAML2Config(boolean addAudiences) {
        Map<String, String> attributeMap = new HashMap<String, String>();
        attributeMap.put("email", "mail");
        SAML2Config.SAML2ConfigBuilder builder = SAML2Config.builder();
        if (addAudiences) {
            List<String> audiences = new ArrayList<String>();
            audiences.add(AM_SP_AUDIENCE);
            return builder
                    .authenticationContext("urn:oasis:names:tc:SAML:2.0:ac:classes:PasswordProtectedTransport")
                    .attributeMap(attributeMap)
                    .nameIdFormat("urn:oasis:names:tc:SAML:2.0:nameid-format:persistent")
                    .audiences(audiences)
                    .tokenLifetimeInSeconds(TOKEN_LIFETIME_SECONDS)
                    .build();
        } else {
            return builder
                    .authenticationContext("urn:oasis:names:tc:SAML:2.0:ac:classes:PasswordProtectedTransport")
                    .attributeMap(attributeMap)
                    .nameIdFormat("urn:oasis:names:tc:SAML:2.0:nameid-format:persistent")
                    .tokenLifetimeInSeconds(TOKEN_LIFETIME_SECONDS)
                    .build();
        }
    }


}
