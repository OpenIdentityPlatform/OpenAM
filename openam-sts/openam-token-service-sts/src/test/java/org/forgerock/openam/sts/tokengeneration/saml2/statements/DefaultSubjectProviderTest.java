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

import com.sun.identity.saml2.assertion.Subject;
import com.sun.identity.saml2.assertion.SubjectConfirmation;
import com.sun.identity.saml2.assertion.SubjectConfirmationData;
import com.sun.identity.saml2.common.SAML2Constants;
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


public class DefaultSubjectProviderTest {
    private static final String AUDIENCE_ID = "http://host.com:8080/openam/Consumer/metaAlias/sp";
    private static final String SUBJECT_ID = "bobo";
    private static final int TOKEN_LIFETIME_SECONDS = 600;
    private static final String NAME_ID_FORMAT = "urn:oasis:names:tc:SAML:2.0:nameid-format:persistent";

    @Test
    public void testBearerStateSettings() throws TokenCreationException {
        SubjectProvider subjectProvider = new DefaultSubjectProvider();
        Date issueInstant = new Date();
        Subject subject = subjectProvider.get(SUBJECT_ID, AUDIENCE_ID, createSAML2Config(),
                TokenGenerationServiceInvocationState.SAML2SubjectConfirmation.BEARER, issueInstant);
        assertTrue(SUBJECT_ID.equals(subject.getNameID().getValue()));
        assertTrue(NAME_ID_FORMAT.equals(subject.getNameID().getFormat()));

        SubjectConfirmation subjectConfirmation = (SubjectConfirmation)subject.getSubjectConfirmation().get(0);
        assertTrue(SAML2Constants.SUBJECT_CONFIRMATION_METHOD_BEARER.equals(subjectConfirmation.getMethod()));
                SubjectConfirmationData subjectConfirmationData = subjectConfirmation.getSubjectConfirmationData();
        assertTrue((issueInstant.getTime() + (TOKEN_LIFETIME_SECONDS * 1000)) == subjectConfirmationData.getNotOnOrAfter().getTime());
    }
    private SAML2Config createSAML2Config() {
        Map<String, String> attributeMap = new HashMap<String, String>();
        attributeMap.put("email", "mail");
        SAML2Config.SAML2ConfigBuilder builder = SAML2Config.builder();
        List<String> audiences = new ArrayList<String>();
        audiences.add("http://macbook.dirk.internal.forgerock.com:8080/openam/sp");
        return builder
                .authenticationContext("urn:oasis:names:tc:SAML:2.0:ac:classes:PasswordProtectedTransport")
                .attributeMap(attributeMap)
                .nameIdFormat(NAME_ID_FORMAT)
                .audiences(audiences)
                .tokenLifetimeInSeconds(TOKEN_LIFETIME_SECONDS)
                .build();
    }

}
