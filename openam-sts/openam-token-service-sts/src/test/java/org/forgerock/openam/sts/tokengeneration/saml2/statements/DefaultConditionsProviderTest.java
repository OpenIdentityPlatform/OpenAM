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
import org.forgerock.openam.sts.AMSTSConstants;
import org.forgerock.openam.sts.TokenCreationException;
import org.forgerock.openam.sts.config.user.SAML2Config;
import org.forgerock.openam.sts.token.SAML2SubjectConfirmation;
import org.testng.annotations.Test;

import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.testng.Assert.assertTrue;

public class DefaultConditionsProviderTest {
    private static final String AM_SP_AUDIENCE = "http://macbook.dirk.internal.forgerock.com:8080/openam/sp";
    private static final int TOKEN_LIFETIME_SECONDS = 600;
    @Test
    public void testBearerWithAudiences() throws TokenCreationException, UnsupportedEncodingException {
        Date issueInstant = new Date();
        ConditionsProvider conditionsProvider = new DefaultConditionsProvider();
        Conditions conditions =
                conditionsProvider.get(createSAML2Config(), issueInstant,
                        SAML2SubjectConfirmation.BEARER);
        assertTrue(issueInstant.equals(conditions.getNotBefore()));
        assertTrue((issueInstant.getTime() + (TOKEN_LIFETIME_SECONDS * 1000)) == conditions.getNotOnOrAfter().getTime());
        AudienceRestriction audienceRestriction = (AudienceRestriction)conditions.getAudienceRestrictions().get(0);
        assertTrue(audienceRestriction.getAudience().contains(AM_SP_AUDIENCE));
    }

    @Test
    public void testNoBearer() throws TokenCreationException, UnsupportedEncodingException {
        Date issueInstant = new Date();
        ConditionsProvider conditionsProvider = new DefaultConditionsProvider();
        Conditions conditions =
                conditionsProvider.get(createSAML2Config(), issueInstant,
                        SAML2SubjectConfirmation.HOLDER_OF_KEY);
        assertTrue(issueInstant.equals(conditions.getNotBefore()));
        assertTrue((issueInstant.getTime() + (TOKEN_LIFETIME_SECONDS * 1000)) == conditions.getNotOnOrAfter().getTime());
    }

    private SAML2Config createSAML2Config() throws UnsupportedEncodingException {
        Map<String, String> attributeMap = new HashMap<String, String>();
        attributeMap.put("email", "mail");
        return SAML2Config.builder()
                .attributeMap(attributeMap)
                .nameIdFormat("urn:oasis:names:tc:SAML:2.0:nameid-format:persistent")
                .spEntityId(AM_SP_AUDIENCE)
                .tokenLifetimeInSeconds(TOKEN_LIFETIME_SECONDS)
                .keystoreFile("/keystore.jks")
                .keystorePassword("changeit".getBytes(AMSTSConstants.UTF_8_CHARSET_ID))
                .encryptionKeyAlias("test")
                .signatureKeyAlias("test")
                .signatureKeyPassword("changeit".getBytes(AMSTSConstants.UTF_8_CHARSET_ID))
                .build();
    }
}
