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

import com.sun.identity.saml2.assertion.AssertionFactory;
import com.sun.identity.saml2.assertion.AudienceRestriction;
import com.sun.identity.saml2.assertion.Conditions;
import com.sun.identity.saml2.common.SAML2Exception;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.openam.sts.TokenCreationException;
import org.forgerock.openam.sts.config.user.SAML2Config;
import org.forgerock.openam.sts.token.SAML2SubjectConfirmation;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @see org.forgerock.openam.sts.tokengeneration.saml2.statements.ConditionsProvider
 */
public class DefaultConditionsProvider implements ConditionsProvider {
    /**
     * @see org.forgerock.openam.sts.tokengeneration.saml2.statements.ConditionsProvider#get(
     * org.forgerock.openam.sts.config.user.SAML2Config, java.util.Date,
     * org.forgerock.openam.sts.token.SAML2SubjectConfirmation)
     */
    public Conditions get(SAML2Config saml2Config, Date issueInstant,
                          SAML2SubjectConfirmation saml2SubjectConfirmation) throws TokenCreationException {
        Conditions conditions = AssertionFactory.getInstance().createConditions();
        try {
            conditions.setNotBefore(issueInstant);
            conditions.setNotOnOrAfter(new Date(issueInstant.getTime() + (saml2Config.getTokenLifetimeInSeconds() * 1000)));
        } catch (SAML2Exception e) {
            throw new TokenCreationException(ResourceException.INTERNAL_ERROR,
                    "Exception caught setting token lifetime state in SAML2TokenGenerationImpl: " + e, e);

        }
        String audience = saml2Config.getSpEntityId();
        /*
         Section 4.1.4.2 of http://docs.oasis-open.org/security/saml/v2.0/saml-profiles-2.0-os.pdf specifies that
         Audiences specifying the entity ids of SPs, must be contained in the AudienceRestriction for bearer tokens.
         */
        if (((audience == null) || audience.isEmpty()) && SAML2SubjectConfirmation.BEARER.equals(saml2SubjectConfirmation)) {
            throw new TokenCreationException(ResourceException.BAD_REQUEST, "The audiences field in the SAML2Config is empty, " +
                    "but the BEARER SubjectConfirmation is required. BEARER tokens must include Conditions with " +
                    "AudienceRestrictions specifying the SP entity ids.");
        }
        if ((audience != null) && !audience.isEmpty()) {
            try {
                AudienceRestriction audienceRestriction = AssertionFactory.getInstance().createAudienceRestriction();
                List<String> audienceList = new ArrayList<String>(1);
                audienceList.add(audience);
                audienceRestriction.setAudience(audienceList);
                List<AudienceRestriction> audienceRestrictionList = new ArrayList<AudienceRestriction>(1);
                audienceRestrictionList.add(audienceRestriction);
                conditions.setAudienceRestrictions(audienceRestrictionList);
            } catch (SAML2Exception e) {
                throw new TokenCreationException(ResourceException.INTERNAL_ERROR,
                        "Exception caught setting audience restriction state in SAML2TokenGenerationImpl: " + e, e);
            }

        }
        return conditions;
    }
}
