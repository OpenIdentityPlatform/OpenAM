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

package org.forgerock.openam.sts.tokengeneration.saml2;

import org.forgerock.openam.sts.TokenCreationException;
import org.forgerock.openam.sts.config.user.SAML2Config;
import org.forgerock.openam.sts.tokengeneration.saml2.statements.AttributeMapper;
import org.forgerock.openam.sts.tokengeneration.saml2.statements.AttributeStatementsProvider;
import org.forgerock.openam.sts.tokengeneration.saml2.statements.AuthenticationStatementsProvider;
import org.forgerock.openam.sts.tokengeneration.saml2.statements.AuthzDecisionStatementsProvider;
import org.forgerock.openam.sts.tokengeneration.saml2.statements.ConditionsProvider;
import org.forgerock.openam.sts.tokengeneration.saml2.statements.SubjectProvider;

/**
 * Defines the interface referenced by the SAML2TokenGeneration implementation to obtain the providers of the various
 * SAML2 assertion statements. The implementation of this method will return an instance of the *Provider specified
 * in the SAML2Config, and if no custom *Provider is specified, an instance of the Default*Provider will be returned.
 */
public interface StatementProvider {
    /**
     * @param saml2Config The SAML2Config corresponding to the STS instance consuming the TokenGenerationService
     * @return The ConditionsProvider instance which will be invoked to obtain the Conditions included in the generated SAML2
     * assertion
     * @throws TokenCreationException
     */
    ConditionsProvider getConditionsProvider(SAML2Config saml2Config) throws TokenCreationException;

    /**
     *
     * @param saml2Config The SAML2Config corresponding to the STS instance consuming the TokenGenerationService
     * @return The SubjectProvider instance which will be invoked to obtain the Subject included in the generated SAML2
     * assertion
     * @throws TokenCreationException
     */
    SubjectProvider getSubjectProvider(SAML2Config saml2Config) throws TokenCreationException;

    /**
     *
     * @param saml2Config The SAML2Config corresponding to the STS instance consuming the TokenGenerationService
     * @return The AuthenticationStatementsProvider instance which will be invoked to obtain the AuthenticationStatements included in the generated SAML2
     * assertion
     * @throws TokenCreationException
     */
    AuthenticationStatementsProvider getAuthenticationStatementsProvider(SAML2Config saml2Config) throws TokenCreationException;

    /**
     *
     * @param saml2Config The SAML2Config corresponding to the STS instance consuming the TokenGenerationService
     * @return The AttributeStatementsProvider instance which will be invoked to obtain the AttributeStatements included in the generated SAML2
     * assertion
     * @throws TokenCreationException
     */
    AttributeStatementsProvider getAttributeStatementsProvider(SAML2Config saml2Config) throws TokenCreationException;

    /**
     *
     * @param saml2Config The SAML2Config corresponding to the STS instance consuming the TokenGenerationService
     * @return The AuthzDecisionStatementsProvider instance which will be invoked to obtain the AuthzDecisionStatements included in the generated SAML2
     * assertion
     * @throws TokenCreationException
     */
    AuthzDecisionStatementsProvider getAuthzDecisionStatementsProvider(SAML2Config saml2Config) throws TokenCreationException;

    /**
     *
     * @param saml2Config The SAML2Config corresponding to the STS instance consuming the TokenGenerationService
     * @return The AttributeMapper instance which will be invoked to obtain the Attributes included in the generated SAML2
     * assertion
     * @throws TokenCreationException
     */
    AttributeMapper getAttributeMapper(SAML2Config saml2Config) throws TokenCreationException;

}
